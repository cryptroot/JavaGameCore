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
import java.util.ArrayList;
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
 * {@link #update(float)} returns {@code true} when a widget has consumed the frame (e.g., a
 * navigation action fired after a click-feedback delay). When this occurs, the screen <em>must</em>
 * return from {@code render()} immediately without issuing any draw calls — a screen transition may
 * already be in progress:
 *
 * <pre>{@code
 * public void render(float delta) {
 *     if (uiLayer.update(delta)) return;
 *     // ... draw ...
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

  private final Vector3 pointer = new Vector3();

  /**
   * A world point guaranteed to be outside every widget — used to clear hover on occluded widgets.
   */
  private static final float OCCLUDED_X = -1e9f;

  private static final float OCCLUDED_Y = -1e9f;

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
    widget.layout();
  }

  /** Removes {@code widget} from this layer. No-op if the widget is not present. */
  public void remove(UiWidget widget) {
    entries.removeIf(e -> e.widget == widget);
    rebuildSorted();
  }

  /** Removes all widgets from this layer. */
  public void clear() {
    entries.clear();
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
   * Calls {@link UiWidget#layout()} on all widgets in ascending z-order. Call from the screen's
   * {@code resize()} after updating the viewport.
   */
  public void layout() {
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
    pointer.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
    viewport.unproject(pointer);
    int blockZ = blockingZForPointer();
    for (Entry e : ascending) {
      if (e.zOrder < blockZ) {
        // Occluded by a higher-z opaque surface — clear hover by polling
        // a point that lies outside every widget's bounds.
        e.widget.updateHover(OCCLUDED_X, OCCLUDED_Y);
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
   * Returns the z-order of the highest opaque widget currently occluding the pointer, or {@link
   * Integer#MIN_VALUE} if nothing blocks it. Widgets with a strictly lower z-order than the
   * returned value are considered occluded and must not receive hover or outline treatment this
   * frame.
   *
   * <p>Polls the cursor afresh so it is safe to call from both {@link #update(float)} and the
   * outline-capture pass.
   */
  private int blockingZForPointer() {
    pointer.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
    viewport.unproject(pointer);
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
   * Draws all widgets in ascending z-order (lowest z drawn first/behind). Must be called inside a
   * {@code batch.begin()} / {@code batch.end()} block.
   */
  public void draw(PolygonSpriteBatch batch) {
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
        pointer.set(screenX, screenY, 0f);
        viewport.unproject(pointer);
        for (Entry e : descending) {
          if (e.widget.hit(pointer.x, pointer.y)) {
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
        pointer.set(screenX, screenY, 0f);
        viewport.unproject(pointer);
        dragTarget.dragged(pointer.x, pointer.y);
        return true;
      }

      @Override
      public boolean touchUp(int screenX, int screenY, int pointerId, int button) {
        dragTarget = null;
        return false;
      }

      @Override
      public boolean scrolled(float amountX, float amountY) {
        pointer.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
        viewport.unproject(pointer);
        for (Entry e : descending) {
          if (e.widget.scrolled(pointer.x, pointer.y, amountX, amountY)) return true;
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
