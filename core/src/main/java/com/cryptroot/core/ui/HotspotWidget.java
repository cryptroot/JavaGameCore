package com.cryptroot.core.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.cryptroot.core.event.Signal0;
import com.cryptroot.core.render.SelectionOutlineRenderer;
import java.util.Objects;

/**
 * An interactive UI widget that draws a texture overlay (e.g. a map landmark extracted from the
 * background) and adds a hover-outline glow and optional label when the pointer enters its bounds.
 *
 * <h3>Hit testing</h3>
 *
 * By default {@code HotspotWidget} performs <em>per-pixel alpha sampling</em> against the
 * underlying texture — transparent pixels are treated as misses so the outline only triggers when
 * the cursor is over a visible part of the asset. Sampling is lazy: the texture's Pixmap is
 * downloaded from the GPU (via {@link TextureData#consumePixmap()}) the first time a hover or hit
 * test is needed, then cached for the remainder of the session.
 *
 * <p>For better performance, or for textures whose data cannot be downloaded (e.g. generated
 * fallbacks), supply an explicit hit mask via {@link #setHitMask(Pixmap)}. The mask is <em>not</em>
 * owned by this widget; the caller is responsible for disposing it.
 *
 * <h3>Outline integration</h3>
 *
 * {@code HotspotWidget} implements {@link OutlineCaptureSource}. The owning screen must drive the
 * capture/blit cycle around {@code batch.begin()}:
 *
 * <pre>{@code
 * // before batch.begin:
 * uiLayer.captureOutlines(context.outlineRenderer(), batch,
 *                          context.camera().combined, context.viewport());
 * batch.begin();
 * uiLayer.draw(batch);
 * uiLayer.drawOutlines(context.outlineRenderer(), batch);
 * batch.end();
 * }</pre>
 *
 * <h3>Day/night swapping</h3>
 *
 * Call {@link #setRegion(TextureRegion)} to swap the drawn and captured region at runtime (e.g. in
 * response to a shift-change signal).
 *
 * <h3>Label</h3>
 *
 * Pass a non-null {@code labelFont} and {@code label} to show a text label centred over the opaque
 * region of the hit mask when the pointer hovers. The label fades in alongside the outline glow.
 * Label position is derived from the bounding box of opaque pixels in the effective hit mask and is
 * updated automatically when {@link #setHitMask(Pixmap)} is called.
 *
 * <h3>Disposable</h3>
 *
 * Call {@link #dispose()} when the owning panel is destroyed to release any cached Pixmap
 * downloaded from the texture.
 */
public final class HotspotWidget extends BoundedWidget implements OutlineCaptureSource, Disposable {

  /** Seconds the widget registers as "clicked" before {@link #onClick} fires. */
  private static final float CLICK_FEEDBACK_DELAY = 0.08f;

  /** Alpha threshold for a pixel to count as opaque during hit testing (0–255). */
  private static final int HIT_ALPHA_THRESHOLD = 127;

  // ---- Draw rect ----
  private final float drawX;
  private final float drawY;
  private final float drawW;
  private final float drawH;

  // ---- Visual ----
  private TextureRegion region;

  /** Font and text for the hover label. Both null suppresses the label entirely. */
  private final BitmapFont labelFont;

  private final String label;

  /** TextLabel rendered centred over the opaque region when hovered. Null until positioned. */
  private TextLabel hoverLabel;

  /** When {@code true} the label is always drawn at full opacity, not only on hover. */
  private boolean labelAlwaysVisible = false;

  /** Pre-allocated colour mutated each frame to drive the label fade. */
  private final Color labelFadeColor = new Color(1f, 1f, 1f, 0f);

  /** Pre-allocated shadow colour (black, same alpha as label). */
  private final Color labelShadowColor = new Color(0f, 0f, 0f, 0f);

  // ---- Hover fade ----
  private float hoverAlpha;

  // ---- Click state ----
  private boolean clicked;
  private float clickTimer;

  // ---- Alpha hit testing ----

  /**
   * Optional explicit hit mask (not owned by this widget). When set, takes precedence over {@link
   * #cachedAlphaMask}.
   */
  private Pixmap externalHitMask;

  /**
   * Lazily downloaded alpha mask derived from the texture. Owned by this widget; disposed in {@link
   * #dispose()}.
   */
  private Pixmap cachedAlphaMask;

  /**
   * {@code true} once a download attempt has been made so we do not retry repeatedly when the
   * texture data is unavailable.
   */
  private boolean downloadAttempted;

