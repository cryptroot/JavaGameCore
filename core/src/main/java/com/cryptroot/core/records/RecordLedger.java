package com.cryptroot.core.records;

import java.util.Map;

/**
 * Read-only access to one entity's records.
 *
 * <p>The interface {@link RecordBookComponent} implements, so anything that only <em>reads</em>
 * records — an achievement condition, a stats screen, a save writer — depends on this instead of on
 * the component, and therefore on neither {@code EntityComponent} nor the world.
 *
 * <pre>{@code
 * boolean isVeteran(RecordLedger records) {
 *   return records.total(RecordQuery.has(ENEMY_DEFEATED)) >= 1_000;
 * }
 * }</pre>
 *
 * <p>Every read is fail-soft: a record that was never observed reads as {@code 0}, because "never
 * happened" and "happened zero times" are the same thing to a record. Use {@link #isRecorded} when
 * the difference matters.
 */
public interface RecordLedger {

  /** The value stored for {@code key}, or {@code 0} if it has never been observed. */
  long value(RecordKey key);

  /**
   * {@code true} if {@code key} has ever been observed — distinguishes a stored {@code 0} from
   * none.
   */
  boolean isRecorded(RecordKey key);

  /**
   * The summed values of every <em>defined</em> record satisfying {@code query}.
   *
   * <p>Records with no definition are skipped: nothing can tell whether they satisfy the query.
   */
  long total(RecordQuery query);

  /** The summed values of every defined record carrying all of {@code query}. */
  long total(RecordComponent... query);

  /** An unmodifiable snapshot of every stored record — the save-and-report hook. */
  Map<RecordKey, Long> snapshot();
}
