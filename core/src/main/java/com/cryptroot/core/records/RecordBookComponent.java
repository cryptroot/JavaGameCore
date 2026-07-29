package com.cryptroot.core.records;

import com.cryptroot.core.event.Signal;
import com.cryptroot.core.world.EntityComponent;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;

/**
 * One entity's records: a {@code key -> value} bag plus the queries that make it useful.
 *
 * <p>Attach it like any other component and read it back by type:
 *
 * <pre>{@code
 * WorldEntity hero = new WorldEntity()
 *     .with(RecordBookComponent.class, new RecordBookComponent(keeper));
 * hero.get(RecordBookComponent.class).ifPresent(book -> book.record(catalogue.enemiesDefeated()));
 * }</pre>
 *
 * <p>Pure data/logic, mirroring {@link com.cryptroot.core.world.component.HealthComponent}: this is
 * not a render or update component, values only change on an explicit {@link #record} / {@link
 * #reset} / {@link #restore} call, and nothing here touches GL.
 *
 * <p>Writing is {@link #record(RecordKey, long) record(key, observation)} rather than "increment"
 * because how an observation combines with what is already stored is the record's own business —
 * see {@link RecordAggregation}. A record with no definition in the {@link RecordKeeper}
 * accumulates as a {@link RecordAggregation#SUM} and is invisible to component queries, since
 * nothing can say what its components are; that is the shape of a key restored from an older save,
 * not something to rely on.
 */
public final class RecordBookComponent implements EntityComponent, RecordLedger {

  private final RecordKeeper keeper;
  private final Map<RecordKey, Long> records = new LinkedHashMap<>();
  private final Signal<RecordChange> onRecorded = new Signal<>();

  /**
   * Constructs an empty record book resolving its keys against {@code keeper}.
   *
   * @throws NullPointerException if {@code keeper} is {@code null}
   */
  public RecordBookComponent(RecordKeeper keeper) {
    this.keeper = Objects.requireNonNull(keeper, "keeper must not be null");
  }

  /**
   * Observes {@code 1} for {@code key} — the counting case, and an increment for a {@link
   * RecordAggregation#SUM} record.
   */
  public void record(RecordKey key) {
    record(key, 1);
  }

  /**
   * Observes {@code observation} for {@code key}, combining it with any value already stored
   * according to the record's {@link RecordAggregation}.
   *
   * <p>Two contracts depend on that aggregation, because they only ever made sense for {@code SUM}:
   *
   * <ul>
   *   <li>An {@code observation} of zero is a silent no-op for {@code SUM} — callers routinely
   *       forward an event's magnitude without pre-checking it — but real data for the others,
   *       where {@code 0} may well be the best time or the latest score.
   *   <li>A negative {@code observation} is rejected for {@code SUM}, since a counter only grows,
   *       and accepted for the others.
   * </ul>
   *
   * <p>{@link #onRecorded()} fires only if the stored value actually moved.
   *
   * @throws NullPointerException if {@code key} is {@code null}
   * @throws IllegalArgumentException if {@code observation} is negative and this record sums
   */
  public void record(RecordKey key, long observation) {
    Objects.requireNonNull(key, "key must not be null");
    RecordAggregation aggregation = aggregationOf(key);
    if (observation < 0 && !aggregation.allowsNegativeObservations()) {
      throw new IllegalArgumentException(
          "observation must not be negative for a " + aggregation + " record: " + observation);
    }
    if (observation == 0 && aggregation.treatsZeroAsNoOp()) {
      return;
    }
    Long stored = records.get(key);
    long previous = stored == null ? 0 : stored;
    long current =
        aggregation.combine(
            stored == null ? OptionalLong.empty() : OptionalLong.of(stored), observation);
    if (current == previous) {
      if (stored == null) {
        records.put(key, current);
      }
      return;
    }
    records.put(key, current);
    onRecorded.emit(new RecordChange(key, previous, current));
  }

  /**
   * The value stored for {@code key}.
   *
   * <p>Fail-soft, documented: a record that was never observed reads as {@code 0} rather than
   * throwing — "never happened" and "happened zero times" are the same thing to a record.
   */
  @Override
  public long value(RecordKey key) {
    Objects.requireNonNull(key, "key must not be null");
    return records.getOrDefault(key, 0L);
  }

