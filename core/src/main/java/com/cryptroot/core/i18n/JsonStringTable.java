package com.cryptroot.core.i18n;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * {@link Localization} backed by flat JSON string tables loaded from the classpath.
 *
 * <p>Each table is a single JSON object of {@code "key": "value"} pairs, named {@code
 * <base>_<language>.json} (mirroring {@code I18NBundle} naming), e.g. {@code
 * assets/i18n/core_en.json}. Multiple bases may be {@linkplain #merge(String) merged}; later merges
 * override earlier keys, so a game module can shadow an engine default.
 *
 * <p>A missing table file is ignored (no crash) — a module that ships no strings simply contributes
 * nothing. A malformed table throws {@link IllegalStateException}, matching {@link
 * com.cryptroot.core.resources.AssetDescriptorLoader} which fails fast on bad data.
 *
 * <p>Pure Jackson + JDK — usable from any module.
 */
public final class JsonStringTable implements Localization {

  private final ObjectMapper mapper = new ObjectMapper();
  private final Locale locale;
  private final Map<String, String> table = new HashMap<>();

  public JsonStringTable(Locale locale) {
    this.locale = Objects.requireNonNull(locale, "locale must not be null");
  }

  /**
   * Loads {@code <base>_<language>.json} from the classpath and overlays its entries onto this
   * table (overriding any existing keys).
   *
   * @param base classpath base without the locale suffix or extension, e.g. {@code
   *     "assets/i18n/core"}
   * @return this table, for chaining
   */
  public JsonStringTable merge(String base) {
    Objects.requireNonNull(base, "base must not be null");
    String path = base + "_" + locale.getLanguage() + ".json";
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
      if (stream == null) {
        return this; // module ships no strings for this base — fine
      }
      MapType type =
          mapper.getTypeFactory().constructMapType(HashMap.class, String.class, String.class);
      Map<String, String> loaded = mapper.readValue(stream, type);
      table.putAll(loaded);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to parse string table: " + path, e);
    }
    return this;
  }

  @Override
  public String get(String key) {
    return table.getOrDefault(key, key);
  }

  @Override
  public String format(String key, Object... args) {
    String pattern = table.get(key);
    if (pattern == null) return key;
    return new MessageFormat(pattern, locale).format(args);
  }

  @Override
  public String getOrDefault(String key, String fallback) {
    return table.getOrDefault(key, fallback);
  }

  @Override
  public boolean has(String key) {
    return table.containsKey(key);
  }
}
