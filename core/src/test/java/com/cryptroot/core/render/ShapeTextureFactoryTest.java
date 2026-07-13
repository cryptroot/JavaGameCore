package com.cryptroot.core.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptroot.core.render.ShapeTextureFactory.ShapeMask;
import org.junit.jupiter.api.Test;

/**
 * GL-free tests for {@link ShapeTextureFactory}'s shape geometry ({@link ShapeMask}). The Pixmap /
 * Texture rasterisation itself needs a GL context and is left uncovered, matching the rest of the
 * render layer.
 */
class ShapeTextureFactoryTest {

  @Test
  void ringMaskFormsAnnulusWithHollowCentre() {
    // diameter 10 -> outer radius 5; thickness 2 -> inner radius 3.
    ShapeMask ring = ShapeTextureFactory.ringMask(10, 2f);
    assertFalse(ring.covers(5, 5, 10, 10), "centre is hollow");
    assertTrue(ring.covers(0, 5, 10, 10), "left edge sits on the stroke");
    assertTrue(ring.covers(5, 0, 10, 10), "bottom edge sits on the stroke");
    assertFalse(ring.covers(0, 0, 10, 10), "corner is outside the outer radius");
  }

  @Test
  void ringMaskThickerThanRadiusFillsToCentre() {
    // inner radius clamps to 0, so the ring degenerates to a filled disc.
    ShapeMask ring = ShapeTextureFactory.ringMask(10, 100f);
    assertTrue(ring.covers(5, 5, 10, 10));
  }

  @Test
  void filledCircleMaskCoversCentreNotCorner() {
    ShapeMask disc = ShapeTextureFactory.filledCircleMask(10);
    assertTrue(disc.covers(5, 5, 10, 10));
    assertTrue(disc.covers(0, 5, 10, 10));
    assertFalse(disc.covers(0, 0, 10, 10));
  }

  @Test
  void maskFactoriesRejectNonPositiveArgs() {
    assertThrows(IllegalArgumentException.class, () -> ShapeTextureFactory.ringMask(0, 2f));
    assertThrows(IllegalArgumentException.class, () -> ShapeTextureFactory.ringMask(10, 0f));
    assertThrows(IllegalArgumentException.class, () -> ShapeTextureFactory.filledCircleMask(0));
  }
}
