package com.cryptroot.core.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Centralised {@link Sound}/{@link Music} cache with master/sfx/music volume control — the {@code
 * core.audio} counterpart of {@link com.cryptroot.core.resources.ResourceManager}, mirroring its
 * {@code getOrCreate}-keyed-by-classpath cache/dispose pattern.
 *
 * <h3>Loading &amp; playing</h3>
 *
 * <p>{@link #loadSound(String)}/{@link #loadMusic(String)} resolve and cache by classpath, exactly
 * like {@link com.cryptroot.core.resources.ResourceManager#createTexture}; {@link
 * #playSound(String)} plays a cached effect once at the current combined SFX volume, and {@link
 * #playMusic(String)} stops whatever is currently looping and starts the new track at the current
 * combined music volume. Use {@link #getOrCreateSound}/{@link #getOrCreateMusic} directly to inject
 * a fake in a test, the same seam {@code ResourceManager.getOrCreateTexture} provides for textures.
 *
 * <h3>Volume</h3>
 *
 * <p>{@link #setMasterVolume}/{@link #setSfxVolume}/{@link #setMusicVolume} are fail-soft and
 * documented as such: an out-of-range value is silently clamped to {@code [0,1]} (see {@link
 * #clampVolume}) rather than rejected, since a UI volume slider or a "loud" content preset
 * routinely produces values slightly outside the unit range. The volume actually applied to a
 * channel is {@link #combinedVolume(float, float)} — master × channel, each independently clamped
 * first.
 *
 * <h3>Testing</h3>
 *
 * <p>{@link #clampVolume} and {@link #combinedVolume(float, float)} are pure static methods, unit
 * tested without touching real audio. Actual {@link Sound}/{@link Music} loading (via {@link
 * #loadSound}/{@link #loadMusic}) is as untestable outside a running LibGDX application as {@link
 * com.cryptroot.core.resources.ResourceManager}'s {@link com.badlogic.gdx.graphics.Texture}
 * loading, and is deliberately left untested for the same reason.
 */
public final class AudioManager implements Disposable {

  private final Map<String, Sound> soundCache = new HashMap<>();
  private final Map<String, Music> musicCache = new HashMap<>();

  private float masterVolume = 1f;
  private float sfxVolume = 1f;
  private float musicVolume = 1f;
  private Music currentMusic;

  // -------------------------------------------------------------------------
  // Sound effects
  // -------------------------------------------------------------------------

  /**
   * Returns the {@link Sound} cached under {@code key}, creating it via {@code factory} on first
   * request. Every call with the same key returns the same instance — the injectable seam for tests
   * (and for callers that need a non-classpath source).
   */
  public Sound getOrCreateSound(String key, Supplier<Sound> factory) {
    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(factory, "factory must not be null");
    return soundCache.computeIfAbsent(key, k -> factory.get());
  }

  /** Loads (or returns the cached) {@link Sound} for {@code classpath}. */
  public Sound loadSound(String classpath) {
    Objects.requireNonNull(classpath, "classpath must not be null");
    return getOrCreateSound(classpath, () -> Gdx.audio.newSound(Gdx.files.classpath(classpath)));
  }

  /**
   * Plays the (cached-or-loaded) sound at {@code classpath} once, at the current combined SFX
   * volume.
   *
   * @return the playing sound instance's id, as returned by {@link Sound#play(float)}
   */
  public long playSound(String classpath) {
    return loadSound(classpath).play(combinedVolume(masterVolume, sfxVolume));
  }

  // -------------------------------------------------------------------------
  // Music
  // -------------------------------------------------------------------------

  /**
   * Returns the {@link Music} cached under {@code key}, creating it via {@code factory} on first
   * request. Every call with the same key returns the same instance.
   */
  public Music getOrCreateMusic(String key, Supplier<Music> factory) {
    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(factory, "factory must not be null");
    return musicCache.computeIfAbsent(key, k -> factory.get());
  }

  /** Loads (or returns the cached) {@link Music} for {@code classpath}. */
  public Music loadMusic(String classpath) {
    Objects.requireNonNull(classpath, "classpath must not be null");
    return getOrCreateMusic(classpath, () -> Gdx.audio.newMusic(Gdx.files.classpath(classpath)));
  }

  /**
   * Stops whatever music is currently playing (if any), then loops the (cached-or-loaded) track at
   * {@code classpath} at the current combined music volume.
   */
  public void playMusic(String classpath) {
    Objects.requireNonNull(classpath, "classpath must not be null");
    if (currentMusic != null) currentMusic.stop();
    currentMusic = loadMusic(classpath);
    currentMusic.setLooping(true);
    currentMusic.setVolume(combinedVolume(masterVolume, musicVolume));
    currentMusic.play();
  }

  /** Stops the currently playing music, if any. A no-op if nothing is playing. */
  public void stopMusic() {
    if (currentMusic != null) currentMusic.stop();
  }

  // -------------------------------------------------------------------------
  // Volume
  // -------------------------------------------------------------------------

  /** Master volume multiplier, applied on top of both SFX and music channels. */
  public void setMasterVolume(float volume) {
    masterVolume = clampVolume(volume);
    refreshMusicVolume();
  }

  public float masterVolume() {
    return masterVolume;
  }

  /** Sound-effects channel volume, combined with {@link #masterVolume()} at play time. */
  public void setSfxVolume(float volume) {
    sfxVolume = clampVolume(volume);
  }

  public float sfxVolume() {
    return sfxVolume;
  }

  /** Music channel volume; changes apply immediately to any currently playing track. */
  public void setMusicVolume(float volume) {
    musicVolume = clampVolume(volume);
    refreshMusicVolume();
  }

  public float musicVolume() {
    return musicVolume;
  }

  private void refreshMusicVolume() {
    if (currentMusic != null) currentMusic.setVolume(combinedVolume(masterVolume, musicVolume));
  }

  /** Pure: clamps a volume to {@code [0, 1]}. */
  public static float clampVolume(float volume) {
    return MathUtils.clamp(volume, 0f, 1f);
  }

  /**
   * Pure: the effective volume for a channel — {@code masterVolume × channelVolume}, each clamped
   * to {@code [0, 1]} first.
   */
  public static float combinedVolume(float masterVolume, float channelVolume) {
    return clampVolume(masterVolume) * clampVolume(channelVolume);
  }

  // -------------------------------------------------------------------------
  // Disposable
  // -------------------------------------------------------------------------

  @Override
  public void dispose() {
    soundCache.values().forEach(Sound::dispose);
    musicCache.values().forEach(Music::dispose);
    soundCache.clear();
    musicCache.clear();
    currentMusic = null;
  }
}
