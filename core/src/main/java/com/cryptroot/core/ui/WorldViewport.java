package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cryptroot.core.GameContext;
import com.cryptroot.core.event.Signal;
import com.cryptroot.core.render.RenderPipeline;
import com.cryptroot.core.render.SceneTransform;
import com.cryptroot.core.ui.layout.Clippable;
import com.cryptroot.core.world.BoundsComponent;
import com.cryptroot.core.world.RenderComponent;
import com.cryptroot.core.world.TooltipComponent;
import com.cryptroot.core.world.World;
import com.cryptroot.core.world.WorldEntity;
import java.util.Objects;

/**
 * A {@link World} rendered inside a UI rectangle: pan, zoom, clip, hover and click, with the
 * ordinary entity/component/render-pass machinery inside rather than a second content model beside
 * it.
 *
 * <p>This is the engine's answer to "show the game inside a panel". A {@link World} is the one
 * description of what exists; {@link RenderPipeline} is the one thing that draws it; {@link
 * com.cryptroot.core.render.system.HoverSystem HoverSystem} and {@link
 * com.cryptroot.core.render.system.ClickSystem ClickSystem} are the one way it is picked. This
 * widget adds a destination rectangle and a {@link SceneTransform}, and otherwise gets out of the
 * way — so anything that works in a full-screen scene (animation via {@code UpdateComponent},
 * Y-sorting, collision, hover outlines, tinting, Spine skeletons) works unchanged when that scene
 * is a box in the corner of a menu. Nothing needs to be re-expressed as a UI-only sprite type.
 *
 * <pre>{@code
 * WorldViewport viewport = new WorldViewport(context, world, skin, pixel);
 * viewport.setSceneBounds(0f, -768f, 1536f, 768f);
 * viewport.setZoomRange(0.5f, 3f);
 * viewport.onEntityClicked.connect(entity -> handle(entity));
 * panel.setContent(viewport);
 * }</pre>
 *
 * <h3>Why a transform and not a camera</h3>
 *
 * The scene is drawn by composing the {@link SceneTransform} into the batch's <em>transform</em>
 * matrix, leaving the enclosing projection — the one {@link UiLayer} lays its widgets out in and
 * asserts on — untouched. A second {@link com.badlogic.gdx.graphics.OrthographicCamera} would have
 * to replace that projection, which is both a contract violation waiting to happen and impossible
 * to nest. Composing instead means zoom is free, several viewports can show the same world at once,
 * and a viewport inside a viewport still clips and transforms correctly.
 *
 * <h3>Pan and click</h3>
 *
 * Dragging pans (zoom-aware, clamped by {@link #setSceneBounds}). A press that travels further than
 * {@link #CLICK_SLOP} before release is a pan and fires nothing; a press that does not is a click,
 * dispatched on release through {@link RenderPipeline#clickAt} so the hit entity's own {@link
 * com.cryptroot.core.world.ClickableComponent ClickableComponent} fires — {@link #onEntityClicked}
 * is a convenience for callers that would rather handle it centrally.
 *
 * <h3>Hover outlines</h3>
 *
 * The widget contributes its outlined entities to {@link UiLayer}'s existing single-pass outline
 * capture as an {@link OutlineCaptureSource}, drawing them under the same scene transform, rather
 * than starting a competing capture — {@link com.cryptroot.core.render.SelectionOutlineRenderer}
 * owns one shared FBO. Targets whose bounds fall outside the visible scene rect are skipped; an
 * entity straddling the edge can ring a few pixels outside the widget, since the blit happens after
 * the clip closes.
 */
public final class WorldViewport extends BoundedWidget implements Clippable, OutlineCaptureSource {

  /** Total press-to-release travel, in UI units, beyond which a press becomes a pure pan. */
  public static final float CLICK_SLOP = 6f;

  private static final float ZOOM_STEP = 0.1f;
  private static final float TOOLTIP_PADDING = 6f;
  private static final float TOOLTIP_OFFSET_X = 14f;
  private static final float TOOLTIP_OFFSET_Y = 14f;

  private static final Color COLOR_BG = new Color(0.10f, 0.10f, 0.16f, 1f);
  private static final Color COLOR_BORDER = new Color(0.5f, 0.5f, 0.5f, 1f);
  private static final Color COLOR_TOOLTIP_BG = new Color(0.05f, 0.05f, 0.08f, 0.95f);

