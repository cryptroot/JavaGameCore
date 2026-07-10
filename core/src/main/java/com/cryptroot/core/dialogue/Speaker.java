package com.cryptroot.core.dialogue;

import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;

/**
 * Abstraction for a character that participates in a dialogue conversation.
 *
 * <p>A {@code Speaker} encapsulates both the renderable representation of a character (Spine
 * skeleton, static PNG, etc.) and the metadata needed to drive conversation display (display name,
 * idle/talk animation switching).
 *
 * <h3>Known implementations</h3>
 *
 * <ul>
 *   <li>{@link SpineSpeaker} — Spine-animated character.
 *   <li>{@link StaticSpeaker} — Single static or swappable PNG portrait.
 * </ul>
 */
public interface Speaker {

  /** Display name shown in the message box header. */
  String name();

  /**
   * Ticks the internal animation/render state forward. Called every frame by the owning widget's
   * {@code update()} method.
   */
  void update(float delta);

  /**
   * Renders the character graphic into the currently open {@link PolygonSpriteBatch}. Must be
   * called inside a {@code batch.begin()} / {@code batch.end()} block.
   */
  void draw(PolygonSpriteBatch batch);

  /**
   * Sets the world-space foot (origin) position of the character. Takes effect on the next {@link
   * #draw} call.
   */
  void setPosition(float x, float y);

  /**
   * Scales the character so that its height matches {@code targetHeight}, clamped so it never
   * exceeds {@code maxWidth}.
   *
   * <p>This method must also re-apply the current {@link #setMirrorX} state so callers do not need
   * to repeat it after a layout pass.
   */
  void fitHeight(float targetHeight, float maxWidth);

  /**
   * When {@code true}, mirrors the sprite on the X axis (faces left instead of right). Must be
   * persisted across {@link #fitHeight} calls.
   */
  void setMirrorX(boolean mirror);

  /**
   * Plays the animation mapped to {@code key} (e.g. {@code "idle"}, {@code "talk"}). If the key has
   * no mapping, or the mapped animation does not exist, implementations must fall back gracefully
   * (typically to idle) without throwing.
   *
   * <p>This is the primary extension point for animation state changes. Use the convenience
   * defaults {@link #startSpeaking()} and {@link #stopSpeaking()} for the two most common
   * transitions.
   */
  void playAnimation(String key);

  /**
   * Convenience: plays the animation mapped to {@code "talk"}. Falls back to idle silently when no
   * talk animation is available.
   */
  default void startSpeaking() {
    playAnimation("talk");
  }

  /** Convenience: plays the animation mapped to {@code "idle"}. */
  default void stopSpeaking() {
    playAnimation("idle");
  }
}
