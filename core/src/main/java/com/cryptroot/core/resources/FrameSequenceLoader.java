package com.cryptroot.core.resources;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.cryptroot.core.concurrent.TaskGate;
import com.cryptroot.core.concurrent.WorkerPool;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Loads a flipbook animation from a directory of individually-numbered frame images — {@code
 * <directory>/1.png}, {@code <directory>/2.png}, … — into a playback-ordered {@link TextureRegion}
 * array. The frame set is discovered from the directory contents rather than hard-coded, so adding
 * or removing frames on disk needs no code change.
 *
 * <h3>How the directory is listed</h3>
 *
 * <p>Frames are served from the classpath ({@code Gdx.files.classpath}), which cannot be enumerated
 * directly. When a build-time {@link ResourceManifest} is present (the normal case), the frame
 * files are read straight from it — any naming scheme works and the frames play in {@link
 * ResourceManifest#NATURAL_ORDER}, so contiguous numbering is no longer required. When no manifest
 * covers the directory (e.g. an IDE build that skipped the generator), the loader falls back to
 * probing {@code 1.png}, {@code 2.png}, … until the next file is absent; that legacy path still
 * requires frames numbered contiguously from {@code 1}.
 *
 * <h3>Asynchronous loading</h3>
 *
 * <p>Decoding a PNG into a {@link Pixmap} is CPU-bound work that is safe to run off the render
 * thread, but uploading that {@link Pixmap} into a GPU {@link Texture} must happen on the thread
 * that owns the GL context. {@link #loadAsync} exploits this split:
 *
 * <ol>
 *   <li>It forks the per-frame {@link Pixmap} decode across the shared {@link WorkerPool} and
 *       returns a {@link Pending} handle <em>immediately</em>, without blocking — the caller can
 *       kick a load off early (e.g. ahead of when the animation is first shown) and let the decode
 *       overlap with other work.
 *   <li>Later, on the render thread, {@link Pending#resolve()} uploads each decoded {@link Pixmap}
 *       into a cached {@link Texture} (via {@link ResourceManager}, which retains sole ownership)
 *       and returns the ready-to-draw frames.
 * </ol>
 *
 * <p>All {@link ResourceManager} access stays on the render thread: the worker threads only touch
 * {@link Gdx#files} and {@link Pixmap} (both safe to use concurrently for distinct files), never
 * the (non-thread-safe) texture cache.
 *
 * <h3>Caching</h3>
 *
 * <p>Each frame is cached under its own classpath ({@code <directory>/<n>.png}), so a directory
 * loaded a second time re-wraps the already-uploaded textures without re-decoding or re-uploading.
 * If the last frame is already cached the whole directory is assumed present and the decode is
 * skipped entirely. Every call still returns a fresh {@link TextureRegion} array (new region
 * objects over the shared cached {@link Texture}s), so each consumer may flip or otherwise mutate
 * its own regions independently.
 *
 * <h3>Peak memory</h3>
 *
 * <p>{@link #loadAsync} holds every decoded {@link Pixmap} in memory until {@link
 * Pending#resolve()} uploads and disposes them, so peak transient CPU memory is the sum of all
 * frames' pixels (comparable to loading one large sprite-sheet {@link Pixmap}). {@link
 * Pending#resolve()} <strong>must</strong> be called exactly once to release them.
 */
public final class FrameSequenceLoader {

  /**
   * Minimum frames decoded per {@link WorkerPool} chunk. Each PNG decode is heavy enough that small
   * chunks parallelize well; this only stops pointless over-splitting of tiny directories.
   */
  private static final int MIN_FRAMES_PER_CHUNK = 4;

  private FrameSequenceLoader() {}

  /**
   * Forks the decode of every frame in {@code directory} across {@code pool} and returns a handle
   * to complete the load later on the render thread. Does not block.
   *
   * @param resources the manager that will own the frame textures (accessed only by {@link
   *     Pending#resolve()}, on the render thread); also supplies the {@link ResourceManifest} used
   *     to list the directory's frames
   * @param pool the worker pool the per-frame {@link Pixmap} decode is forked onto
   * @param directory classpath directory holding the frame images, e.g. {@code
   *     "assets/sprites/Animation"}
   * @return a {@link Pending} whose {@link Pending#resolve()} yields the frames in playback order
   * @throws IllegalArgumentException if the directory is not listed in the manifest and contains no
   *     {@code 1.png} to probe
   */
  public static Pending loadAsync(ResourceManager resources, WorkerPool pool, String directory) {
    Objects.requireNonNull(resources, "resources must not be null");
    Objects.requireNonNull(pool, "pool must not be null");
    Objects.requireNonNull(directory, "directory must not be null");

    String[] frameKeys = resolveFrameKeys(resources, directory);
    int frameCount = frameKeys.length;

    // If the last frame is already cached, assume the whole directory is loaded (it is always
    // loaded as a unit) and skip the decode; resolve() will just re-wrap the cached textures.
    TaskGate<Pixmap[]> decodeGate =
        resources.hasCachedTexture(frameKeys[frameCount - 1])
            ? null
            : pool.mapChunks(
                0, frameCount, MIN_FRAMES_PER_CHUNK, (lo, hi) -> decodeRange(frameKeys, lo, hi));

    return new Pending(resources, frameKeys, decodeGate);
  }

  /**
   * Resolves the ordered classpath keys of a directory's frames, preferring the build-time {@link
   * ResourceManifest} and falling back to probing {@code 1.png}, {@code 2.png}, … when the manifest
   * does not cover the directory.
   */
  private static String[] resolveFrameKeys(ResourceManager resources, String directory) {
    List<String> listed = resources.manifest().list(directory);
    if (!listed.isEmpty()) {
      List<String> frames = new ArrayList<>(listed.size());
      for (String path : listed) {
        if (path.endsWith(".png")) {
          frames.add(path);
        }
      }
      if (!frames.isEmpty()) {
        return frames.toArray(new String[0]);
      }
    }
    int frameCount = probeFrameCount(directory);
    String[] frameKeys = new String[frameCount];
    for (int i = 0; i < frameCount; i++) {
      frameKeys[i] = framePath(directory, i + 1);
    }
    return frameKeys;
  }

  /**
   * Probes {@code directory/1.png}, {@code directory/2.png}, … and returns how many contiguous
   * frames exist. Used only as a fallback when no {@link ResourceManifest} lists the directory.
   */
  private static int probeFrameCount(String directory) {
    int count = 0;
    while (Gdx.files.classpath(framePath(directory, count + 1)).exists()) {
      count++;
    }
    if (count == 0) {
      throw new IllegalArgumentException(
          "No animation frames found: expected " + framePath(directory, 1) + " (1.png, 2.png, …)");
    }
    return count;
  }

  /** Decodes frames {@code [lo, hi)} into {@link Pixmap}s. Runs on a {@link WorkerPool} thread. */
  private static Pixmap[] decodeRange(String[] frameKeys, int lo, int hi) {
    Pixmap[] chunk = new Pixmap[hi - lo];
    for (int i = lo; i < hi; i++) {
      chunk[i - lo] = new Pixmap(Gdx.files.classpath(frameKeys[i]));
    }
    return chunk;
  }

  /** Concatenates the per-chunk decode results (already in range order) into one frame array. */
  private static Pixmap[] flatten(List<Pixmap[]> chunks, int frameCount) {
    Pixmap[] pixmaps = new Pixmap[frameCount];
    int next = 0;
    for (Pixmap[] chunk : chunks) {
      for (Pixmap pixmap : chunk) {
        pixmaps[next++] = pixmap;
      }
    }
    return pixmaps;
  }

  private static Texture textureFromPixmap(Pixmap pixmap) {
    Texture texture = new Texture(pixmap);
    texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
    return texture;
  }

  private static String framePath(String directory, int frameNumber) {
    return directory + "/" + frameNumber + ".png";
  }

  /**
   * A forked-but-not-yet-uploaded frame load returned by {@link #loadAsync}. Complete it with a
   * single {@link #resolve()} call on the render thread once the frames are needed.
   */
  public static final class Pending {

    private final ResourceManager resources;
    private final String[] frameKeys;
    private final TaskGate<Pixmap[]> decodeGate;
    private boolean resolved;

    private Pending(ResourceManager resources, String[] frameKeys, TaskGate<Pixmap[]> decodeGate) {
      this.resources = resources;
      this.frameKeys = frameKeys;
      this.decodeGate = decodeGate;
    }

    /** Number of frames in the sequence (known before {@link #resolve()}). */
    public int frameCount() {
      return frameKeys.length;
    }

    /**
     * Uploads the decoded frames into cached {@link Texture}s and returns them in playback order.
     * Blocks only if the background decode has not finished yet. Must be called exactly once, on
     * the render (GL) thread.
     *
     * @return a fresh {@link TextureRegion} array over the shared cached frame textures
     * @throws IllegalStateException if called more than once
     */
    public TextureRegion[] resolve() {
      if (resolved) {
        throw new IllegalStateException("resolve() already called");
      }
      resolved = true;

      // decodeGate == null means every frame was already cached: no pixmaps to upload or dispose.
      Pixmap[] pixmaps = decodeGate == null ? null : flatten(decodeGate.get(), frameKeys.length);
      TextureRegion[] frames = new TextureRegion[frameKeys.length];
      try {
        for (int i = 0; i < frameKeys.length; i++) {
          String key = frameKeys[i];
          Texture texture;
          if (pixmaps == null) {
            texture = resources.createTexture(key, TextureFilter.Linear, TextureFilter.Linear);
          } else {
            Pixmap pixmap = pixmaps[i];
            texture = resources.getOrCreateTexture(key, () -> textureFromPixmap(pixmap));
          }
          frames[i] = new TextureRegion(texture);
        }
      } finally {
        if (pixmaps != null) {
          for (Pixmap pixmap : pixmaps) {
            if (pixmap != null) {
              pixmap.dispose();
            }
          }
        }
      }
      return frames;
    }
  }
}