  /** {@code true} when the download failed; fall back to rectangular bounds. */
  private boolean downloadFailed;

  // ---- Public signal ----

  /** Fired after a short visual-feedback delay when the hotspot is clicked. */
  public final Signal0 onClick = new Signal0();

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  /**
   * Creates a hotspot without a hover label.
   *
   * @param region the overlay texture region drawn at {@code (drawX,drawY,drawW,drawH)}
   * @param drawX bottom-left world X of the draw rect (typically matches the panel X)
   * @param drawY bottom-left world Y of the draw rect
   * @param drawW draw width in world units
   * @param drawH draw height in world units
   */
  public HotspotWidget(TextureRegion region, float drawX, float drawY, float drawW, float drawH) {
    this(region, null, null, drawX, drawY, drawW, drawH);
  }

  /**
   * Factory: creates a hotspot sized by applying a uniform scale to the region's natural pixel
   * dimensions, preserving its aspect ratio.
   *
   * @param region the overlay texture region
   * @param drawX bottom-left world X of the draw rect
   * @param drawY bottom-left world Y of the draw rect
   * @param scale multiplier applied to {@link TextureRegion#getRegionWidth()} and {@link
   *     TextureRegion#getRegionHeight()}
   */
  public static HotspotWidget scaled(TextureRegion region, float drawX, float drawY, float scale) {
    Objects.requireNonNull(region, "region must not be null");
    return new HotspotWidget(
        region, drawX, drawY, region.getRegionWidth() * scale, region.getRegionHeight() * scale);
  }

  /**
   * Creates a hotspot with an optional hover label.
   *
   * @param region the overlay texture region
   * @param labelFont font for the hover label, or {@code null} to suppress
   * @param label text shown on hover, or {@code null} to suppress
   * @param drawX bottom-left world X of the draw rect
   * @param drawY bottom-left world Y of the draw rect
   * @param drawW draw width in world units
   * @param drawH draw height in world units
   */
  public HotspotWidget(
      TextureRegion region,
      BitmapFont labelFont,
      String label,
      float drawX,
      float drawY,
      float drawW,
      float drawH) {
    Objects.requireNonNull(region, "region must not be null");
    this.region = region;
    this.labelFont = labelFont;
    this.label = label;
    this.drawX = drawX;
    this.drawY = drawY;
    this.drawW = drawW;
    this.drawH = drawH;
    if (labelFont != null && label != null) {
      hoverLabel = new TextLabel(labelFont, label, 0f, 0f);
      hoverLabel.setAlign(TextLabel.HAlign.CENTER, 0f);
    }
  }

  // -------------------------------------------------------------------------
  // Configuration
  // -------------------------------------------------------------------------

  /**
   * Replaces the drawn and captured texture region. Use this for day/night swapping or any other
   * runtime region change.
   *
   * <p>Changing the region does <em>not</em> invalidate the cached alpha mask — the mask is assumed
   * to remain valid across region swaps (e.g. the day and night variants of a landmark have the
   * same opaque footprint). If the new region has a meaningfully different alpha layout, call
   * {@link #setHitMask} with an updated mask, or call {@link #clearCachedHitMask()} to force a
   * re-download on the next hover event.
   */
  public void setRegion(TextureRegion region) {
    Objects.requireNonNull(region, "region must not be null");
    this.region = region;
  }

  /**
   * When {@code true} the label is drawn at full opacity at all times, not only while the pointer
   * hovers. Has no effect if no label was supplied.
   */
  public void setLabelAlwaysVisible(boolean always) {
    this.labelAlwaysVisible = always;
  }

  /**
   * Supplies an explicit per-pixel alpha hit mask. When set, this mask is used in preference to the
   * lazily-downloaded texture data.
   *
   * <p>The Pixmap is <em>not</em> owned by this widget — the caller is responsible for disposing it
   * and for calling this method again (or {@link #clearCachedHitMask()}) if the mask changes.
   *
   * @param mask a Pixmap representing the alpha footprint of the drawn region, at any resolution
   *     (pixel coordinates are scaled to the draw rect). Pass {@code null} to revert to dynamic
   *     texture sampling.
   */
  public void setHitMask(Pixmap mask) {
    this.externalHitMask = mask;
    if (mask != null) updateLabelPosition(mask);
  }

  /**
   * Discards the internally cached alpha mask so that the next hover event triggers a fresh
   * download attempt from the current region's texture.
   */
  public void clearCachedHitMask() {
    if (cachedAlphaMask != null) {
      cachedAlphaMask.dispose();
      cachedAlphaMask = null;
    }
    downloadAttempted = false;
    downloadFailed = false;
  }

