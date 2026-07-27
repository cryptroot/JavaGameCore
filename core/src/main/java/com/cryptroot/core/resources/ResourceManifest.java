package com.cryptroot.core.resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * An in-memory index of classpath resource paths that gives libGDX/Java the one thing the classpath
 * cannot provide on its own: the ability to <em>list a directory</em>.
 *
 * <h3>Why this exists</h3>
 *
 * <p>Resources served from the classpath ({@code Gdx.files.classpath} / {@link
 * ClassLoader#getResourceAsStream}) cannot be enumerated — there is no portable way to ask "what
 * files live under this folder?" once the assets are packed into a jar. Framework code has
 * historically worked around this by probing predictably-named files ({@code 1.png}, {@code 2.png},
 * … until the next is absent), which forces contiguous numbering and one file lookup per frame.
 *
 * <p>{@link ResourceManifest} replaces that guesswork with a manifest generated at build time by
 * {@link ResourceManifestGenerator}: a flat, newline-separated listing of every packaged resource
 * path. Loaded once at runtime, it answers {@link #list(String)} and {@link #contains(String)} in
 * memory — effectively "resource reflection" over the classpath.
 *
 * <h3>Manifest format</h3>
 *
 * <p>UTF-8 text, one classpath-relative resource path per line (e.g. {@code
 * assets/sprites/Animation/1.png}). Blank lines and lines beginning with {@code #} (comments) are
 * ignored, so the generator may write a header. Paths are stored normalized (forward slashes, no
 * leading slash) and sorted with {@link #NATURAL_ORDER} so numbered siblings read {@code 1, 2, …,
 * 10, …, 100} rather than lexicographically.
 *
 * <h3>Scope</h3>
 *
 * <p>A manifest indexes whatever the build pointed the generator at — typically the game module's
 * own asset tree. It need not cover every resource on the runtime classpath; directories it does
 * not list simply return {@link #list(String) an empty list}, letting callers fall back to probing.
 * All queries are pure and side-effect-free; the class is immutable and thread-safe after loading.
 */
public final class ResourceManifest {

  /** Default classpath location the build writes to and {@link ResourceManager} reads from. */
  public static final String DEFAULT_MANIFEST_PATH = "assets/manifest.txt";

  /**
   * Orders paths so that embedded runs of digits compare numerically rather than lexicographically
   * ({@code 2.png} before {@code 10.png}). Shared with {@link ResourceManifestGenerator} so the
   * file on disk and the in-memory listing agree.
   */
  public static final Comparator<String> NATURAL_ORDER = ResourceManifest::compareNatural;

  /** All indexed resource paths, normalized and {@link #NATURAL_ORDER}-sorted. Immutable. */
  private final List<String> paths;

  /** Same paths as a set, for O(1) {@link #contains(String)}. */
  private final Set<String> pathSet;

  /** Directory (path up to the last {@code /}) → its immediate file children, in sorted order. */
  private final Map<String, List<String>> byDirectory;

  private ResourceManifest(List<String> sortedNormalizedPaths) {
    this.paths = List.copyOf(sortedNormalizedPaths);
    this.pathSet = new HashSet<>(sortedNormalizedPaths);
    this.byDirectory = indexByDirectory(sortedNormalizedPaths);
  }

  // -------------------------------------------------------------------------
  // Factories
  // -------------------------------------------------------------------------

  /** Returns a manifest that lists nothing (every {@link #list(String)} returns empty). */
  public static ResourceManifest empty() {
    return new ResourceManifest(List.of());
  }

  /**
   * Builds a manifest from raw path lines. Each line is normalized (trimmed, back-slashes turned to
   * forward slashes, any leading slash stripped); blank lines and {@code #} comments are skipped.
   * Duplicate paths collapse to one. The result is sorted with {@link #NATURAL_ORDER}.
   *
   * @param resourcePaths raw path lines (e.g. the lines of a manifest file)
   * @return an immutable manifest over the distinct, sorted paths
   */
  public static ResourceManifest of(List<String> resourcePaths) {
    Objects.requireNonNull(resourcePaths, "resourcePaths must not be null");
    LinkedHashSet<String> unique = new LinkedHashSet<>();
    for (String raw : resourcePaths) {
      String normalized = normalizePath(raw);
      if (normalized != null) {
        unique.add(normalized);
      }
    }
    List<String> sorted = new ArrayList<>(unique);
    sorted.sort(NATURAL_ORDER);
    return new ResourceManifest(sorted);
  }

  /**
   * Reads and parses a manifest from an open stream. The caller retains ownership of {@code in} —
   * this method reads it fully but does not close the underlying resource beyond the buffered
   * reader it wraps (which does close {@code in}).
   *
   * @param in UTF-8 manifest content
   * @return the parsed manifest
   * @throws IOException if the stream cannot be read
   */
  public static ResourceManifest parse(InputStream in) throws IOException {
    Objects.requireNonNull(in, "in must not be null");
    List<String> lines = new ArrayList<>();
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        lines.add(line);
      }
    }
    return of(lines);
  }

  /**
   * Loads the manifest resource at {@code classpathResource} from the classpath.
   *
   * <p>A <strong>missing</strong> manifest is treated as {@link #empty()} rather than an error — a
   * module that never ran the generator (or an IDE build that skips Maven plugins) simply gets an
   * empty index and callers fall back to probing. A manifest that exists but cannot be read is a
   * real fault and throws.
   *
   * @param classpathResource resource path, e.g. {@link #DEFAULT_MANIFEST_PATH}
   * @return the parsed manifest, or {@link #empty()} if no such resource exists
   * @throws IllegalStateException if the resource exists but cannot be read
   */
  public static ResourceManifest load(String classpathResource) {
    Objects.requireNonNull(classpathResource, "classpathResource must not be null");
    ClassLoader loader = ResourceManifest.class.getClassLoader();
    try (InputStream in = loader.getResourceAsStream(classpathResource)) {
      return in == null ? empty() : parse(in);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read resource manifest: " + classpathResource, e);
    }
  }

  // -------------------------------------------------------------------------
  // Queries
  // -------------------------------------------------------------------------

  /** Returns {@code true} if this manifest indexes no paths. */
  public boolean isEmpty() {
    return paths.isEmpty();
  }

  /** Number of resource paths indexed. */
  public int size() {
    return paths.size();
  }

  /**
   * Returns {@code true} if {@code resourcePath} is one of the indexed resources. The argument is
   * normalized the same way manifest lines are, so a leading slash or back-slashes are tolerated.
   */
  public boolean contains(String resourcePath) {
    Objects.requireNonNull(resourcePath, "resourcePath must not be null");
    String normalized = normalizePath(resourcePath);
    return normalized != null && pathSet.contains(normalized);
  }

  /**
   * Lists the immediate <em>file</em> children of {@code directory} (paths one segment deeper whose
   * parent is exactly {@code directory}), in {@link #NATURAL_ORDER}. Nested sub-directories are not
   * flattened in — only direct children appear.
   *
   * <p>Returns an empty list if the directory is unknown to this manifest, so a caller can treat
   * "not indexed" and "indexed but empty" alike and fall back to another discovery strategy.
   *
   * @param directory directory path, with or without a trailing slash, e.g. {@code
   *     "assets/sprites/Animation"}
   * @return an unmodifiable, naturally-ordered list of full child resource paths
   */
  public List<String> list(String directory) {
    Objects.requireNonNull(directory, "directory must not be null");
    String dir = normalizeDirectory(directory);
    List<String> children = byDirectory.get(dir);
    return children == null ? List.of() : Collections.unmodifiableList(children);
  }

  /** Returns every indexed resource path, naturally ordered and unmodifiable. */
  public List<String> paths() {
    return paths;
  }

  // -------------------------------------------------------------------------
  // Internals
  // -------------------------------------------------------------------------

  private static Map<String, List<String>> indexByDirectory(List<String> sortedPaths) {
    // sortedPaths is already NATURAL_ORDER-sorted, so each directory's list comes out ordered.
    Map<String, List<String>> index = new LinkedHashMap<>();
    for (String path : sortedPaths) {
      int slash = path.lastIndexOf('/');
      String dir = slash < 0 ? "" : path.substring(0, slash);
      index.computeIfAbsent(dir, d -> new ArrayList<>()).add(path);
    }
    return index;
  }

  /**
   * Normalizes a raw manifest line into a canonical resource path, or {@code null} for a line that
   * carries no path (blank or a {@code #} comment).
   */
  private static String normalizePath(String raw) {
    if (raw == null) {
      return null;
    }
    String s = raw.trim().replace('\\', '/');
    if (s.isEmpty() || s.charAt(0) == '#') {
      return null;
    }
    int start = 0;
    while (start < s.length() && s.charAt(start) == '/') {
      start++;
    }
    s = s.substring(start);
    return s.isEmpty() ? null : s;
  }

  /** Canonicalizes a directory argument: forward slashes, no leading or trailing slash. */
  private static String normalizeDirectory(String directory) {
    String s = directory.trim().replace('\\', '/');
    int start = 0;
    while (start < s.length() && s.charAt(start) == '/') {
      start++;
    }
    int end = s.length();
    while (end > start && s.charAt(end - 1) == '/') {
      end--;
    }
    return s.substring(start, end);
  }

  /**
   * Compares two strings so that maximal runs of ASCII digits are ordered by numeric value while
   * everything else is ordered by char value. Leading zeros do not change the numeric value but
   * break ties (fewer digits first) to keep the order total and stable.
   */
  private static int compareNatural(String a, String b) {
    int ia = 0;
    int ib = 0;
    int la = a.length();
    int lb = b.length();
    while (ia < la && ib < lb) {
      char ca = a.charAt(ia);
      char cb = b.charAt(ib);
      boolean da = ca >= '0' && ca <= '9';
      boolean db = cb >= '0' && cb <= '9';
      if (da && db) {
        int enda = ia;
        while (enda < la && a.charAt(enda) >= '0' && a.charAt(enda) <= '9') {
          enda++;
        }
        int endb = ib;
        while (endb < lb && b.charAt(endb) >= '0' && b.charAt(endb) <= '9') {
          endb++;
        }
        int sa = ia;
        while (sa < enda - 1 && a.charAt(sa) == '0') {
          sa++;
        }
        int sb = ib;
        while (sb < endb - 1 && b.charAt(sb) == '0') {
          sb++;
        }
        int lenA = enda - sa;
        int lenB = endb - sb;
        if (lenA != lenB) {
          return lenA - lenB;
        }
        for (int k = 0; k < lenA; k++) {
          char x = a.charAt(sa + k);
          char y = b.charAt(sb + k);
          if (x != y) {
            return x - y;
          }
        }
        if ((enda - ia) != (endb - ib)) {
          return (enda - ia) - (endb - ib);
        }
        ia = enda;
        ib = endb;
      } else if (ca != cb) {
        return ca - cb;
      } else {
        ia++;
        ib++;
      }
    }
    return (la - ia) - (lb - ib);
  }
}
