package com.cryptroot.core.ui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class OverlayTest {

  private static final float EPS = 1e-3f;

  @Test
  void everyChildReceivesTheIdenticalContentRectangle() {
    FakeElement a = new FakeElement(10f, 10f);
    FakeElement b = new FakeElement(500f, 500f);
    Overlay overlay = new Overlay().padding(Insets.all(12f));
    overlay.add(a).add(b);

    overlay.setBounds(100f, 200f, 400f, 300f);
    overlay.layout();

    assertEquals(112f, a.assigned.x, EPS);
    assertEquals(212f, a.assigned.y, EPS);
    assertEquals(376f, a.assigned.width, EPS);
    assertEquals(276f, a.assigned.height, EPS);

    assertEquals(a.assigned.x, b.assigned.x, EPS);
    assertEquals(a.assigned.y, b.assigned.y, EPS);
    assertEquals(a.assigned.width, b.assigned.width, EPS);
    assertEquals(a.assigned.height, b.assigned.height, EPS);
  }

  @Test
  void preferredSizeIsTheLargestChildInEachDimension() {
    Overlay overlay = new Overlay().padding(Insets.all(5f));
    overlay.add(new FakeElement(100f, 20f)).add(new FakeElement(30f, 80f));

    com.badlogic.gdx.math.Vector2 size = overlay.preferredSize(new com.badlogic.gdx.math.Vector2());

    assertEquals(110f, size.x, EPS);
    assertEquals(90f, size.y, EPS);
  }

  @Test
  void emptyOverlayReportsOnlyItsPadding() {
    Overlay overlay = new Overlay().padding(Insets.symmetric(8f, 3f));

    com.badlogic.gdx.math.Vector2 size = overlay.preferredSize(new com.badlogic.gdx.math.Vector2());

    assertEquals(16f, size.x, EPS);
    assertEquals(6f, size.y, EPS);
  }
}