  // -------------------------------------------------------------------------
  // BoundedWidget
  // -------------------------------------------------------------------------

  @Override
  protected void doBoundedLayout() {
    bounds.set(drawX, drawY, drawW, drawH);
    if (hoverLabel != null) hoverLabel.layout();
  }

  @Override
  public void updateHover(float worldX, float worldY) {
    if (!bounds.contains(worldX, worldY)) {
      hovered = false;
      return;
    }
    hovered = isAlphaHit(worldX, worldY);
  }

  @Override
  public boolean hit(float worldX, float worldY) {
    if (!bounds.contains(worldX, worldY)) return false;
    if (!isAlphaHit(worldX, worldY)) return false;
    triggerClick();
    return true;
  }

  @Override
  protected void doDraw(PolygonSpriteBatch batch) {
    batch.setColor(Color.WHITE);
    batch.draw(region, drawX, drawY, drawW, drawH);
    // Always-visible label: draw here at full alpha so it shows even when not
    // hovered.  When hovered the outline pass will cover this draw, and
    // drawPostOutline() redraws it on top at full alpha.
    if (labelAlwaysVisible && hoverLabel != null) {
      labelFadeColor.a = 1f;
      labelShadowColor.a = 1f;
      hoverLabel.drawWithColorOffset(batch, labelShadowColor, 1f, -1f);
      hoverLabel.drawWithColor(batch, labelFadeColor);
    }
  }

  @Override
  public void drawPostOutline(PolygonSpriteBatch batch) {
    if (hoverLabel == null) return;
    float a = labelAlwaysVisible ? 1f : hoverAlpha;
    labelFadeColor.a = a;
    labelShadowColor.a = a;
    hoverLabel.drawWithColorOffset(batch, labelShadowColor, 1f, -1f);
    hoverLabel.drawWithColor(batch, labelFadeColor);
  }

  @Override
  public boolean update(float delta) {
    // Fade hover alpha toward 1 when hovered (or clicked, to keep glow visible
    // during the feedback delay), toward 0 when idle.
    float target = (hovered || clicked) ? 1f : 0f;
    float speed = SelectionOutlineRenderer.FADE_SPEED;
    if (target > hoverAlpha) {
      hoverAlpha = Math.min(1f, hoverAlpha + speed * delta);
    } else {
      hoverAlpha = Math.max(0f, hoverAlpha - speed * delta);
    }

    if (clicked) {
      clickTimer -= delta;
      if (clickTimer <= 0f) {
        clicked = false;
        onClick.emit();
        return true;
      }
    }
    return false;
  }

  @Override
  protected void doBoundedReset() {
    hoverAlpha = 0f;
    clicked = false;
    clickTimer = 0f;
  }

  // -------------------------------------------------------------------------
  // OutlineCaptureSource
  // -------------------------------------------------------------------------

  @Override
  public boolean blocksPointer(float worldX, float worldY) {
    return bounds.contains(worldX, worldY) && isAlphaHit(worldX, worldY);
  }

  @Override
  public boolean outlineActive() {
    return hoverAlpha > 0f;
  }

  @Override
  public float outlineAlpha() {
    return hoverAlpha;
  }

  @Override
  public void drawForCapture(PolygonSpriteBatch batch) {
    batch.draw(region, drawX, drawY, drawW, drawH);
  }

  // -------------------------------------------------------------------------
  // Disposable
  // -------------------------------------------------------------------------

  /**
   * Releases the internally cached alpha Pixmap (if any). Does <em>not</em> dispose the
   * externally-supplied hit mask or the texture region.
   */
  @Override
  public void dispose() {
    if (cachedAlphaMask != null) {
      cachedAlphaMask.dispose();
      cachedAlphaMask = null;
    }
  }

  // -------------------------------------------------------------------------
  // Private helpers
  // -------------------------------------------------------------------------

  private void triggerClick() {
    clicked = true;
    hovered = false;
    clickTimer = CLICK_FEEDBACK_DELAY;
  }

  /**
   * Returns {@code true} if the world coordinate hits an opaque pixel in the effective hit mask
   * (external > cached > rectangular bounds fallback).
   */
  private boolean isAlphaHit(float worldX, float worldY) {
    if (externalHitMask != null) {
      return samplePixmap(externalHitMask, worldX, worldY);
    }
    Pixmap mask = ensureCachedMask();
    if (mask != null) {
      return samplePixmap(mask, worldX, worldY);
    }
    // Download failed or texture type unsupported — treat whole bounds as opaque.
    return true;
  }