  /** Fires with the entity that was pressed and released without exceeding {@link #CLICK_SLOP}. */
  public final Signal<WorldEntity> onEntityClicked = new Signal<>();

  private final World world;
  private final RenderPipeline pipeline;
  private final SceneTransform transform = new SceneTransform();

  private final UiSkin skin;
  private final ScissorRegion scissor = new ScissorRegion();
  private final PixelBorder border;
  private final PixelRect bg;
  private final PixelRect tooltipBg;
  private final TextLabel tooltipLabel;

  /** Reused every frame so drawing allocates nothing. */
  private final Matrix4 sceneMatrix = new Matrix4();

  private final Matrix4 savedMatrix = new Matrix4();
  private final Rectangle boundsScratch = new Rectangle();
  private final Vector2 scratch = new Vector2();

  private boolean panEnabled = true;
  private boolean zoomEnabled = true;
  private boolean autoUpdate = true;

  private float pointerX;
  private float pointerY;

  private boolean pressed;
  private boolean dragExceededSlop;
  private float pressX;
  private float pressY;
  private float lastDragX;
  private float lastDragY;

  /**
   * @param context supplies the shared batch, viewport and outline renderer the scene draws with
   * @param world the entities shown; owned by the caller and safe to mutate between frames
   * @param skin provides the font for the hover tooltip
   * @param pixel 1×1 white pixel texture for the background, border and tooltip backing
   */
  public WorldViewport(GameContext context, World world, UiSkin skin, Texture pixel) {
    Objects.requireNonNull(context, "context must not be null");
    this.world = Objects.requireNonNull(world, "world must not be null");
    this.skin = Objects.requireNonNull(skin, "skin must not be null");
    Objects.requireNonNull(pixel, "pixel must not be null");
    this.pipeline = new RenderPipeline(context);
    this.border = new PixelBorder(pixel, 2f, COLOR_BORDER.cpy());
    this.bg = new PixelRect(pixel, COLOR_BG.cpy());
    this.tooltipBg = new PixelRect(pixel, COLOR_TOOLTIP_BG.cpy());
    this.tooltipLabel = new TextLabel(skin.font(), "");
  }

  // -------------------------------------------------------------------------
  // Public API
  // -------------------------------------------------------------------------

  /** The world this viewport shows. Add, remove and mutate entities on it directly. */
  public World world() {
    return world;
  }

  /**
   * The pan/zoom state, for callers that want to drive the view directly (animate a zoom, follow an
   * entity, restore a saved view).
   */
  public SceneTransform view() {
    return transform;
  }

  /** The pipeline driving this scene, for callers that need its systems (collision, dialogue). */
  public RenderPipeline pipeline() {
    return pipeline;
  }

  /**
   * Constrains panning and zooming to the given scene extents. Without this the view pans freely.
   *
   * @throws IllegalArgumentException if either axis has {@code max < min}
   */
  public void setSceneBounds(float minX, float minY, float maxX, float maxY) {
    transform.setSceneBounds(minX, minY, maxX, maxY);
  }

  /**
   * Sets the permitted zoom range, where {@code 1} draws the scene at its natural size and larger
   * values magnify.
   *
   * @throws IllegalArgumentException if {@code min} is not positive, or {@code max < min}
   */
  public void setZoomRange(float min, float max) {
    transform.setZoomRange(min, max);
  }

  /** Pans so scene point ({@code sceneX}, {@code sceneY}) is centred in the viewport. */
  public void centreOn(float sceneX, float sceneY) {
    transform.centreOn(sceneX, sceneY);
  }

  /** Whether dragging inside the viewport pans the scene. Defaults to {@code true}. */
  public void setPanEnabled(boolean enabled) {
    this.panEnabled = enabled;
  }

  /** Whether the scroll wheel zooms towards the cursor. Defaults to {@code true}. */
  public void setZoomEnabled(boolean enabled) {
    this.zoomEnabled = enabled;
  }

