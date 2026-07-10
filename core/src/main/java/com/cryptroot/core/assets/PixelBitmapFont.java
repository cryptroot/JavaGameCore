package com.cryptroot.core.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.BitmapFontData;
import com.badlogic.gdx.graphics.g2d.BitmapFont.Glyph;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.cryptroot.core.resources.ResourceManager;
import com.cryptroot.core.resources.ResourcePath;

/**
 * Loads a low-resolution bitmap font from a GameMaker-style glyph atlas: a single PNG sheet plus a
 * JSON array describing each glyph's source rectangle and advance.
 *
 * <h3>JSON format</h3>
 *
 * The JSON (e.g. {@code assets/fonts/en_pixel.json}) is a flat array of glyph records, one per
 * character:
 *
 * <pre>{@code
 * [ { "char": 65, "x": 12, "y": 24, "w": 6, "h": 9, "shift": 7, "offset": 0 }, ... ]
 * }</pre>
 *
 * where {@code char} is the Unicode code point, {@code x}/{@code y} the glyph's top-left corner in
 * the atlas (y measured downward from the top), {@code w}/{@code h} its pixel size, {@code shift}
 * the horizontal advance (x-advance), and {@code offset} the left-side bearing (x-offset).
 *
 * <h3>Filtering</h3>
 *
 * The atlas texture is always loaded {@link TextureFilter#Nearest} so the font stays crisp when a
 * pixel-art scene is upscaled. The returned {@link BitmapFont} uses integer positions for the same
 * reason.
 *
 * <p>The atlas {@link Texture} is owned by the supplied {@link ResourceManager} (and disposed with
 * it); the returned {@link BitmapFont} owns only its {@link BitmapFontData} and must be disposed by
 * its holder.
 *
 * <h3>Baseline metrics</h3>
 *
 * The JSON carries no global ascent/descent/line-gap, so this loader derives sensible defaults from
 * the (uniform) glyph height: the baseline sits at the glyph bottom and the line height equals the
 * glyph height. These metrics are a deliberate tuning point — adjust {@link #BASELINE_DESCENT} /
 * {@link #LINE_GAP} if the rendered text needs vertical nudging.
 */
public final class PixelBitmapFont {

  /**
   * Pixels of the glyph box that sit below the baseline. {@code 0} places the baseline at the very
   * bottom of each glyph; raise it if the font art reserves rows for descenders. Tuning point.
   */
  private static final int BASELINE_DESCENT = 0;

  /** Extra blank pixels added between successive text lines. Tuning point. */
  private static final int LINE_GAP = 1;

  private PixelBitmapFont() {}

  /**
   * Loads the font described by {@code jsonName} + {@code pngName}, both resolved under {@link
   * ResourcePath#FONTS} ({@code assets/fonts/}).
   *
   * @param resources the manager that loads and owns the atlas texture
   * @param jsonName glyph-atlas JSON filename, e.g. {@code "en_pixel.json"}
   * @param pngName glyph-atlas PNG filename, e.g. {@code "en_pixel.png"}
   * @return a ready-to-use {@link BitmapFont} (caller disposes)
   */
  public static BitmapFont load(ResourceManager resources, String jsonName, String pngName) {
    Texture texture =
        resources.loadTexture(
            ResourcePath.FONTS, pngName, TextureFilter.Nearest, TextureFilter.Nearest);
    TextureRegion sheet = new TextureRegion(texture);

    JsonValue root =
        new JsonReader().parse(Gdx.files.classpath(ResourcePath.FONTS.prefix() + jsonName));

    BitmapFontData data = new BitmapFontData();
    data.flipped = false;

    int glyphHeight = 0;
    for (JsonValue e = root.child; e != null; e = e.next) {
      int id = e.getInt("char");
      int x = e.getInt("x");
      int y = e.getInt("y");
      int w = e.getInt("w");
      int h = e.getInt("h");
      int shift = e.getInt("shift", w);
      int offset = e.getInt("offset", 0);

      Glyph glyph = new Glyph();
      glyph.id = id;
      glyph.srcX = x;
      glyph.srcY = y;
      glyph.width = w;
      glyph.height = h;
      glyph.xadvance = shift;
      glyph.xoffset = offset;
      // Non-flipped font: y grows up, draw Y is the line top; glyphs hang
      // below it, so yoffset is negative (top of glyph below the line top).
      glyph.yoffset = -(h - BASELINE_DESCENT);

      data.setGlyph(id, glyph);
      data.setGlyphRegion(glyph, sheet);

      glyphHeight = Math.max(glyphHeight, h);
      if (id == ' ') {
        data.spaceXadvance = shift;
      }
    }

    // Uniform-height pixel font: derive vertical metrics from the glyph box.
    data.lineHeight = glyphHeight + LINE_GAP;
    data.capHeight = glyphHeight - BASELINE_DESCENT;
    data.ascent = 0f;
    data.descent = 0f;
    data.down = -data.lineHeight;
    if (data.spaceXadvance == 0f) {
      data.spaceXadvance = glyphHeight * 0.5f;
    }

    BitmapFont font = new BitmapFont(data, sheet, true);
    font.setUseIntegerPositions(true);
    return font;
  }
}
