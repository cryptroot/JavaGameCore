package com.cryptroot.core.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests for {@link ResourceManifestGenerator}'s directory walking and manifest writing. */
class ResourceManifestGeneratorTest {

  private static void touch(Path file) throws IOException {
    Files.createDirectories(file.getParent());
    Files.writeString(file, "x");
  }

  @Test
  void scanReturnsRelativePathsInNaturalOrder(@TempDir Path root) throws IOException {
    Path assets = root.resolve("assets");
    touch(assets.resolve("sprites/Animation/1.png"));
    touch(assets.resolve("sprites/Animation/10.png"));
    touch(assets.resolve("sprites/Animation/2.png"));
    touch(assets.resolve("ui/panel.png"));

    List<String> entries = ResourceManifestGenerator.scan(root, assets, null);

    assertEquals(
        List.of(
            "assets/sprites/Animation/1.png",
            "assets/sprites/Animation/2.png",
            "assets/sprites/Animation/10.png",
            "assets/ui/panel.png"),
        entries);
  }

  @Test
  void scanExcludesTheManifestFileItself(@TempDir Path root) throws IOException {
    Path assets = root.resolve("assets");
    touch(assets.resolve("sprites/1.png"));
    Path manifest = assets.resolve("manifest.txt");
    touch(manifest); // a stale manifest from a previous build

    List<String> entries = ResourceManifestGenerator.scan(root, assets, manifest);

    assertEquals(List.of("assets/sprites/1.png"), entries);
  }

  @Test
  void generateWritesAHeaderedManifestThatReadsBack(@TempDir Path root) throws IOException {
    Path assets = root.resolve("assets");
    touch(assets.resolve("sprites/DenRoom/1.png"));
    touch(assets.resolve("sprites/DenRoom/2.png"));

    ResourceManifestGenerator.generate(root, "assets", "assets/manifest.txt");

    Path manifest = assets.resolve("manifest.txt");
    assertTrue(Files.exists(manifest));
    List<String> lines = Files.readAllLines(manifest, StandardCharsets.UTF_8);
    assertTrue(lines.get(0).startsWith("#"), "first line should be the generated header comment");
    assertEquals(
        List.of("assets/sprites/DenRoom/1.png", "assets/sprites/DenRoom/2.png"),
        lines.subList(1, lines.size()));

    // The written manifest round-trips through the runtime reader.
    ResourceManifest parsed = ResourceManifest.parse(Files.newInputStream(manifest));
    assertEquals(
        List.of("assets/sprites/DenRoom/1.png", "assets/sprites/DenRoom/2.png"),
        parsed.list("assets/sprites/DenRoom"));
    assertFalse(parsed.contains("assets/manifest.txt"), "manifest must not list itself");
  }

  @Test
  void generateNoOpsWhenSourceDirectoryIsMissing(@TempDir Path root) throws IOException {
    ResourceManifestGenerator.generate(root, "assets", "assets/manifest.txt");
    assertFalse(Files.exists(root.resolve("assets/manifest.txt")));
  }
}
