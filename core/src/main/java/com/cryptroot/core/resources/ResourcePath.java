package com.cryptroot.core.resources;

/**
 * Enumeration of the top-level asset directory paths used as roots when loading resources through
 * {@link ResourceManager}.
 *
 * <p>Each constant holds a classpath prefix that is prepended to the caller-supplied filename or
 * sub-path by {@link ResourceManager#loadTexture(ResourcePath, String)}.
 *
 * <h3>Sub-path example</h3>
 *
 * <pre>{@code
 * // Resolves to "assets/ui/icons/bar_quest_icon.png"
 * resources.loadTexture(ResourcePath.UI, "icons/bar_quest_icon.png");
 * }</pre>
 */
public enum ResourcePath {

  /** {@code assets/bg/} — full-screen background images. */
  BG("assets/bg/"),

  /** {@code assets/ui/} — UI textures, nine-patch slices, and icon sub-folders. */
  UI("assets/ui/"),

  /** {@code assets/fonts/} — TrueType / OpenType font files. */
  FONTS("assets/fonts/"),

  /** {@code assets/i18n/} — JSON localization string tables ({@code <base>_<lang>.json}). */
  I18N("assets/i18n/");

  private final String prefix;

  ResourcePath(String prefix) {
    this.prefix = prefix;
  }

  /** Returns the classpath prefix string, including the trailing {@code /}. */
  public String prefix() {
    return prefix;
  }
}
