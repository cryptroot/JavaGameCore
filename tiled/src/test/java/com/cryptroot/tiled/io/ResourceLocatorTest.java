package com.cryptroot.tiled.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Path-resolution tests for {@link ResourceLocator}. */
class ResourceLocatorTest {

  @Test
  void resolvesSiblingFile() {
    assertEquals(
        "assets/test/Cave_Tilemap.tsx",
        ResourceLocator.resolve("assets/test/Cave.tmx", "Cave_Tilemap.tsx"));
  }

  @Test
  void resolvesParentTraversal() {
    assertEquals(
        "assets/img/floor.png",
        ResourceLocator.resolve("assets/test/Cave.tmx", "../img/floor.png"));
  }

  @Test
  void collapsesCurrentDirectorySegments() {
    assertEquals(
        "assets/test/floor.png", ResourceLocator.resolve("assets/test/Cave.tmx", "./floor.png"));
  }

  @Test
  void handlesBaseWithoutDirectory() {
    assertEquals("floor.png", ResourceLocator.resolve("Cave.tmx", "floor.png"));
  }

  @Test
  void handlesMultipleParentTraversals() {
    assertEquals("floor.png", ResourceLocator.resolve("assets/test/Cave.tmx", "../../floor.png"));
  }
}
