package com.cryptroot.core.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Rectangle;
import com.cryptroot.core.world.component.DefaultPositionComponent;
import org.junit.jupiter.api.Test;

class BoxColliderTest {

  @Test
  void boundsTrackAnchorPositionWithOffset() {
    DefaultPositionComponent anchor = new DefaultPositionComponent(10f, 20f);
    BoxCollider collider = new BoxCollider(anchor, 1f, 2f, 16f, 16f);

    Rectangle bounds = collider.bounds(new Rectangle());
    assertEquals(11f, bounds.x, 1e-4f);
    assertEquals(22f, bounds.y, 1e-4f);
    assertEquals(16f, bounds.width, 1e-4f);
    assertEquals(16f, bounds.height, 1e-4f);

    anchor.moveTo(100f, 200f);
    collider.bounds(bounds);
    assertEquals(101f, bounds.x, 1e-4f);
    assertEquals(202f, bounds.y, 1e-4f);
  }

  @Test
  void overlapsDetectsIntersectionAndSeparation() {
    DefaultPositionComponent anchorA = new DefaultPositionComponent(0f, 0f);
    DefaultPositionComponent anchorB = new DefaultPositionComponent(8f, 0f);
    BoxCollider a = new BoxCollider(anchorA, 0f, 0f, 16f, 16f);
    BoxCollider b = new BoxCollider(anchorB, 0f, 0f, 16f, 16f);

    assertTrue(a.overlaps(b), "boxes overlap by 8 units on X");

    anchorB.moveTo(16f, 0f);
    assertFalse(a.overlaps(b), "edge-touching boxes do not overlap");

    anchorB.moveTo(17f, 0f);
    assertFalse(a.overlaps(b));
  }
}
