package com.cryptroot.core.i18n;

/**
 * Resolves localization <em>keys</em> to display strings for the current language.
 *
 * <p>The game model and save layers never hold display text — they hold keys (e.g. {@code
 * "character.laura.name"}). The UI/engine layer resolves those keys through this service
 * immediately before drawing.
 *
 * <p>The backing data ships as JSON string tables (see {@link JsonStringTable}). A future build
 * will select the table by the player's language preference, possibly fetching it from a server;
 * that mechanism is not yet present, so the only bundled locale today is {@code en}.
 *
 * <h3>Incremental rollout</h3>
 *
 * <p>{@link #getOrDefault(String, String)} returns a supplied literal when a key is missing. This
 * lets call sites adopt keys one at a time: a not-yet-added key falls back to the literal that used
 * to be hard-coded, so the screen never shows a raw key or crashes during the migration.
 */
public interface Localization {

  /**
   * Returns the string mapped to {@code key}, or the key itself when no mapping exists (so a
   * missing key is visible in-game rather than throwing).
   */
  String get(String key);

  /**
   * Resolves {@code key} then substitutes positional arguments using {@link
   * java.text.MessageFormat} syntax ({@code {0}}, {@code {1}}, …).
   */
  String format(String key, Object... args);

  /**
   * Returns the string mapped to {@code key}, or {@code fallback} when no mapping exists. Use this
   * while migrating a hard-coded literal to a key.
   */
  String getOrDefault(String key, String fallback);

  /** {@code true} when {@code key} has a mapping in the current table. */
  boolean has(String key);
}
