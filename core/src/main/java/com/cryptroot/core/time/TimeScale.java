package com.cryptroot.core.time;

/**
 * A per-screen pause/speed-up multiplier applied to the frame delta before it reaches the world
 * pipeline — the framework form of the "pause the game" / "2x speed" control almost every game ends
 * up needing.
 *
 * <p>Owned by {@link com.cryptroot.core.screen.BaseGameScreen} (one instance per screen, reset
 * naturally when the screen is torn down) rather than {@link com.cryptroot.core.GameContext} —
 * pause is a per-session concern, not a whole-game one. A screen (or its input handling) calls
 * {@link #setPaused}/{@link #togglePause}/{@link #setScale} in response to a key press or UI
 * button; {@link #apply(float)} is called once per frame on the raw {@code delta} before it is
 * passed to {@link com.cryptroot.core.render.RenderPipeline#update} and {@link
 * com.cryptroot.core.render.RenderPipeline#processHover}.
 *
 * <p>Default state ({@code scale=1}, not paused) makes {@link #apply(float)} an identity function,
 * so adding this to a screen is a no-op until something actually calls a setter.
 */
public final class TimeScale {

  private float scale = 1f;
  private boolean paused;

  /** The current speed multiplier (independent of {@link #isPaused()}). */
  public float scale() {
    return scale;
  }

  /**
   * Sets the speed multiplier applied while not paused (e.g. {@code 1} normal, {@code 2} double
   * speed). Does not itself pause or unpause.
   *
   * @throws IllegalArgumentException if {@code scale} is negative
   */
  public void setScale(float scale) {
    if (scale < 0f) {
      throw new IllegalArgumentException("scale must not be negative: " + scale);
    }
    this.scale = scale;
  }

  /**
   * {@code true} if {@link #apply(float)} currently returns zero regardless of {@link #scale()}.
   */
  public boolean isPaused() {
    return paused;
  }

  public void setPaused(boolean paused) {
    this.paused = paused;
  }

  /** Flips {@link #isPaused()}. */
  public void togglePause() {
    paused = !paused;
  }

  /**
   * Returns the effective delta for this frame: zero while paused, otherwise {@code rawDelta *
   * }{@link #scale()}.
   */
  public float apply(float rawDelta) {
    return paused ? 0f : rawDelta * scale;
  }
}
