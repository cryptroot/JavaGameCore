package com.cryptroot.core.world.component;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.cryptroot.core.render.RenderPass;
import com.cryptroot.core.world.PositionComponent;
import com.cryptroot.core.world.RenderComponent;
import com.cryptroot.core.world.UpdateComponent;

/**
 * A world-space health bar that floats above an entity — the framework form of Unity's procedural
 * {@code UnitHealthBar} (a dark backing plus a coloured fill that shrinks and shifts green→red as
 * the fraction drops). Distinct from the screen-space {@link com.cryptroot.core.ui.ProgressBar}:
 * this one lives in the world, anchored to a {@link PositionComponent}, and draws in {@link
 * RenderPass#FOREGROUND_WORLD} (above the world, not Y-sorted).
 *
 * <p>Push-model: the owning unit calls {@link #setFraction} when its HP changes and {@link
 * #setVisible} per phase. Both {@link RenderComponent} and {@link UpdateComponent} are implemented;
 * {@link #update} eases the displayed fraction toward the target so a hit reads as a smooth drain.
 *
 * <p>Drawn from a 1×1 white pixel region (see {@link
 * com.cryptroot.core.resources.ResourceManager#getPixelTexture()}), scaled to size — no textures of
 * its own.
 */
public final class WorldHealthBarComponent implements RenderComponent, UpdateComponent {

  /** Colours/geometry of the bar. {@link #defaults} supplies the Unity-style palette. */
  public record Config(
      float width,
      float height,
      float offsetX,
      float offsetY,
      Color full,
      Color empty,
      Color background) {

    public static Config defaults(float width, float height, float offsetX, float offsetY) {
      return new Config(
          width,
          height,
          offsetX,
          offsetY,
          new Color(0.35f, 0.9f, 0.4f, 1f), // full  = green
          new Color(0.9f, 0.25f, 0.25f, 1f), // empty = red
          new Color(0.08f, 0.08f, 0.1f, 0.85f)); // backing
    }
  }

  /** Units of displayed fraction closed per second when easing toward the target. */
  private static final float EASE_PER_SEC = 6f;

  private final TextureRegion pixel;
  private final PositionComponent anchor;
  private final Config cfg;
  private final Color fillScratch = new Color();
  private final Color saveScratch = new Color();

  private float target = 1f;
  private float displayed = 1f;
  private boolean visible = true;

  public WorldHealthBarComponent(TextureRegion pixel, PositionComponent anchor, Config cfg) {
    this.pixel = pixel;
    this.anchor = anchor;
    this.cfg = cfg;
  }

  /** Sets the target HP fraction (clamped 0..1). */
  public void setFraction(float f) {
    target = MathUtils.clamp(f, 0f, 1f);
  }

  /** Snaps the displayed fraction to the target immediately (e.g. on reset). */
  public void snap() {
    displayed = target;
  }

  public float fraction() {
    return target;
  }

  public void setVisible(boolean v) {
    visible = v;
  }

  public boolean isVisible() {
    return visible;
  }

  @Override
  public void update(float delta) {
    float step = EASE_PER_SEC * delta;
    if (Math.abs(target - displayed) <= step) {
      displayed = target;
    } else {
      displayed += Math.signum(target - displayed) * step;
    }
  }

  @Override
  public void draw(PolygonSpriteBatch batch) {
    if (!visible) return;

    float w = cfg.width();
    float h = cfg.height();
    float left = anchor.x() + cfg.offsetX() - w * 0.5f;
    float bottom = anchor.y() + cfg.offsetY();

    Color prev = saveScratch.set(batch.getColor());

    batch.setColor(cfg.background());
    batch.draw(pixel, left, bottom, w, h);

    float frac = MathUtils.clamp(displayed, 0f, 1f);
    batch.setColor(barColor(frac, cfg.full(), cfg.empty(), fillScratch));
    batch.draw(pixel, left, bottom, w * frac, h);

    batch.setColor(prev);
  }

  @Override
  public RenderPass renderPass() {
    return RenderPass.FOREGROUND_WORLD;
  }

  @Override
  public float sortKey() {
    return 0f;
  }

  /** Fill colour for a fraction between {@code empty} (0) and {@code full} (1). */
  public static Color barColor(float fraction, Color full, Color empty, Color out) {
    return out.set(empty).lerp(full, MathUtils.clamp(fraction, 0f, 1f));
  }

  /** Default green→red fill colour for a fraction (Unity-style palette). */
  public static Color barColor(float fraction, Color out) {
    return barColor(
        fraction, new Color(0.35f, 0.9f, 0.4f, 1f), new Color(0.9f, 0.25f, 0.25f, 1f), out);
  }
}
