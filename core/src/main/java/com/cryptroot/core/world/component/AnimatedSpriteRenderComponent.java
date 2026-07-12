package com.cryptroot.core.world.component;

import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.cryptroot.core.render.RenderPass;
import com.cryptroot.core.render.SpriteAnimation;
import com.cryptroot.core.world.PositionComponent;
import com.cryptroot.core.world.RenderComponent;

/**
 * Renders a {@link SpriteAnimation}'s current frame at a configurable world position.
 *
 * <p>Implements both {@link RenderComponent} and {@link PositionComponent}, mirroring {@link
 * TextureRenderComponent} but drawing {@link SpriteAnimation#currentFrame()} every frame instead of
 * a single static region. Moving the entity via {@link #moveTo} repositions the drawn quad.
 *
 * <p>The animation itself is driven externally — call {@link #advance(float)} once per frame
 * (typically from a game-specific {@link com.cryptroot.core.world.UpdateComponent}, since an entity
 * may only register one) and {@link #play()}/{@link #idle()} to start/stop the flipbook, e.g. based
 * on whether the entity is currently moving.
 *
 * <p>The {@link #sortKey()} returns the entity's world Y ({@link #y()}), so animated sprites placed
 * in the {@link RenderPass#WORLD} layer participate correctly in painter's-algorithm Y-sorting.
 */
public final class AnimatedSpriteRenderComponent implements RenderComponent, PositionComponent {

  private final SpriteAnimation animation;
  private float x;
  private float y;
  private final float width;
  private final float height;
  private final RenderPass renderPass;

  /**
   * @param animation the flipbook player to draw and drive
   * @param x bottom-left world X
   * @param y bottom-left world Y
   * @param width draw width in world units
   * @param height draw height in world units
   * @param renderPass which render pass this entity belongs to
   */
  public AnimatedSpriteRenderComponent(
      SpriteAnimation animation,
      float x,
      float y,
      float width,
      float height,
      RenderPass renderPass) {
    this.animation = animation;
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
    this.renderPass = renderPass;
  }

  /** Advances the underlying animation's playback time by {@code delta} seconds. */
  public void advance(float delta) {
    animation.advance(delta);
  }

  /** Starts/resumes the flipbook (e.g. while the entity is moving). */
  public void play() {
    animation.play();
  }

  /** Stops the flipbook and rewinds it to its static first frame (the idle pose). */
  public void idle() {
    animation.idle();
  }

  /** Returns {@code true} if the underlying flipbook is currently playing. */
  public boolean isPlaying() {
    return animation.isPlaying();
  }

  // -------------------------------------------------------------------------
  // RenderComponent
  // -------------------------------------------------------------------------

  @Override
  public void draw(PolygonSpriteBatch batch) {
    batch.draw(animation.currentFrame(), x, y, width, height);
  }

  @Override
  public RenderPass renderPass() {
    return renderPass;
  }

  @Override
  public float sortKey() {
    return y;
  }

  // -------------------------------------------------------------------------
  // PositionComponent
  // -------------------------------------------------------------------------

  @Override
  public float x() {
    return x;
  }

  @Override
  public float y() {
    return y;
  }

  @Override
  public void moveTo(float x, float y) {
    this.x = x;
    this.y = y;
  }
}
