package com.cryptroot.core.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Behavioural tests for {@link ResourceManifest}'s parsing, listing, and ordering. */
class ResourceManifestTest {

  private static ResourceManifest sample() {
    return ResourceManifest.of(
        List.of(
            "assets/sprites/Animation/2.png",
            "assets/sprites/Animation/10.png",
            "assets/sprites/Animation/1.png",
            "assets/sprites/DenRoom/1.png",
            "assets/sprites/DenRoom/3.png",
            "assets/sprites/DenRoom/2.png",
            "assets/ui/panel.png"));
  }

  @Test
  void listReturnsImmediateChildrenInNumericOrder() {
    assertEquals(
        List.of(
            "assets/sprites/Animation/1.png",
            "assets/sprites/Animation/2.png",
            "assets/sprites/Animation/10.png"),
        sample().list("assets/sprites/Animation"));
  }

  @Test
  void listToleratesTrailingSlashAndLeadingSlash() {
    ResourceManifest m = sample();
    assertEquals(m.list("assets/sprites/DenRoom"), m.list("assets/sprites/DenRoom/"));
    assertEquals(m.list("assets/sprites/DenRoom"), m.list("/assets/sprites/DenRoom"));
  }

  @Test
  void listOnlyReturnsDirectChildrenNotDescendants() {
    // "assets/sprites" has no direct file children — only sub-directories.
    assertTrue(sample().list("assets/sprites").isEmpty());
  }

  @Test
  void listOfUnknownDirectoryIsEmpty() {
    assertTrue(sample().list("assets/sprites/DoesNotExist").isEmpty());
  }

  @Test
  void containsNormalizesSeparatorsAndLeadingSlash() {
    ResourceManifest m = sample();
    assertTrue(m.contains("assets/ui/panel.png"));
    assertTrue(m.contains("/assets/ui/panel.png"));
    assertTrue(m.contains("assets\\ui\\panel.png"));
    assertFalse(m.contains("assets/ui/missing.png"));
  }

  @Test
  void ofSkipsBlankLinesAndCommentsAndDeduplicates() {
    ResourceManifest m =
        ResourceManifest.of(
            List.of(
                "# header comment",
                "  ",
                "assets/a/1.png",
                "assets/a/1.png",
                "  assets/a/2.png  "));
    assertEquals(List.of("assets/a/1.png", "assets/a/2.png"), m.list("assets/a"));
    assertEquals(2, m.size());
  }

  @Test
  void emptyManifestListsNothing() {
    ResourceManifest m = ResourceManifest.empty();
    assertTrue(m.isEmpty());
    assertEquals(0, m.size());
    assertTrue(m.list("assets/a").isEmpty());
    assertFalse(m.contains("assets/a/1.png"));
  }

  @Test
  void parseReadsNewlineSeparatedPaths() throws IOException {
    String body = "# manifest\nassets/x/1.png\nassets/x/10.png\nassets/x/2.png\n";
    ResourceManifest m =
        ResourceManifest.parse(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
    assertEquals(
        List.of("assets/x/1.png", "assets/x/2.png", "assets/x/10.png"), m.list("assets/x"));
  }

  @Test
  void loadOfAbsentResourceYieldsEmptyManifest() {
    assertTrue(ResourceManifest.load("assets/definitely-not-present-manifest.txt").isEmpty());
  }
}
