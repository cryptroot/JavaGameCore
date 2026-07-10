package com.cryptroot.tiled.io;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Resolves classpath resource paths that are referenced <em>relative to another file</em>, such as
 * a TSX tileset referenced by a TMX map, or a PNG image referenced by a tileset.
 *
 * <p>Paths use {@code /} separators (classpath convention) and are normalised so that {@code .} and
 * {@code ..} segments are collapsed.
 */
public final class ResourceLocator {

  private ResourceLocator() {}

  /**
   * Resolves {@code relative} against the directory containing {@code baseFile}.
   *
   * <pre>{@code
   * resolve("assets/test/Cave.tmx", "Cave_Tilemap.tsx")   // "assets/test/Cave_Tilemap.tsx"
   * resolve("assets/test/Cave.tmx", "../img/floor.png")   // "assets/img/floor.png"
   * }</pre>
   *
   * @param baseFile the classpath of the referencing file (e.g. the map or tileset)
   * @param relative the path referenced from within {@code baseFile}
   * @return the normalised classpath of the referenced resource
   */
  public static String resolve(String baseFile, String relative) {
    if (relative == null) {
      throw new IllegalArgumentException("relative path must not be null");
    }
    String base = baseFile == null ? "" : baseFile;
    int slash = base.lastIndexOf('/');
    String dir = slash >= 0 ? base.substring(0, slash) : "";
    String combined = dir.isEmpty() ? relative : dir + "/" + relative;
    return normalise(combined);
  }

  private static String normalise(String path) {
    Deque<String> stack = new ArrayDeque<>();
    for (String part : path.split("/")) {
      if (part.isEmpty() || part.equals(".")) {
        continue;
      }
      if (part.equals("..")) {
        if (!stack.isEmpty() && !stack.peekLast().equals("..")) {
          stack.removeLast();
        } else {
          stack.addLast(part);
        }
      } else {
        stack.addLast(part);
      }
    }
    return String.join("/", stack);
  }
}
