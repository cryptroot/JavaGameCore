package com.cryptroot.core.world.component;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.cryptroot.core.render.RenderPass;
import com.cryptroot.core.world.RenderComponent;
import com.cryptroot.core.world.UpdateComponent;
import java.util.Objects;

/**
 * A hit-flash tint that wraps any {@link RenderComponent}. It reproduces the Unity pattern of
 * tinting {@code SpriteRenderer.color} to a flash colour on damage and lerping it back to the base
 * colour over a short duration.
 *
 * <p>It is a <em>decorator</em> rather than a subclass because {@link TextureRenderComponent} (and
 * the tiled layer component) are {@code final}: {@link #draw} sets the batch colour around the
 * delegate's own draw call (the batch multiplies every vertex by its colour), so tint works with
 * any render component without touching existing code. Register the decorator under {@link
 * RenderComponent} and {@link UpdateComponent}; register the inner component under {@link
 * com.cryptroot.core.world.PositionComponent} if it carries position.
 */
public final class TintFlashRenderComponent implements RenderComponent, UpdateComponent {

  private final RenderComponent delegate;
  private final Color baseColor = new Color(Color.WHITE);
  private final Color flashColor = new Color(Color.WHITE);
  private final Color tint = new Color(Color.WHITE);
  private final Color scratch = new Color();

  private float elapsed;
  private float duration; // 0 = not flashing

  public TintFlashRenderComponent(RenderComponent delegate) {
    Objects.requireNonNull(delegate, "delegate must not be null");
    this.delegate = delegate;
  }

  /** Sets the resting colour the flash decays back to (default white). */
  public void setBaseColor(Color base) {
    Objects.requireNonNull(base, "base must not be null");
    baseColor.set(base);
    if (duration <= 0f) tint.set(base);
  }

  /** Starts a flash from {@code flashColor} decaying to the base over {@code durationSec}. */
  public void flash(Color flashColor, float durationSec) {
    Objects.requireNonNull(flashColor, "flashColor must not be null");
    if (durationSec <= 0f) {
      tint.set(baseColor);
      duration = 0f;
      return;
    }
    this.duration = durationSec;
    this.elapsed = 0f;
    this.flashColor.set(flashColor);
    tint.set(flashColor);
  }

  public boolean isFlashing() {
    return duration > 0f;
  }

  @Override
  public void update(float delta) {
    if (duration <= 0f) return;
    elapsed += delta;
    if (elapsed >= duration) {
      tint.set(baseColor);
      duration = 0f;
    } else {
      tintAt(flashColor, baseColor, elapsed, duration, tint);
    }
  }

  @Override
  public void draw(PolygonSpriteBatch batch) {
    if (duration <= 0f && tint.equals(baseColor) && baseColor.equals(Color.WHITE)) {
      delegate.draw(batch); // fast path: no tint in effect
      return;
    }
    scratch.set(batch.getColor());
    batch.setColor(tint);
    delegate.draw(batch);
    batch.setColor(scratch);
  }

  @Override
  public RenderPass renderPass() {
    return delegate.renderPass();
  }

  @Override
  public float sortKey() {
    return delegate.sortKey();
  }

  /**
   * The tint colour at a point in a flash: {@code flash} at {@code elapsed==0}, {@code base} once
   * {@code elapsed >= duration}, linearly interpolated between. Pure and side-effect-free (writes
   * into {@code out}, returns it).
   */
  public static Color tintAt(Color flash, Color base, float elapsed, float duration, Color out) {
    if (duration <= 0f) return out.set(base);
    float t = elapsed <= 0f ? 0f : Math.min(1f, elapsed / duration);
    return out.set(flash).lerp(base, t);
  }
}
