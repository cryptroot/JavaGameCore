package com.cryptroot.core.render;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

/**
 * A minimal sprite-sheet flipbook animation player.
 *
 * <p>Wraps a libGDX {@link Animation} of {@link TextureRegion} frames together with its own
 * play/pause state and elapsed playback time, so a render component can drive it every frame
 * without keeping a separate state-time field itself. Playback starts paused, showing frame 0.
 *
 * <h3>Typical use (walk cycle / idle)</h3>
 *
 * <pre>{@code
 * SpriteAnimation walk = new SpriteAnimation(frames, 0.12f, PlayMode.LOOP);
 * // per frame, while moving:
 * walk.play();
 * walk.advance(delta);
 * // when stopped:
 * walk.idle();
 * }</pre>
 *
 * <p>Construction and playback are pure Java/no GL calls (frames are only ever handed to a batch,
 * never inspected), so this class is directly unit-testable with no-op {@link TextureRegion}
 * instances (its no-arg constructor touches no {@code Texture}).
 */
public final class SpriteAnimation {

  private final Animation<TextureRegion> animation;
  private final TextureRegion firstFrame;
  private float stateTime;
  private boolean playing;

  /**
   * @param frames the flipbook frames, in playback order (must be non-empty)
   * @param frameDuration seconds each frame is shown
   * @param playMode libGDX loop behaviour ({@code LOOP} for a walk cycle, {@code NORMAL} to play
   *     once and hold the last frame)
   */
  public SpriteAnimation(TextureRegion[] frames, float frameDuration, PlayMode playMode) {
    if (frames.length == 0) {
      throw new IllegalArgumentException("frames must not be empty");
    }
    this.animation = new Animation<>(frameDuration, new Array<>(frames), playMode);
    this.firstFrame = frames[0];
  }

  /**
   * Advances playback time by {@code delta} seconds. Does nothing while {@link #isPlaying()} is
   * {@code false}.
   */
  public void advance(float delta) {
    if (playing) {
      stateTime += delta;
    }
  }

  /** Returns the frame for the current playback time (frame 0 if never started or idle). */
  public TextureRegion currentFrame() {
    return stateTime <= 0f ? firstFrame : animation.getKeyFrame(stateTime);
  }

  /** Resumes/starts playback from the current elapsed time without resetting it. */
  public void play() {
    playing = true;
  }

  /** Stops playback and rewinds to the static first frame (the idle pose). */
  public void idle() {
    playing = false;
    // TODO: Add an option for a separate playable animation for idle
    stateTime = 0f;
  }

  /** Returns {@code true} if this animation is currently advancing on {@link #advance(float)}. */
  public boolean isPlaying() {
    return playing;
  }
}
