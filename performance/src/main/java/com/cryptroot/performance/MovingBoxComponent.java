package com.cryptroot.performance;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.cryptroot.core.world.UpdateComponent;
import com.cryptroot.core.world.component.TextureRenderComponent;
import java.util.Objects;

/**
 * Per-frame behaviour for one box in the {@link BoxField} showcase: straight-line motion that
 * bounces off the arena walls, plus a brief hit-flash tint that decays back to the box's base
 * colour after {@link #flash()} is called.
 *
 * <p>Motion and flash-decay are combined into a single {@link UpdateComponent} rather than two
 * separate components, because a {@code WorldEntity} keeps only one registration per component
 * interface (see {@code WorldEntity.with}) — a box has exactly one moving/tintable thing to update
 * per frame, so there is nowhere to attach a second {@code UpdateComponent}.
 */
public final class MovingBoxComponent implements UpdateComponent {

  private static final float FLASH_DURATION = 0.15f;

  private final TextureRenderComponent render;
  private final Vector2 velocity;
  private final float boxSize;
  private final float arenaWidth;
  private final float arenaHeight;
  private final Color baseColor;
  private final Color flashColor = Color.RED.cpy();
  private final Color tintScratch = new Color();
  private float flashElapsed = FLASH_DURATION; // starts fully decayed (no flash in progress)

  /**
   * @param render the box's render/position component; its <em>current</em> tint at construction
   *     time is captured as the base colour to decay back to, so callers must call {@link
   *     TextureRenderComponent#setTint} before constructing this
   */
  public MovingBoxComponent(
      TextureRenderComponent render,
      Vector2 velocity,
      float boxSize,
      float arenaWidth,
      float arenaHeight) {
    this.render = Objects.requireNonNull(render, "render must not be null");
    this.velocity = Objects.requireNonNull(velocity, "velocity must not be null");
    if (boxSize <= 0f) {
      throw new IllegalArgumentException("boxSize must be positive, got " + boxSize);
    }
    if (arenaWidth <= 0f || arenaHeight <= 0f) {
      throw new IllegalArgumentException("arena size must be positive");
    }
    this.boxSize = boxSize;
    this.arenaWidth = arenaWidth;
    this.arenaHeight = arenaHeight;
    this.baseColor = render.tint().cpy();
  }

  /** Starts a brief red flash that decays back to the base colour over {@link #FLASH_DURATION}. */
  public void flash() {
    flashElapsed = 0f;
  }

  @Override
  public void update(float delta) {
    stepMotion(delta);
    stepFlash(delta);
  }

  private void stepMotion(float delta) {
    float x = render.x() + velocity.x * delta;
    float y = render.y() + velocity.y * delta;
    if (x < 0f || x + boxSize > arenaWidth) {
      velocity.x = -velocity.x;
      x = Math.max(0f, Math.min(x, arenaWidth - boxSize));
    }
    if (y < 0f || y + boxSize > arenaHeight) {
      velocity.y = -velocity.y;
      y = Math.max(0f, Math.min(y, arenaHeight - boxSize));
    }
    render.moveTo(x, y);
  }

  private void stepFlash(float delta) {
    if (flashElapsed >= FLASH_DURATION) return;
    flashElapsed = Math.min(FLASH_DURATION, flashElapsed + delta);
    float t = flashElapsed / FLASH_DURATION;
    tintScratch.set(flashColor).lerp(baseColor, t);
    render.setTint(tintScratch);
  }
}
