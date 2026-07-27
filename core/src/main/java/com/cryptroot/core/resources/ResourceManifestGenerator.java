package com.cryptroot.core.resources;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Build-time tool that walks a directory of packaged resources and writes the flat manifest that
 * {@link ResourceManifest} ingests at runtime, giving the classpath the directory-listing ability
 * it otherwise lacks (see {@link ResourceManifest} for the "why").
 *
 * <h3>How it is run</h3>
 *
 * <p>It has no libGDX or GL dependency — only {@code java.nio.file} — so it runs as a plain {@code
 * main} during the build (wired via {@code exec-maven-plugin} in the reactor's parent POM, bound to
 * {@code process-classes} so the manifest lands in {@code target/classes} and is packaged like any
 * other resource). The scanned folder and output path are POM-configurable.
 *
 * <pre>{@code
 * ResourceManifestGenerator <classpathRoot> <sourceDir> <outputPath>
 *   classpathRoot : the build output root whose layout equals the runtime classpath
 *                   (e.g. target/classes); emitted paths are relative to it
 *   sourceDir     : sub-directory of classpathRoot to index, or empty to index all of it
 *                   (e.g. "assets")
 *   outputPath    : manifest file to write, relative to classpathRoot
 *                   (e.g. "assets/manifest.txt")
 * }</pre>
 *
 * <h3>Guarantees</h3>
 *
 * <ul>
 *   <li>Only regular files are listed; directories themselves are implied by their children.
 *   <li>The output file is excluded from its own scan, so re-running over an existing manifest (an
 *       incremental build with no {@code clean}) never indexes the stale manifest.
 *   <li>Paths are emitted relative to {@code classpathRoot} with {@code /} separators and sorted
 *       with {@link ResourceManifest#NATURAL_ORDER}, matching the runtime reader.
 *   <li>A missing {@code sourceDir} is a no-op (logged, exit 0) — a module with nothing to index
 *       does not fail the build.
 * </ul>
 */
public final class ResourceManifestGenerator {

  private static final String LOG_PREFIX = "[resource-manifest] ";

  private ResourceManifestGenerator() {}

  public static void main(String[] args) throws IOException {
    if (args.length < 3) {
      System.err.println(
          LOG_PREFIX + "usage: ResourceManifestGenerator <classpathRoot> <sourceDir> <outputPath>");
      return;
    }
    generate(Paths.get(args[0]), args[1], args[2]);
  }

  /**
   * Scans {@code sourceDir} under {@code classpathRoot} and writes the manifest to {@code
   * outputRelativePath} (also under {@code classpathRoot}). No-ops if the source directory does not
   * exist.
   *
   * @param classpathRoot build output root; emitted paths are relative to it
   * @param sourceDir sub-directory to index, or {@code null}/blank to index the whole root
   * @param outputRelativePath manifest file path relative to {@code classpathRoot}
   * @throws IOException if scanning or writing fails
   */
  public static void generate(Path classpathRoot, String sourceDir, String outputRelativePath)
      throws IOException {
    Objects.requireNonNull(classpathRoot, "classpathRoot must not be null");
    Objects.requireNonNull(outputRelativePath, "outputRelativePath must not be null");

    Path scanRoot =
        sourceDir == null || sourceDir.isBlank() ? classpathRoot : classpathRoot.resolve(sourceDir);
    Path output = classpathRoot.resolve(outputRelativePath);

    if (!Files.isDirectory(scanRoot)) {
      System.out.println(LOG_PREFIX + "no resources to index at " + scanRoot + " (skipping)");
      return;
    }

    List<String> entries = scan(classpathRoot, scanRoot, output);
    write(output, entries);
    System.out.println(LOG_PREFIX + "indexed " + entries.size() + " resource(s) into " + output);
  }

  /**
   * Returns every regular file under {@code scanRoot}, as paths relative to {@code classpathRoot}
   * (with {@code /} separators), sorted with {@link ResourceManifest#NATURAL_ORDER}. The file at
   * {@code excludeFile}, if any, is omitted so a manifest never lists itself.
   *
   * @param classpathRoot root the returned paths are relative to
   * @param scanRoot directory tree to walk (must exist)
   * @param excludeFile a file to omit (typically the manifest being written), or {@code null}
   * @return the naturally-ordered relative resource paths
   * @throws IOException if the tree cannot be walked
   */
  public static List<String> scan(Path classpathRoot, Path scanRoot, Path excludeFile)
      throws IOException {
    Objects.requireNonNull(classpathRoot, "classpathRoot must not be null");
    Objects.requireNonNull(scanRoot, "scanRoot must not be null");
    Path exclude = excludeFile == null ? null : excludeFile.toAbsolutePath().normalize();
    Path root = classpathRoot.toAbsolutePath().normalize();

    List<String> entries = new ArrayList<>();
    try (Stream<Path> walk = Files.walk(scanRoot)) {
      walk.filter(Files::isRegularFile)
          .forEach(
              file -> {
                Path absolute = file.toAbsolutePath().normalize();
                if (absolute.equals(exclude)) {
                  return;
                }
                entries.add(root.relativize(absolute).toString().replace(File.separatorChar, '/'));
              });
    }
    entries.sort(ResourceManifest.NATURAL_ORDER);
    return entries;
  }

  /**
   * Writes {@code entries} to {@code output} (creating parent directories), one path per line under
   * a single generated header comment.
   */
  public static void write(Path output, List<String> entries) throws IOException {
    Objects.requireNonNull(output, "output must not be null");
    Objects.requireNonNull(entries, "entries must not be null");
    Path parent = output.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    List<String> lines = new ArrayList<>(entries.size() + 1);
    lines.add(
        "# Generated by ResourceManifestGenerator - do not edit. One resource path per line.");
    lines.addAll(entries);
    Files.write(output, lines, StandardCharsets.UTF_8);
  }
}
