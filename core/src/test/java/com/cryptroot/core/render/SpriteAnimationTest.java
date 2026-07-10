package com.cryptroot.core.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import org.junit.jupiter.api.Test;

class SpriteAnimationTest {

  /** No-arg {@link TextureRegion} touches no {@link com.badlogic.gdx.graphics.Texture}/GL. */
  private static TextureRegion[] fakeFrames(int count) {
    TextureRegion[] frames = new TextureRegion[count];
    for (int i = 0; i < count; i++) {
      frames[i] = new TextureRegion();
    }
    return frames;
  }

  @Test
  void startsPausedOnFirstFrame() {
    TextureRegion[] frames = fakeFrames(4);
    SpriteAnimation anim = new SpriteAnimation(frames, 0.1f, PlayMode.LOOP);
    assertFalse(anim.isPlaying());
    assertSame(frames[0], anim.currentFrame());
  }

  @Test
  void advanceDoesNothingWhilePaused() {
    TextureRegion[] frames = fakeFrames(4);
    SpriteAnimation anim = new SpriteAnimation(frames, 0.1f, PlayMode.LOOP);
    anim.advance(1f);
    assertSame(frames[0], anim.currentFrame());
  }

  @Test
  void playAdvancesThroughFrames() {
    TextureRegion[] frames = fakeFrames(4);
    SpriteAnimation anim = new SpriteAnimation(frames, 0.1f, PlayMode.LOOP);
    anim.play();
    anim.advance(0.15f); // 1.5 frames in @ 0.1s/frame -> index 1
    assertSame(frames[1], anim.currentFrame());
  }

  @Test
  void loopModeWrapsAround() {
    TextureRegion[] frames = fakeFrames(4);
    SpriteAnimation anim = new SpriteAnimation(frames, 0.1f, PlayMode.LOOP);
    anim.play();
    anim.advance(0.45f); // 4.5 frames in, 4 frames total -> wraps to index 0
    assertSame(frames[0], anim.currentFrame());
  }

  @Test
  void idleStopsAndRewindsToFirstFrame() {
    TextureRegion[] frames = fakeFrames(4);
    SpriteAnimation anim = new SpriteAnimation(frames, 0.1f, PlayMode.LOOP);
    anim.play();
    anim.advance(0.25f);
    anim.idle();

    assertFalse(anim.isPlaying());
    assertSame(frames[0], anim.currentFrame());

    // Further advances without a play() call must have no effect.
    anim.advance(1f);
    assertSame(frames[0], anim.currentFrame());
  }

  @Test
  void playResumesWithoutResettingElapsedTime() {
    TextureRegion[] frames = fakeFrames(4);
    SpriteAnimation anim = new SpriteAnimation(frames, 0.1f, PlayMode.LOOP);
    anim.play();
    anim.advance(0.15f); // -> index 1
    anim.play(); // resume, must not rewind
    assertTrue(anim.isPlaying());
    assertSame(frames[1], anim.currentFrame());
  }

  @Test
  void rejectsEmptyFrameArray() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new SpriteAnimation(new TextureRegion[0], 0.1f, PlayMode.LOOP));
  }
}
