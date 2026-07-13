package com.cryptroot.core.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class AudioManagerTest {

  /** No-op {@link Sound} fake — never touches real audio, just tracks disposal. */
  private static final class FakeSound implements Sound {
    boolean disposed;

    @Override
    public long play() {
      return 0;
    }

    @Override
    public long play(float volume) {
      return 0;
    }

    @Override
    public long play(float volume, float pitch, float pan) {
      return 0;
    }

    @Override
    public long loop() {
      return 0;
    }

    @Override
    public long loop(float volume) {
      return 0;
    }

    @Override
    public long loop(float volume, float pitch, float pan) {
      return 0;
    }

    @Override
    public void stop() {}

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void dispose() {
      disposed = true;
    }

    @Override
    public void stop(long soundId) {}

    @Override
    public void pause(long soundId) {}

    @Override
    public void resume(long soundId) {}

    @Override
    public void setLooping(long soundId, boolean looping) {}

    @Override
    public void setPitch(long soundId, float pitch) {}

    @Override
    public void setVolume(long soundId, float volume) {}

    @Override
    public void setPan(long soundId, float pan, float volume) {}
  }

  /** No-op {@link Music} fake — tracks volume and disposal without touching real audio. */
  private static final class FakeMusic implements Music {
    boolean disposed;
    float volume;

    @Override
    public void play() {}

    @Override
    public void pause() {}

    @Override
    public void stop() {}

    @Override
    public boolean isPlaying() {
      return false;
    }

    @Override
    public void setLooping(boolean isLooping) {}

    @Override
    public boolean isLooping() {
      return false;
    }

    @Override
    public void setVolume(float volume) {
      this.volume = volume;
    }

    @Override
    public float getVolume() {
      return volume;
    }

    @Override
    public void setPan(float pan, float volume) {}

    @Override
    public void setPosition(float position) {}

    @Override
    public float getPosition() {
      return 0;
    }

    @Override
    public void dispose() {
      disposed = true;
    }

    @Override
    public void setOnCompletionListener(OnCompletionListener listener) {}
  }

  // ---- clampVolume / combinedVolume (pure) -------------------------------

  @Test
  void clampVolumeClampsToUnitRange() {
    assertEquals(0f, AudioManager.clampVolume(-1f));
    assertEquals(1f, AudioManager.clampVolume(2f));
    assertEquals(0.5f, AudioManager.clampVolume(0.5f));
  }

  @Test
  void combinedVolumeMultipliesClampedChannels() {
    assertEquals(0.25f, AudioManager.combinedVolume(0.5f, 0.5f), 1e-6f);
    assertEquals(
        1f, AudioManager.combinedVolume(2f, 2f), 1e-6f, "each channel clamped before multiplying");
    assertEquals(0f, AudioManager.combinedVolume(-1f, 1f));
  }

  // ---- getOrCreate caching ------------------------------------------------

  @Test
  void getOrCreateSoundCachesByKey() {
    AudioManager audio = new AudioManager();
    AtomicInteger creations = new AtomicInteger();
    FakeSound first = new FakeSound();

    Sound a =
        audio.getOrCreateSound(
            "sfx/shoot.wav", () -> creations.getAndIncrement() == 0 ? first : new FakeSound());
    Sound b = audio.getOrCreateSound("sfx/shoot.wav", FakeSound::new);

    assertSame(first, a);
    assertSame(a, b);
    assertEquals(1, creations.get());
  }

  @Test
  void getOrCreateMusicCachesByKey() {
    AudioManager audio = new AudioManager();
    FakeMusic first = new FakeMusic();

    Music a = audio.getOrCreateMusic("music/theme.ogg", () -> first);
    Music b = audio.getOrCreateMusic("music/theme.ogg", FakeMusic::new);

    assertSame(first, a);
    assertSame(a, b);
  }

  @Test
  void getOrCreateRejectsNullArguments() {
    AudioManager audio = new AudioManager();
    assertThrows(NullPointerException.class, () -> audio.getOrCreateSound(null, FakeSound::new));
    assertThrows(NullPointerException.class, () -> audio.getOrCreateSound("k", null));
  }

  // ---- volume setters (fail-soft clamping) -------------------------------

  @Test
  void volumeSettersClampOutOfRangeValues() {
    AudioManager audio = new AudioManager();
    audio.setMasterVolume(5f);
    audio.setSfxVolume(-5f);
    audio.setMusicVolume(0.3f);

    assertEquals(1f, audio.masterVolume());
    assertEquals(0f, audio.sfxVolume());
    assertEquals(0.3f, audio.musicVolume(), 1e-6f);
  }

  // ---- dispose ------------------------------------------------------------

  @Test
  void disposeDisposesEveryCachedSoundAndMusic() {
    AudioManager audio = new AudioManager();
    FakeSound sound = new FakeSound();
    FakeMusic music = new FakeMusic();
    audio.getOrCreateSound("a", () -> sound);
    audio.getOrCreateMusic("b", () -> music);

    audio.dispose();

    assertTrue(sound.disposed);
    assertTrue(music.disposed);
  }
}