  /**
   * Whether this widget ticks the world's {@code UpdateComponent}s and collision each frame.
   * Defaults to {@code true}.
   *
   * <p>Set {@code false} when the same world is already being simulated elsewhere — two viewports
   * onto one world, or a viewport onto the world a {@link com.cryptroot.core.screen.BaseGameScreen}
   * is already running — so it is not stepped twice per frame.
   */
  public void setAutoUpdate(boolean enabled) {
    this.autoUpdate = enabled;
  }

  /** The entity currently under the pointer, or {@code null}. */
  public WorldEntity hoveredEntity() {
    return hovered ? pipeline.hoveredEntity() : null;
  }

  /** Converts a scene point to this widget's UI coordinates. Returns {@code out}. */
  public Vector2 sceneToView(float sceneX, float sceneY, Vector2 out) {
    return transform.sceneToView(sceneX, sceneY, out);
  }

  /** Converts a UI point to scene coordinates. Returns {@code out}. */
  public Vector2 viewToScene(float viewX, float viewY, Vector2 out) {
    return transform.viewToScene(viewX, viewY, out);
  }

  // -------------------------------------------------------------------------
  // Clippable
  // -------------------------------------------------------------------------

  @Override
  public void setClipContext(Viewport viewport, Camera camera) {
    scissor.setClipContext(viewport, camera);
  }

  // -------------------------------------------------------------------------
  // CompositeWidget template methods
  // -------------------------------------------------------------------------

  @Override
  protected void doBoundedLayout() {
    bounds.set(frame);
    border.setBounds(frame.x, frame.y, frame.width, frame.height);
    bg.setBounds(frame.x, frame.y, frame.width, frame.height);
    scissor.setBounds(frame.x, frame.y, frame.width, frame.height);
    transform.setView(frame.x, frame.y, frame.width, frame.height);
  }

  /** Draws the background unclipped, then the clipped scene under the pan/zoom transform. */
  @Override
  protected void doDraw(PolygonSpriteBatch batch) {
    bg.draw(batch);

    if (!scissor.begin(batch)) return;

    // Compose rather than replace: the enclosing projection (and any parent transform) stays
    // intact, so UiLayer's projection contract holds and nested viewports still work.
    savedMatrix.set(batch.getTransformMatrix());
    batch.setTransformMatrix(transform.applyTo(sceneMatrix, savedMatrix));
    pipeline.drawSceneInto(world, batch);
    batch.setTransformMatrix(savedMatrix);
  }

  /** Closes the clip region, draws the border on top of it, then the hover tooltip. */
  @Override
  protected void doAfterDraw(PolygonSpriteBatch batch) {
    scissor.end(batch);
    border.draw(batch);
    drawTooltip(batch);
  }

  private void drawTooltip(PolygonSpriteBatch batch) {
    WorldEntity entity = hoveredEntity();
    if (entity == null) return;
    String text = entity.get(TooltipComponent.class).map(TooltipComponent::tooltip).orElse(null);
    if (text == null || text.isEmpty()) return;

    float boxX = pointerX + TOOLTIP_OFFSET_X;
    float boxY = pointerY + TOOLTIP_OFFSET_Y;
    tooltipLabel.setText(text);
    tooltipLabel.setPosition(boxX + TOOLTIP_PADDING, boxY + TOOLTIP_PADDING);

    float boxWidth = tooltipLabel.getMeasuredWidth() + TOOLTIP_PADDING * 2f;
    float boxHeight = UiHelper.barHeight(skin.font(), TOOLTIP_PADDING);
    tooltipBg.setBounds(boxX, boxY, boxWidth, boxHeight);
    tooltipBg.draw(batch);
    tooltipLabel.drawWithColor(batch, Color.WHITE);
  }

  @Override
  protected void doBoundedReset() {
    pressed = false;
    dragExceededSlop = false;
    pipeline.reset();
  }

  // -------------------------------------------------------------------------
  // Frame update
  // -------------------------------------------------------------------------

  /**
   * Ticks the hosted scene: entity updates, collision, and hover — the same sequence {@link
   * com.cryptroot.core.screen.BaseGameScreen} runs, minus the render call, which happens later in
   * {@link #doDraw}.
   */
  @Override
  public boolean update(float delta) {
    if (super.update(delta)) return true;
    if (autoUpdate) {
      pipeline.update(world, delta);
      pipeline.processCollisions(world);
    }
    if (hovered) {
      pipeline.processHover(
          world, transform.viewToSceneX(pointerX), transform.viewToSceneY(pointerY), delta);
    } else {
      // A point no finite entity can contain: clears hover and lets the outline fade out.
      pipeline.processHover(world, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, delta);
    }
    return false;
  }

