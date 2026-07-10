package com.cryptroot.tiled.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.badlogic.gdx.graphics.Color;
import org.junit.jupiter.api.Test;

/** Tests for {@link TmxColors} parsing of Tiled colour strings (alpha-first ordering). */
class TmxColorsTest {

  private static final float EPS = 1f / 255f / 2f;

  @Test
  void parsesArgbWithLeadingHash() {
    Color c = TmxColors.parse("#80FF8040");
    assertEquals(0x80 / 255f, c.a, EPS);
    assertEquals(0xFF / 255f, c.r, EPS);
    assertEquals(0x80 / 255f, c.g, EPS);
    assertEquals(0x40 / 255f, c.b, EPS);
  }

  @Test
  void parsesRgbAsOpaque() {
    Color c = TmxColors.parse("#FF8040");
    assertEquals(1f, c.a, EPS);
    assertEquals(0xFF / 255f, c.r, EPS);
    assertEquals(0x80 / 255f, c.g, EPS);
    assertEquals(0x40 / 255f, c.b, EPS);
  }

  @Test
  void parsesWithoutLeadingHash() {
    Color c = TmxColors.parse("FF8040");
    assertEquals(1f, c.a, EPS);
    assertEquals(0xFF / 255f, c.r, EPS);
  }

  @Test
  void rejectsMalformedColour() {
    assertThrows(IllegalArgumentException.class, () -> TmxColors.parse("#FFF"));
  }
}
