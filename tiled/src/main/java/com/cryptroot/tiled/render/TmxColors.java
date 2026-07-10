package com.cryptroot.tiled.render;

import com.badlogic.gdx.graphics.Color;

/**
 * Parses Tiled colour strings into libGDX {@link Color}s.
 *
 * <p>Tiled stores colours as {@code #AARRGGBB} or {@code #RRGGBB} (the leading {@code #} is
 * optional), i.e. with the alpha component <em>first</em> — unlike libGDX's {@code RRGGBBAA}
 * ordering — so the components are reordered here.
 */
public final class TmxColors {

  private TmxColors() {}

  /**
   * Parses a Tiled colour string.
   *
   * @param tmx a colour in {@code #AARRGGBB}, {@code AARRGGBB}, {@code #RRGGBB} or {@code RRGGBB}
   *     form
   * @return the corresponding {@link Color}
   * @throws IllegalArgumentException if {@code tmx} is null or malformed
   */
  public static Color parse(String tmx) {
    if (tmx == null) {
      throw new IllegalArgumentException("colour string must not be null");
    }
    String s = tmx.startsWith("#") ? tmx.substring(1) : tmx;
    int a;
    int r;
    int g;
    int b;
    if (s.length() == 8) {
      a = hex(s, 0);
      r = hex(s, 2);
      g = hex(s, 4);
      b = hex(s, 6);
    } else if (s.length() == 6) {
      a = 255;
      r = hex(s, 0);
      g = hex(s, 2);
      b = hex(s, 4);
    } else {
      throw new IllegalArgumentException("Unrecognised Tiled colour: " + tmx);
    }
    return new Color(r / 255f, g / 255f, b / 255f, a / 255f);
  }

  private static int hex(String s, int offset) {
    return Integer.parseInt(s.substring(offset, offset + 2), 16);
  }
}