  @Override
  public boolean isRecorded(RecordKey key) {
    Objects.requireNonNull(key, "key must not be null");
    return records.containsKey(key);
  }

  /**
   * The summed values of every defined record satisfying {@code query}.
   *
   * <p>Records whose key has no definition in the keeper are skipped, so a key restored from an
   * older save never leaks into a total. {@link RecordQuery#anything()} therefore sums every
   * <em>defined</em> record this entity has accumulated.
   */
  @Override
  public long total(RecordQuery query) {
    Objects.requireNonNull(query, "query must not be null");
    long total = 0;
    for (Map.Entry<RecordKey, Long> record : records.entrySet()) {
      if (keeper.matches(record.getKey(), query)) {
        total += record.getValue();
      }
    }
    return total;
  }

  /**
   * The summed values of every defined record carrying all of {@code query} — sugar for {@link
   * RecordQuery#allOf} over {@link RecordQuery#has}.
   *
   * <p>Two components on the same dimension is a contradiction, not an error: no record can carry
   * both, so the total is {@code 0}.
   */
  @Override
  public long total(RecordComponent... query) {
    Objects.requireNonNull(query, "query must not be null");
    RecordQuery composed = RecordQuery.anything();
    for (RecordComponent component : query) {
      Objects.requireNonNull(component, "query must not contain null");
      composed = composed.and(RecordQuery.has(component));
    }
    return total(composed);
  }

  /**
   * Resets every defined record satisfying {@code query}, leaving all others untouched — {@code
   * reset(RecordQuery.has(perBattleContext))} is the per-battle wipe that keeps lifetime totals
   * intact.
   *
   * <p>What "reset" means is the record's own business. A {@link RecordAggregation#SUM} record
   * stays in the book at {@code 0}, so a report still lists what this entity has ever touched;
   * every other aggregation is removed instead, because a zeroed best time could never be beaten.
   * Records with no definition are never reset.
   */
  public void reset(RecordQuery query) {
    for (RecordKey key : keeper.keysMatching(query, records.keySet())) {
      if (aggregationOf(key).clearedByReset()) {
        records.remove(key);
      } else {
        records.put(key, 0L);
      }
    }
  }

  /** Resets every defined record carrying {@code component}. */
  public void reset(RecordComponent component) {
    reset(RecordQuery.has(component));
  }

  /**
   * An unmodifiable snapshot of every stored record, in first-observation order — the reporting
   * hook, and half of the save path.
   *
   * <p>The returned map is a copy: later writes do not change it. Pair it with {@link #restore}.
   */
  @Override
  public Map<RecordKey, Long> snapshot() {
    return Collections.unmodifiableMap(new LinkedHashMap<>(records));
  }

  /**
   * Replaces every stored record with {@code saved} — the other half of the save path.
   *
   * <p>Deliberately permissive, because this reads save data rather than program state: a key with
   * no definition is kept (its definition may have been renamed or removed since), no aggregation
   * is applied, and nothing is emitted on {@link #onRecorded()} — restoring is not observing, and a
   * listener attached to a fresh book would otherwise see the entire save history as new events.
   *
   * @throws NullPointerException if {@code saved} is {@code null} or contains a {@code null} key or
   *     value
   */
  public void restore(Map<RecordKey, Long> saved) {
    Objects.requireNonNull(saved, "saved must not be null");
    saved.forEach(
        (key, value) -> {
          Objects.requireNonNull(key, "saved must not contain a null key");
          Objects.requireNonNull(value, "saved must not contain a null value");
        });
    records.clear();
    records.putAll(saved);
  }

  /**
   * Fires after every {@link #record} call that actually moved a value, never on {@link #restore}.
   *
   * <p>Listeners are read-only guests: recording from inside a listener would re-enter this book
   * while it is dispatching.
   */
  public Signal<RecordChange> onRecorded() {
    return onRecorded;
  }

  /**
   * A record with no definition sums, since that is the only aggregation needing no declaration.
   */
  private RecordAggregation aggregationOf(RecordKey key) {
    return keeper.find(key).map(RecordDefinition::aggregation).orElse(RecordAggregation.SUM);
  }
}