  // -------------------------------------------------------------------------
  // Pointer input
  // -------------------------------------------------------------------------

  @Override
  public void updateHover(float worldX, float worldY) {
    hovered = bounds.contains(worldX, worldY);
    pointerX = worldX;
    pointerY = worldY;
  }

  /** Opaque: this widget always paints a background, so it occludes whatever is beneath it. */
  @Override
  public boolean blocksPointer(float worldX, float worldY) {
    return bounds.contains(worldX, worldY);
  }

  @Override
  public boolean hit(float worldX, float worldY) {
    if (!bounds.contains(worldX, worldY)) return false;
    pressed = true;
    dragExceededSlop = false;
    pressX = worldX;
    pressY = worldY;
    lastDragX = worldX;
    lastDragY = worldY;
    return true;
  }

  /**
   * Pans by the delta since the last drag point, and cancels the pending click once total travel
   * from the press point exceeds {@link #CLICK_SLOP} — so a press that wobbles a couple of pixels
   * still counts as a click, but a real pan gesture never also fires one.
   */
  @Override
  public void dragged(float worldX, float worldY) {
    if (!pressed) return;

    if (panEnabled) {
      transform.panByView(worldX - lastDragX, worldY - lastDragY);
    }
    lastDragX = worldX;
    lastDragY = worldY;

    if (!dragExceededSlop
        && (Math.abs(worldX - pressX) > CLICK_SLOP || Math.abs(worldY - pressY) > CLICK_SLOP)) {
      dragExceededSlop = true;
    }
  }

  /** Dispatches the click if the gesture stayed within {@link #CLICK_SLOP} and ended inside. */
  @Override
  public void released(float worldX, float worldY) {
    boolean wasClick = pressed && !dragExceededSlop && bounds.contains(worldX, worldY);
    pressed = false;
    dragExceededSlop = false;
    if (!wasClick) return;

    pipeline
        .clickAt(world, transform.viewToSceneX(worldX), transform.viewToSceneY(worldY))
        .ifPresent(onEntityClicked::emit);
  }

  @Override
  public boolean scrolled(float worldX, float worldY, float amountX, float amountY) {
    if (!zoomEnabled || !bounds.contains(worldX, worldY)) return false;
    // amountY > 0 = scroll down = zoom out.
    transform.zoomAt(amountY > 0f ? (1f - ZOOM_STEP) : (1f + ZOOM_STEP), worldX, worldY);
    return true;
  }

  // -------------------------------------------------------------------------
  // OutlineCaptureSource
  // -------------------------------------------------------------------------

  @Override
  public boolean outlineActive() {
    return pipeline.hoverAlpha() > 0f && !pipeline.outlineTargets(world).isEmpty();
  }

  @Override
  public float outlineAlpha() {
    return pipeline.hoverAlpha();
  }

  /**
   * Draws the outlined entities into the shared FBO at the position they occupy on screen, by
   * applying the same scene transform used for the normal pass. The capture batch is opened by
   * {@link com.cryptroot.core.render.SelectionOutlineRenderer} with the UI projection, so composing
   * onto it lands the pixels exactly where the ring is expected.
   */
  @Override
  public void drawForCapture(PolygonSpriteBatch batch) {
    Rectangle visible = transform.visibleSceneRect();
    savedMatrix.set(batch.getTransformMatrix());
    batch.setTransformMatrix(transform.applyTo(sceneMatrix, savedMatrix));
    for (WorldEntity entity : pipeline.outlineTargets(world)) {
      if (!isVisibleInScene(entity, visible)) continue;
      entity.get(RenderComponent.class).ifPresent(rc -> rc.draw(batch));
    }
    batch.setTransformMatrix(savedMatrix);
  }

  private boolean isVisibleInScene(WorldEntity entity, Rectangle visible) {
    return entity
        .get(BoundsComponent.class)
        .map(b -> visible.overlaps(b.bounds(boundsScratch)))
        .orElse(true);
  }
}