  /**
   * Lazily downloads the region's alpha data from its texture on first call.
   *
   * @return the cached Pixmap, or {@code null} if unavailable
   */
  private Pixmap ensureCachedMask() {
    if (cachedAlphaMask != null) return cachedAlphaMask;
    if (downloadAttempted) return null; // already failed; don't retry
    downloadAttempted = true;

    TextureData texData = region.getTexture().getTextureData();
    if (texData.getType() != TextureData.TextureDataType.Pixmap) {
      Gdx.app.log(
          "HotspotWidget",
          "Texture data is not Pixmap type — falling back to rectangular hit bounds. "
              + "Supply an explicit hit mask via setHitMask() for precise alpha testing.");
      downloadFailed = true;
      return null;
    }

    try {
      if (!texData.isPrepared()) texData.prepare();
      Pixmap full = texData.consumePixmap();

      int rx = region.getRegionX();
      int ry = region.getRegionY();
      int rw = region.getRegionWidth();
      int rh = region.getRegionHeight();

      if (rx == 0 && ry == 0 && rw == full.getWidth() && rh == full.getHeight()) {
        // Region covers the entire texture — use as-is.
        cachedAlphaMask = full;
      } else {
        // Crop to region bounds.
        cachedAlphaMask = new Pixmap(rw, rh, full.getFormat());
        cachedAlphaMask.drawPixmap(full, 0, 0, rx, ry, rw, rh);
        full.dispose();
      }
      updateLabelPosition(cachedAlphaMask);
    } catch (Exception e) {
      Gdx.app.log(
          "HotspotWidget",
          "Could not download texture alpha data: "
              + e.getMessage()
              + " — falling back to rectangular hit bounds.");
      downloadFailed = true;
      return null;
    }
    return cachedAlphaMask;
  }

  /**
   * Computes the axis-aligned bounding box of opaque pixels in {@code mask} and positions {@link
   * #hoverLabel} at its centre in world space. No-ops when the label has not been created or no
   * opaque pixels are found.
   */
  private void updateLabelPosition(Pixmap mask) {
    if (hoverLabel == null || mask == null) return;

    int minPx = mask.getWidth(), maxPx = -1;
    int minPy = mask.getHeight(), maxPy = -1;
    for (int py = 0; py < mask.getHeight(); py++) {
      for (int px = 0; px < mask.getWidth(); px++) {
        if ((mask.getPixel(px, py) & 0xFF) > HIT_ALPHA_THRESHOLD) {
          if (px < minPx) minPx = px;
          if (px > maxPx) maxPx = px;
          if (py < minPy) minPy = py;
          if (py > maxPy) maxPy = py;
        }
      }
    }
    if (maxPx < 0) return; // no opaque pixels

    // Map bounding-box centre to world coordinates.
    // Pixmap y=0 is at the top; world y=0 is at the bottom — flip Y.
    float cx = drawX + ((minPx + maxPx) * 0.5f / mask.getWidth()) * drawW;
    float cy = drawY + (1f - (minPy + maxPy) * 0.5f / mask.getHeight()) * drawH;

    hoverLabel.setPosition(cx, cy);
    hoverLabel.layout();
    // Shift baseline down by half the cap height so the text is visually centred at cy.
    float halfH = hoverLabel.getMeasuredHeight() * 0.5f;
    hoverLabel.setPosition(cx, cy - halfH);
    hoverLabel.layout();
  }

  /**
   * Samples {@code pixmap} at the world position, mapping it onto the draw rect.
   *
   * <p>Pixmap y=0 is at the top; world y=0 is at the bottom, so the Y axis is flipped during the
   * mapping.
   *
   * @return {@code true} if the sampled pixel's alpha exceeds {@value #HIT_ALPHA_THRESHOLD}
   */
  private boolean samplePixmap(Pixmap pixmap, float worldX, float worldY) {
    float fx = (worldX - drawX) / drawW;
    float fy = (worldY - drawY) / drawH; // 0=bottom,  1=top  in world space

    int px = (int) (fx * pixmap.getWidth());
    int py = (int) ((1f - fy) * pixmap.getHeight()); // flip Y for Pixmap

    px = Math.max(0, Math.min(pixmap.getWidth() - 1, px));
    py = Math.max(0, Math.min(pixmap.getHeight() - 1, py));

    int alpha = pixmap.getPixel(px, py) & 0xFF;
    return alpha > HIT_ALPHA_THRESHOLD;
  }
}
