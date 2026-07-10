package com.cryptroot.core.dialogue;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link Speaker} backed by static PNG textures mapped to semantic keys.
 *
 * <p>Each animation state (e.g. {@code "idle"}, {@code "talk"}) resolves to a {@link Texture} via
 * an internal map. Calling {@link #playAnimation(String)} swaps the displayed texture to whichever
 * entry matches the key, falling back to the idle texture when the key is unmapped.
 *
 * <h3>Default map</h3>
 *
 * <pre>
 *   "idle" → idleTexture
 *   "talk" → talkTexture  (same as idle when none is supplied)
 * </pre>
 *
 * <h3>Custom mappings</h3>
 *
 * <pre>{@code
 * speaker.mapAnimation("surprised", surprisedTexture);
 * speaker.playAnimation("surprised");   // swaps to surprisedTexture
 * }</pre>
 */
public final class StaticSpeaker implements Speaker {

  private final String displayName;
  private final Texture idleTexture; // hard fallback for missing keys
  private final Map<String, Texture> textureMap = new HashMap<>();

  private Texture currentTexture;

  private float x;
  private float y;
  private float drawW;
  private float drawH;
  private boolean mirrorX = false;

  /** Creates a static speaker that uses {@code idleTexture} for all states. */
  public StaticSpeaker(String displayName, Texture idleTexture) {
    this(displayName, idleTexture, null);
  }

  /**
   * Creates a static speaker with separate idle and talking textures.
   *
   * @param displayName display name shown in the message box header
   * @param idleTexture texture shown at rest; used as fallback for unmapped keys
   * @param talkTexture texture shown while speaking; {@code null} falls back to idle
   */
  public StaticSpeaker(String displayName, Texture idleTexture, Texture talkTexture) {
    this.displayName = displayName;
    this.idleTexture = idleTexture;
    this.currentTexture = idleTexture;
    textureMap.put("idle", idleTexture);
    textureMap.put("talk", talkTexture != null ? talkTexture : idleTexture);
  }

  /**
   * Adds or replaces a mapping from a semantic animation key to a texture. Overrides any existing
   * entry for {@code key}.
   *
   * @param key semantic name used by {@link #playAnimation(String)}
   * @param texture texture to display when this key is active
   * @return {@code this} for fluent chaining
   */
  public StaticSpeaker mapAnimation(String key, Texture texture) {
    textureMap.put(key, texture);
    return this;
  }

  // -------------------------------------------------------------------------
  // Speaker
  // -------------------------------------------------------------------------

  @Override
  public String name() {
    return displayName;
  }

  @Override
  public void update(float delta) {
    // Static textures need no per-frame state update.
  }

  @Override
  public void draw(PolygonSpriteBatch batch) {
    if (drawW <= 0 || drawH <= 0) return;
    float drawX = mirrorX ? x + drawW : x;
    float scaleX = mirrorX ? -1f : 1f;
    // Draw centred on x: shift left by half of width.
    batch.draw(
        currentTexture,
        drawX - drawW / 2f,
        y,
        0,
        0,
        drawW,
        drawH,
        scaleX,
        1f,
        0f,
        0,
        0,
        currentTexture.getWidth(),
        currentTexture.getHeight(),
        false,
        false);
  }

  @Override
  public void setPosition(float x, float y) {
    this.x = x;
    this.y = y;
  }

  @Override
  public void fitHeight(float targetHeight, float maxWidth) {
    if (currentTexture == null) return;
    float texW = currentTexture.getWidth();
    float texH = currentTexture.getHeight();
    if (texH <= 0) return;

    float scale = targetHeight / texH;
    drawH = targetHeight;
    drawW = texW * scale;

    if (maxWidth > 0 && drawW > maxWidth) {
      float clamp = maxWidth / drawW;
      drawW *= clamp;
      drawH *= clamp;
    }
    drawW = MathUtils.clamp(drawW, 0, maxWidth > 0 ? maxWidth : Float.MAX_VALUE);
  }

  @Override
  public void setMirrorX(boolean mirror) {
    this.mirrorX = mirror;
  }

  /**
   * Looks up {@code key} in the texture map and swaps to that texture. Falls back to the idle
   * texture when the key has no mapping.
   */
  @Override
  public void playAnimation(String key) {
    Texture mapped = textureMap.get(key);
    currentTexture = (mapped != null) ? mapped : idleTexture;
  }
}
