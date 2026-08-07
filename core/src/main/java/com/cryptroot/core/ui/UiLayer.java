package com.cryptroot.core.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cryptroot.core.render.SelectionOutlineRenderer;
import com.cryptroot.core.ui.layout.Clippable;
import com.cryptroot.core.ui.layout.LayoutElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Manages a collection of {@link UiWidget}s for a single screen, providing:
 *
 * <ul>
 *   <li>Z-order–driven draw order: lowest z drawn first (behind).
 *   <li>Z-order–driven hit-testing and scroll routing: highest z tested first (on top), stopping at
 *       the first consumer.
 *   <li>Viewport-correct cursor polling for hover updates each frame.
 *   <li>An {@link InputProcessor} for pointer and scroll routing that can be composed with a
 *       screen's keyboard handler.
 * </ul>
 *
 * <h3>Z-order</h3>
 *
 * Z-order is specified per-widget when calling {@link #add(UiWidget, int)}. It is the single source
 * of truth for interaction precedence: a widget added later with a lower z-order will still lose to
 * an earlier widget with a higher z-order, regardless of insertion sequence.
 *
 * <h3>Input routing</h3>
 *
 * The {@link InputProcessor} returned by {@link #inputProcessor()} handles left-click, drag,
 * scroll, and focused-widget keyboard events. Screen-level keyboard handling (Escape, shortcuts,
 * etc.) should be in a separate {@link InputAdapter} composed via {@link
 * com.badlogic.gdx.InputMultiplexer InputMultiplexer}:
 *
 * <pre>{@code
 * Gdx.input.setInputProcessor(
 *     new InputMultiplexer(layer.inputProcessor(), keyboardAdapter));
 * }</pre>
 *
 * <h3>Focus</h3>
 *
 * When a {@link Focusable} widget is hit, {@link UiLayer} grants it keyboard focus automatically.
 * Only one widget holds focus at a time. Focus is cleared when a non-{@code Focusable} widget is
 * hit, or when {@link #reset()} is called. Keyboard events are forwarded to the focused widget
 * before reaching the screen adapter; any event not consumed ({@code false}) falls through.
 *
 * <h3>Render contract</h3>
 *
 * This layer owns its own {@link Viewport} and {@link OrthographicCamera}, so it also owns the
 * projection the UI is drawn with: call {@link #render(PolygonSpriteBatch,
 * SelectionOutlineRenderer)} and it sets the projection, brackets {@code begin()}/{@code end()},
 * and drives the outline passes in the correct order. {@link com.cryptroot.core.screen.BaseScreen
 * BaseScreen} does this for every screen automatically. {@link #draw(PolygonSpriteBatch)} remains
 * available for composing the layer into an existing batch block, and fails fast if the projection
 * does not match — drawing the UI with a different projection than {@link #update(float)} and
 * {@link #inputProcessor()} unproject with is the one mistake that silently misplaces every widget
 * on screen while clicks keep landing elsewhere.
 *
 * <p>{@link #update(float)} returns {@code true} when a widget has consumed the frame (e.g., a
 * navigation action fired after a click-feedback delay). When this occurs, the screen <em>must</em>
 * return from {@code render()} immediately without issuing any draw calls — a screen transition may
 * already be in progress:
 *
 * <pre>{@code
 * public void render(float delta) {
 *     if (uiLayer.update(delta)) return;
 *     // ... draw world ...
 *     uiLayer.render(batch, outlineRenderer);
 * }
 * }</pre>
 */
public final class UiLayer {

  private static final class Entry {
    final UiWidget widget;
    final int zOrder;

    Entry(UiWidget widget, int zOrder) {
      this.widget = widget;
      this.zOrder = zOrder;
    }
  }

  private final Viewport viewport;
  private final OrthographicCamera camera;
  private final List<Entry> entries = new ArrayList<>();

  /** Draw / layout order — ascending z (lowest z drawn first/behind). */
  private List<Entry> ascending = List.of();

  /** Hit-test / scroll order — descending z (highest z tested first/on top). */
  private List<Entry> descending = List.of();

  /**
   * Z-order the {@linkplain #setRoot layout root} occupies. Deliberately very negative so ordinary
   * widgets added with {@link #add} sit above it by default.
   */
  public static final int ROOT_Z_ORDER = Integer.MIN_VALUE + 1;

  /** Full-screen layout tree, or {@code null}. See {@link #setRoot(LayoutElement)}. */
  private LayoutElement root;

  /** Cursor position in world space, refreshed once per frame by {@link #syncPointer()}. */
  private final Vector3 pointer = new Vector3();

  /**
   * Frame id the {@link #pointer} was last unprojected on, so the several places that need the
   * cursor in world space during one frame share a single unprojection instead of repeating it.
   */
  private long pointerFrameId = -1L;

  /**
   * Separate scratch for unprojecting the coordinates carried by an input <em>event</em>.
   *
   * <p>Kept distinct from {@link #pointer} on purpose: input events are delivered during the same
   * frame as the render that follows, so writing event coordinates into the cached cursor would
   * leave the frame's hover pass reading a stale position.
   */
  private final Vector3 eventPointer = new Vector3();

  /** The widget currently holding keyboard focus, or {@code null}. */
  private Focusable focused;

  /**
   * Ring opacity captured by {@link #captureOutlines} this frame; consumed by {@link
   * #drawOutlines}.
   */
  private float outlineAlpha;

  public UiLayer(Viewport viewport, OrthographicCamera camera) {
    Objects.requireNonNull(viewport, "viewport must not be null");
    Objects.requireNonNull(camera, "camera must not be null");
    this.viewport = viewport;
    this.camera = camera;
  }

  /** Returns the {@link OrthographicCamera} this layer was constructed with. */
  public OrthographicCamera getCamera() {
    return camera;
  }

  /** Returns the {@link Viewport} this layer was constructed with. */
  public Viewport getViewport() {
    return viewport;
  }

  // -------------------------------------------------------------------------
  // Widget management
  // -------------------------------------------------------------------------

  /**
   * Adds {@code widget} to this layer with the given {@code zOrder} and immediately calls {@link
   * UiWidget#layout()} to initialise its bounds. Higher z-order widgets are drawn on top and
   * receive pointer events first.
   */
  public void add(UiWidget widget, int zOrder) {
    Objects.requireNonNull(widget, "widget must not be null");
    entries.add(new Entry(widget, zOrder));
    rebuildSorted();
    pushClipContext(widget);
    widget.layout();
  }

  /**
   * Installs {@code root} as this layer's full-screen layout root at z-order {@link #ROOT_Z_ORDER}.
   *
   * <p>On every layout pass the root is assigned the whole viewport — {@code (0, 0,
   * viewport.getWorldWidth(), viewport.getWorldHeight())} — and the rest of the tree sizes itself
   * from there. This is the <em>only</em> place the world's dimensions enter the UI toolkit, and
   * they come from the viewport this layer already owns, so no widget needs to know the resolution
   * and nothing has to be re-specified when it changes. A window resize re-runs it for free via the
   * existing {@code resize() → layout()} path.
   *
   * <p>Replaces any previous root. Widgets added with {@link #add} are unaffected and can be
   * layered above or below by z-order.
   *
   * @param root the layout tree to fill the screen, or {@code null} to remove the current root
   */
  public void setRoot(LayoutElement root) {
    if (this.root != null) {
      remove(this.root);
    }
    this.root = root;
    if (root != null) {
      // Assign the viewport rectangle before add(), because add() lays the widget out immediately
      // and
      // would otherwise measure it against a zero-sized frame.
      layoutRoot();
      add(root, ROOT_Z_ORDER);
    }
  }

  /** Returns the layout root set by {@link #setRoot(LayoutElement)}, or {@code null}. */
  public LayoutElement getRoot() {
    return root;
  }

  /**
   * Returns every widget in this layer in ascending z-order (lowest z first), including the
   * {@linkplain #setRoot layout root}.
   *
   * <p>A read-only snapshot of the same order {@link #draw(PolygonSpriteBatch)} uses, provided so a
   * caller can walk the whole widget tree — {@link #getRoot()} alone misses anything added with
   * {@link #add(UiWidget, int)}, which is how dialogs, popups and tooltips get onto a layer.
   * Mutating the layer while iterating the returned list is safe: the list is a copy of the entry
   * order, not a live view.
   */
  public List<UiWidget> widgets() {
    List<UiWidget> out = new ArrayList<>(ascending.size());
    for (Entry e : ascending) {
      out.add(e.widget);
    }
    return List.copyOf(out);
  }

  /** Assigns the viewport rectangle to the layout root, if one is set. */
  private void layoutRoot() {
    if (root == null) return;
    root.setBounds(0f, 0f, viewport.getWorldWidth(), viewport.getWorldHeight());
  }

  /**
   * Supplies this layer's viewport and camera to every {@link Clippable} in {@code widget}'s
   * subtree.
   *
   * <p>Done here because this layer is the one object that already holds both, which spares every
   * clipping widget from taking them through its constructor and spares game code from threading
   * them down the tree.
   */
  private void pushClipContext(UiWidget widget) {
    if (widget instanceof Clippable clippable) {
      clippable.setClipContext(viewport, camera);
    }
    if (widget instanceof CompositeWidget composite) {
      for (UiWidget child : composite.children()) {
        pushClipContext(child);
      }
    }
  }

  /** Removes {@code widget} from this layer. No-op if the widget is not present. */
  public void remove(UiWidget widget) {
    entries.removeIf(e -> e.widget == widget);
    rebuildSorted();
  }

  /** Removes all widgets from this layer, including the {@linkplain #setRoot layout root}. */
  public void clear() {
    entries.clear();
    root = null;
    ascending = List.of();
    descending = List.of();
  }

  private void rebuildSorted() {
    List<Entry> buf = new ArrayList<>(entries);
    buf.sort(Comparator.comparingInt(e -> e.zOrder));
    ascending = List.copyOf(buf);
    buf.sort(Comparator.comparingInt((Entry e) -> e.zOrder).reversed());
    descending = List.copyOf(buf);
  }

  // -------------------------------------------------------------------------
  // Lifecycle delegation
  // -------------------------------------------------------------------------

  /**
   * Assigns the viewport rectangle to the {@linkplain #setRoot layout root}, then calls {@link
   * UiWidget#layout()} on all widgets in ascending z-order. Call from the screen's {@code resize()}
   * after updating the viewport.
   */
  public void layout() {
    layoutRoot();
    for (Entry e : ascending) {
      e.widget.layout();
    }
  }

  /**
   * Calls {@link UiWidget#reset()} on all widgets and clears keyboard focus. Call from the screen's
   * {@code hide()} to clear transient state so the screen can be safely re-entered.
   */
  public void reset() {
    clearFocus();
    for (Entry e : ascending) {
      e.widget.reset();
    }
  }

  // -------------------------------------------------------------------------
  // Focus
  // -------------------------------------------------------------------------

  /**
   * Grants keyboard focus to {@code widget}. If another widget currently holds focus, {@link
   * Focusable#onFocusLost()} is called on it first. Passing {@code null} is equivalent to {@link
   * #clearFocus()}.
   */
  public void setFocus(Focusable widget) {
    if (focused == widget) return;
    if (focused != null) focused.onFocusLost();
    focused = widget;
    if (focused != null) focused.onFocusGained();
  }

  /** Removes keyboard focus from whichever widget currently holds it. */
  public void clearFocus() {
    setFocus(null);
  }

  // -------------------------------------------------------------------------
  // Frame update
  // -------------------------------------------------------------------------

  /**
   * Each frame: polls the cursor position, calls {@link UiWidget#updateHover(float, float)} on
   * every widget, then ticks each widget's per-frame state via {@link UiWidget#update(float)}.
   *
   * <p><b>Render contract:</b> when this method returns {@code true}, the screen must return from
   * {@code render()} immediately without issuing any draw calls. See {@link UiWidget#update(float)}
   * and the class-level note for details.
   *
   * @return {@code true} if any widget consumed the frame.
   */
  public boolean update(float delta) {
    syncPointer();
    int blockZ = blockingZForPointer();
    for (Entry e : ascending) {
      if (e.zOrder < blockZ) {
        e.widget.clearHover(); // occluded by a higher-z opaque surface
      } else {
        e.widget.updateHover(pointer.x, pointer.y);
      }
    }
    for (Entry e : ascending) {
      if (e.widget.update(delta)) return true;
    }
    return false;
  }

  /**
   * Unprojects the cursor into {@link #pointer}, at most once per frame.
   *
   * <p>The cursor position is needed by {@link #update(float)}, the occlusion scan, and both
   * outline passes. Unprojecting it separately in each was redundant work every frame and, worse,
   * meant those passes could in principle disagree about where the pointer was.
   */
  private void syncPointer() {
    long frameId = Gdx.graphics.getFrameId();
    if (frameId == pointerFrameId) return;
    pointerFrameId = frameId;
    pointer.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
    viewport.unproject(pointer);
  }

  /**
   * Returns the z-order of the highest opaque widget currently occluding the pointer, or {@link
   * Integer#MIN_VALUE} if nothing blocks it. Widgets with a strictly lower z-order than the
   * returned value are considered occluded and must not receive hover or outline treatment this
   * frame.
   */
  private int blockingZForPointer() {
    syncPointer();
    for (Entry e : descending) { // highest z first
      if (e.widget.blocksPointer(pointer.x, pointer.y)) {
        return e.zOrder;
      }
    }
    return Integer.MIN_VALUE;
  }

  // -------------------------------------------------------------------------
  // Outline capture (for HotspotWidget and any OutlineCaptureSource widgets)
  // -------------------------------------------------------------------------

  /**
   * Captures all active {@link OutlineCaptureSource} widgets found in the widget tree into the
   * shared FBO in a single pass.
   *
   * <p>All active sources are rendered together so their outlines can be blitted in one {@link
   * #drawOutlines} call. The maximum hover alpha across all active sources is used as the ring
   * opacity; individual fade curves therefore converge to the brightest active hotspot's alpha.
   *
   * <p><b>Must be called before {@code batch.begin()} for the current frame.</b> Typically invoked
   * at the top of the screen's {@code onRender} method, before the main draw pass.
   *
   * @param sor the shared outline renderer
   * @param batch the polygon sprite batch (must not be in begin/end)
   * @param projectionMatrix the {@code camera.combined} matrix used for the scene
   * @param viewport used to restore the GL viewport after FBO capture
   */
  public void captureOutlines(
      SelectionOutlineRenderer sor,
      PolygonSpriteBatch batch,
      Matrix4 projectionMatrix,
      Viewport viewport) {
    Objects.requireNonNull(sor, "sor must not be null");
    ArrayList<OutlineCaptureSource> sources = new ArrayList<>();
    int blockZ = blockingZForPointer();
    for (Entry e : ascending) {
      if (e.zOrder < blockZ) continue; // occluded by a higher-z opaque surface
      collectCaptureSources(e.widget, sources);
    }
    outlineAlpha =
        sources.isEmpty() ? 0f : sor.captureSources(batch, projectionMatrix, viewport, sources);
  }

  /**
   * Blits the outline FBO for all active {@link OutlineCaptureSource} widgets.
   *
   * <p><b>Must be called inside an active {@code batch.begin()/end()} block</b>, after {@link
   * #draw(PolygonSpriteBatch)} so the outline ring appears on top of the normally-drawn overlay
   * textures.
   *
   * @param sor the shared outline renderer
   * @param batch the polygon sprite batch (must be in a begin/end block)
   */
  public void drawOutlines(SelectionOutlineRenderer sor, PolygonSpriteBatch batch) {
    Objects.requireNonNull(sor, "sor must not be null");
    if (outlineAlpha > 0f) {
      sor.drawOutline(batch, outlineAlpha);
    }
  }

  /**
   * Calls {@link OutlineCaptureSource#drawPostOutline} on every active capture source in the widget
   * tree. Must be called inside a {@code batch.begin()/end()} block, immediately after {@link
   * #drawOutlines}, so that labels and other overlays appear on top of the outline FBO blit.
   */
  public void drawPostOutlines(PolygonSpriteBatch batch) {
    int blockZ = blockingZForPointer();
    for (Entry e : ascending) {
      if (e.zOrder < blockZ) continue; // occluded by a higher-z opaque surface
      callPostOutline(e.widget, batch);
    }
  }

  private void callPostOutline(UiWidget widget, PolygonSpriteBatch batch) {
    if (widget instanceof OutlineCaptureSource src && src.outlineActive()) {
      src.drawPostOutline(batch);
    }
    if (widget instanceof CompositeWidget cw) {
      for (UiWidget child : cw.children()) {
        callPostOutline(child, batch);
      }
    }
  }

  /** Recursively collects all active {@link OutlineCaptureSource}s in the widget tree. */
  private void collectCaptureSources(UiWidget widget, ArrayList<OutlineCaptureSource> out) {
    if (widget instanceof OutlineCaptureSource src && src.outlineActive()) {
      out.add(src);
    }
    if (widget instanceof CompositeWidget cw) {
      for (UiWidget child : cw.children()) {
        collectCaptureSources(child, out);
      }
    }
  }

  // -------------------------------------------------------------------------
  // Draw
  // -------------------------------------------------------------------------

  /**
   * Draws this layer as one self-contained pass: sets the batch projection from this layer's own
   * camera, opens and closes the {@code begin()}/{@code end()} block, and draws every widget in
   * ascending z-order.
   *
   * <p><b>Prefer this over {@link #draw(PolygonSpriteBatch)}.</b> The layer already owns the {@link
   * Viewport} and {@link OrthographicCamera} its widgets were laid out in, so letting it own the
   * projection makes the contract impossible to get wrong. A caller that sets a different (or no)
   * projection silently renders the UI at the wrong scale — screen-space widget coordinates would
   * no longer match the coordinates {@link #update(float)} and {@link #inputProcessor()} hit-test
   * against.
   *
   * @param batch the batch to draw with; must <em>not</em> already be drawing
   * @throws IllegalStateException if {@code batch} is already inside a {@code begin()} block
   */
  public void render(PolygonSpriteBatch batch) {
    render(batch, null);
  }

  /**
   * As {@link #render(PolygonSpriteBatch)}, but additionally drives the selection-outline cycle for
   * any {@link OutlineCaptureSource} widgets in the tree: FBO capture before {@code begin()}, then
   * the ring blit and post-outline overlays inside the block. Screens therefore get working UI
   * outlines without hand-sequencing {@link #captureOutlines}, {@link #drawOutlines} and {@link
   * #drawPostOutlines} in the right order relative to {@code begin()}.
   *
   * @param batch the batch to draw with; must <em>not</em> already be drawing
   * @param sor the shared outline renderer, or {@code null} to skip the outline passes entirely
   * @throws IllegalStateException if {@code batch} is already inside a {@code begin()} block
   */
  public void render(PolygonSpriteBatch batch, SelectionOutlineRenderer sor) {
    Objects.requireNonNull(batch, "batch must not be null");
    if (batch.isDrawing()) {
      throw new IllegalStateException(
          "batch must not already be drawing: UiLayer.render owns begin()/end() — "
              + "use draw(batch) to append this layer to an existing block");
    }
    // A world-only screen keeps an empty layer; skip the begin/end pair and the FBO capture
    // entirely.
    if (ascending.isEmpty()) return;
    camera.update();
    // Outline capture rebinds the FBO and must precede begin().
    if (sor != null) {
      captureOutlines(sor, batch, camera.combined, viewport);
    }
    batch.setProjectionMatrix(camera.combined);
    batch.begin();
    draw(batch);
    if (sor != null) {
      drawOutlines(sor, batch);
      drawPostOutlines(batch);
    }
    batch.end();
  }

  /**
   * Draws all widgets in ascending z-order (lowest z drawn first/behind).
   *
   * <p>Low-level entry point for composing this layer into an existing {@code batch.begin()}/{@code
   * end()} block. The caller is responsible for having set the projection matrix to this layer's
   * {@linkplain #getCamera() camera}; both preconditions are checked and fail fast, because getting
   * either wrong renders the whole UI at the wrong scale while hit-testing continues to use the
   * correct one. {@link #render(PolygonSpriteBatch)} handles both for you.
   *
   * @throws IllegalStateException if {@code batch} is not drawing, or if its projection matrix does
   *     not match this layer's camera
   */
  public void draw(PolygonSpriteBatch batch) {
    if (!batch.isDrawing()) {
      throw new IllegalStateException(
          "draw() must be called inside batch.begin()/end() — did you mean render(batch)?");
    }
    if (!Arrays.equals(batch.getProjectionMatrix().val, camera.combined.val)) {
      throw new IllegalStateException(
          "batch projection matrix does not match this UiLayer's camera: widgets were laid out in "
              + viewport.getWorldWidth()
              + "x"
              + viewport.getWorldHeight()
              + " world units. Call batch.setProjectionMatrix(uiLayer.getCamera().combined) first, "
              + "or use render(batch) which does it for you.");
    }
    for (Entry e : ascending) {
      e.widget.draw(batch);
    }
  }

  // -------------------------------------------------------------------------
  // Input
  // -------------------------------------------------------------------------

  /**
   * Returns an {@link InputProcessor} that routes pointer (left-click, drag, release), scroll, and
   * focused-keyboard events.
   *
   * <p><b>Call this method exactly once per screen show</b> and store the returned reference in an
   * {@link com.badlogic.gdx.InputMultiplexer InputMultiplexer}. Do not call it on every frame.
   *
   * <p>Keyboard events consumed by the focused {@link Focusable} widget return {@code true};
   * unconsumed events fall through to the screen's own keyboard adapter.
   */
  public InputProcessor inputProcessor() {
    return new InputAdapter() {
      private UiWidget dragTarget;

      @Override
      public boolean touchDown(int screenX, int screenY, int pointerId, int button) {
        if (button != Input.Buttons.LEFT) return false;
        eventPointer.set(screenX, screenY, 0f);
        viewport.unproject(eventPointer);
        for (Entry e : descending) {
          if (e.widget.hit(eventPointer.x, eventPointer.y)) {
            dragTarget = e.widget;
            Focusable f = e.widget.hitFocusable();
            if (f != null) setFocus(f);
            else clearFocus();
            return true;
          }
        }
        clearFocus();
        return false;
      }

      @Override
      public boolean touchDragged(int screenX, int screenY, int pointerId) {
        if (dragTarget == null) return false;
        eventPointer.set(screenX, screenY, 0f);
        viewport.unproject(eventPointer);
        dragTarget.dragged(eventPointer.x, eventPointer.y);
        return true;
      }

      @Override
      public boolean touchUp(int screenX, int screenY, int pointerId, int button) {
        if (button != Input.Buttons.LEFT || dragTarget == null) return false;
        eventPointer.set(screenX, screenY, 0f);
        viewport.unproject(eventPointer);
        UiWidget released = dragTarget;
        // Cleared before the callback: a widget may legitimately tear down this layer from its
        // release handler (screen transitions), and must not leave a stale capture behind.
        dragTarget = null;
        released.released(eventPointer.x, eventPointer.y);
        return true;
      }

      @Override
      public boolean scrolled(float amountX, float amountY) {
        eventPointer.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
        viewport.unproject(eventPointer);
        for (Entry e : descending) {
          if (e.widget.scrolled(eventPointer.x, eventPointer.y, amountX, amountY)) return true;
        }
        return false;
      }

      @Override
      public boolean keyDown(int keycode) {
        return focused != null && focused.focusedKeyDown(keycode);
      }

      @Override
      public boolean keyTyped(char character) {
        if (focused == null) return false;
        // Forward printable characters only; control codes go through focusedKeyDown.
        if (character >= 32 && character != 127) {
          focused.keyTyped(character);
          return true;
        }
        return false;
      }
    };
  }
}
