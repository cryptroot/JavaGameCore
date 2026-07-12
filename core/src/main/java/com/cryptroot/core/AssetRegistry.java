package com.cryptroot.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.utils.Disposable;
import com.cryptroot.core.assets.PixelBitmapFont;
import com.cryptroot.core.i18n.JsonStringTable;
import com.cryptroot.core.i18n.Localization;
import com.cryptroot.core.resources.ResourceManager;
import com.cryptroot.core.resources.ResourcePath;
import com.cryptroot.core.ui.UiSkin;
import java.util.Locale;
import java.util.Objects;

/**
 * Central holder for all game assets: textures, atlases, fonts, nine-patch slices, and the three
 * asset repositories.
 *
 * <p>Must be created after LibGDX initialisation (i.e. inside {@link
 * com.badlogic.gdx.Game#create()}). Dispose when the game exits.
 */
public final class AssetRegistry implements Disposable {

  /** Classpath base of the engine-wide string table ({@code core_en.json}). */
  private static final String CORE_STRINGS = ResourcePath.I18N.prefix() + "core";

  private final ResourceManager resources;
  private final JsonStringTable localization;

  private final BitmapFont fontHint;
  private final BitmapFont fontBody;
  private final BitmapFont fontMenu;
  private final BitmapFont fontTitle;

  /** Lazily built low-resolution bitmap font (see {@link #pixelFont()}). */
  private BitmapFont pixelFont;

  private final NinePatch lightBorder;
  private final NinePatch selectedBorder;

  public AssetRegistry() {
    resources = new ResourceManager();

    // Engine-wide localization; games overlay their own table via registerStrings().
    localization = new JsonStringTable(Locale.ENGLISH).merge(CORE_STRINGS);

    fontHint = buildFont(22);
    fontBody = buildFont(34);
    fontMenu = buildFont(52);
    fontTitle = buildFont(72);

    lightBorder =
        new NinePatch(resources.loadTexture(ResourcePath.UI, "light_border_slice.png"), 8, 8, 8, 8);
    selectedBorder =
        new NinePatch(
            resources.loadTexture(ResourcePath.UI, "selected_border_slice.png"), 8, 8, 8, 8);
  }

  // -------------------------------------------------------------------------
  // Accessors
  // -------------------------------------------------------------------------

  public ResourceManager resources() {
    return resources;
  }

  public Localization localization() {
    return localization;
  }

  public NinePatch lightBorder() {
    return lightBorder;
  }

  public NinePatch selectedBorder() {
    return selectedBorder;
  }

  /**
   * Overlays an additional JSON string table (game-specific keys) on top of the engine defaults.
   * Call once at startup before any screen is shown.
   *
   * @param classpathBase base path without locale/extension, e.g. {@code "assets/i18n/kennel"}
   */
  public void registerStrings(String classpathBase) {
    Objects.requireNonNull(classpathBase, "classpathBase must not be null");
    localization.merge(classpathBase);
  }

  public BitmapFont font(FontSize size) {
    Objects.requireNonNull(size, "size must not be null");
    return switch (size) {
      case HINT -> fontHint;
      case BODY -> fontBody;
      case MENU -> fontMenu;
      case TITLE -> fontTitle;
    };
  }

  public UiSkin defaultSkin() {
    return new UiSkin(lightBorder, selectedBorder, fontBody);
  }

  /**
   * Returns the shared low-resolution bitmap font, loaded from {@code
   * assets/fonts/en_pixel.{json,png}} on first use.
   *
   * <p>This is the default font for pixel-art games that render into a low-resolution framebuffer:
   * its 9px glyphs upscale crisply with the rest of the scene, unlike the FreeType {@link
   * #font(FontSize)} faces which are baked for high-resolution worlds. It is built lazily so games
   * that never use it pay no load cost.
   *
   * @return the cached-or-newly-built pixel {@link BitmapFont} (never {@code null})
   */
  public BitmapFont pixelFont() {
    if (pixelFont == null) {
      pixelFont = PixelBitmapFont.load(resources, "en_pixel.json", "en_pixel.png");
    }
    return pixelFont;
  }

  /** Convenience shortcut: {@code resources().getPixelTexture()}. */
  public Texture pixel() {
    return resources.getPixelTexture();
  }

  // -------------------------------------------------------------------------
  // Disposable
  // -------------------------------------------------------------------------

  @Override
  public void dispose() {
    resources.dispose();
    fontHint.dispose();
    fontBody.dispose();
    fontMenu.dispose();
    fontTitle.dispose();
    if (pixelFont != null) pixelFont.dispose();
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private static BitmapFont buildFont(int size) {
    FreeTypeFontGenerator generator =
        new FreeTypeFontGenerator(Gdx.files.classpath("assets/fonts/NotoSans-Regular.ttf"));
    FreeTypeFontParameter param = new FreeTypeFontParameter();
    param.size = size;
    param.minFilter = TextureFilter.Linear;
    param.magFilter = TextureFilter.Linear;
    BitmapFont font = generator.generateFont(param);
    generator.dispose();
    return font;
  }
}
