package com.cryptroot.core.records;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptroot.core.world.WorldEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class RecordBookComponentTest {

  /** A well-formed key nothing ever defines — the shape of a key restored from an older save. */
  private static final RecordKey UNDEFINED =
      RecordKey.of(RecordComponent.of(RecordDimension.of("skill"), "3"));

  private final RecordKeeper keeper = new RecordKeeper();

  private final RecordKey bankTimes =
      define(RecordAggregation.SUM, Context.BANK, Content.BLOOD, Origin.PLAYER, Measure.TIMES);
  private final RecordKey bankAmount =
      define(RecordAggregation.SUM, Context.BANK, Content.BLOOD, Origin.PLAYER, Measure.AMOUNT);
  private final RecordKey battleTimes =
      define(RecordAggregation.SUM, Context.BATTLE, Content.BLOOD, Origin.ENEMY, Measure.TIMES);
  private final RecordKey battleAmount =
      define(RecordAggregation.SUM, Context.BATTLE, Content.BLOOD, Origin.ENEMY, Measure.AMOUNT);
  private final RecordKey mapWater =
      define(RecordAggregation.SUM, Context.MAP, Content.WATER, Origin.NPC, Measure.TIMES);
  private final RecordKey biggestDrain =
      define(RecordAggregation.MAX, Content.BLOOD, Origin.ENEMY, Quality.NORMAL, Measure.AMOUNT);
  private final RecordKey fastestDonation =
      define(RecordAggregation.MIN, Context.BANK, Content.BLOOD, Quality.NORMAL, Measure.TIMES);
  private final RecordKey lastDrink =
      define(RecordAggregation.LAST, Content.WATER, Source.DRINK, Measure.AMOUNT);

  private final RecordBookComponent book = new RecordBookComponent(keeper);

  private RecordKey define(RecordAggregation aggregation, RecordComponent... components) {
    return keeper.define(
        RecordDefinition.builder().components(components).aggregation(aggregation).build());
  }

  /** 1 + 120 bank blood, 3 + 46 battle blood, 7 map water, and an undefined counter at 5. */
  private void accumulate() {
    book.record(bankTimes);
    book.record(bankAmount, 120);
    book.record(battleTimes, 3);
    book.record(battleAmount, 46);
    book.record(mapWater, 7);
    book.record(UNDEFINED, 5);
  }

  @Test
  void unrecordedKeyReadsAsZero() {
    assertEquals(0, book.value(bankTimes));
    assertEquals(0, book.value(UNDEFINED));
    assertFalse(book.isRecorded(bankTimes));
  }

  @Test
  void isRecordedDistinguishesAStoredZeroFromNone() {
    book.record(battleTimes, 3);
    book.reset(Context.BATTLE);

    assertEquals(0, book.value(battleTimes));
    assertTrue(book.isRecorded(battleTimes));
  }

  @Test
  void recordDefaultsToOne() {
    book.record(bankTimes);

    assertEquals(1, book.value(bankTimes));
  }

  @Test
  void recordAccumulates() {
    book.record(bankTimes);
    book.record(bankTimes);
    book.record(bankTimes, 3);

    assertEquals(5, book.value(bankTimes));
  }

  @Test
  void recordByAmount() {
    book.record(bankAmount, 120);

    assertEquals(120, book.value(bankAmount));
  }

  @Test
  void recordAccumulatesBeyondIntRange() {
    book.record(bankAmount, Integer.MAX_VALUE);
    book.record(bankAmount, Integer.MAX_VALUE);

    assertEquals(2L * Integer.MAX_VALUE, book.value(bankAmount));
  }

  @Test
  void sumRejectsNegativeObservation() {
    assertThrows(IllegalArgumentException.class, () -> book.record(bankTimes, -1));
    assertThrows(IllegalArgumentException.class, () -> book.record(UNDEFINED, -1));
  }

  @Test
  void sumTreatsZeroAsNoOp() {
    book.record(bankTimes, 0);

    assertEquals(0, book.value(bankTimes));
    assertFalse(book.isRecorded(bankTimes));
    assertEquals(Map.of(), book.snapshot());
  }

  @Test
  void maxKeepsTheBestObservation() {
    book.record(biggestDrain, 31);
    book.record(biggestDrain, 12);
    book.record(biggestDrain, 44);

    assertEquals(44, book.value(biggestDrain));
  }

  @Test
  void minFirstObservationWinsOutright() {
    book.record(fastestDonation, 90);

    assertEquals(90, book.value(fastestDonation));

    book.record(fastestDonation, 120);

    assertEquals(90, book.value(fastestDonation));

    book.record(fastestDonation, 45);

    assertEquals(45, book.value(fastestDonation));
  }

  @Test
  void lastOverwrites() {
    book.record(lastDrink, 7);
    book.record(lastDrink, 2);

    assertEquals(2, book.value(lastDrink));
  }

  @Test
  void nonSummingRecordsAcceptZeroAndNegativeObservations() {
    book.record(lastDrink, 0);

    assertTrue(book.isRecorded(lastDrink));
    assertEquals(0, book.value(lastDrink));

    book.record(biggestDrain, -5);

    assertEquals(-5, book.value(biggestDrain));
  }

  @Test
  void onRecordedCarriesBothSidesOfTheChange() {
    List<RecordChange> changes = new ArrayList<>();
    book.onRecorded().connect(changes::add);

    book.record(bankAmount, 120);
    book.record(bankAmount, 30);

    assertEquals(
        List.of(new RecordChange(bankAmount, 0, 120), new RecordChange(bankAmount, 120, 150)),
        changes);
    assertEquals(30, changes.get(1).delta());
  }

  @Test
  void onRecordedIsSilentWhenNothingMoved() {
    AtomicInteger emissions = new AtomicInteger();
    AtomicReference<RecordChange> last = new AtomicReference<>();
    book.onRecorded()
        .connect(
            change -> {
              emissions.incrementAndGet();
              last.set(change);
            });

    book.record(bankTimes);
    book.record(bankAmount, 5);
    book.record(bankAmount, 0); // SUM: zero is a no-op.
    book.record(biggestDrain, 40);
    book.record(biggestDrain, 12); // MAX: does not beat 40.

    assertEquals(3, emissions.get());
    assertEquals(new RecordChange(biggestDrain, 0, 40), last.get());
  }

  @Test
  void valueReadsOneRecord() {
    accumulate();

    assertEquals(120, book.value(bankAmount));
    assertEquals(5, book.value(UNDEFINED));
  }

  @Test
  void totalBySingleComponentSumsMatchingRecords() {
    accumulate();

    assertEquals(170, book.total(Content.BLOOD));
    assertEquals(121, book.total(Context.BANK));
    assertEquals(7, book.total(Content.WATER));
  }

  @Test
  void totalByMultipleComponentsRequiresAll() {
    accumulate();

    assertEquals(121, book.total(Content.BLOOD, Context.BANK));
    assertEquals(120, book.total(Content.BLOOD, Context.BANK, Measure.AMOUNT));
    assertEquals(49, book.total(Content.BLOOD, Origin.ENEMY));
    assertEquals(0, book.total(Content.WATER, Origin.PLAYER));
  }

  @Test
  void totalAcceptsAComposedQuery() {
    accumulate();

    assertEquals(
        49, book.total(RecordQuery.has(Content.BLOOD).and(RecordQuery.has(Context.BANK).negate())));
    assertEquals(
        177, book.total(RecordQuery.has(Content.BLOOD).or(RecordQuery.has(Content.WATER))));
    assertEquals(177, book.total(RecordQuery.hasDimension(Measure.DIMENSION)));
    assertEquals(0, book.total(RecordQuery.nothing()));
  }

  @Test
  void totalIgnoresUndefinedKeys() {
    accumulate();

    assertEquals(177, book.total(RecordQuery.anything()));
    assertEquals(170, book.total(Content.BLOOD));
    assertEquals(5, book.value(UNDEFINED));
  }

  @Test
  void totalIsZeroWhenNothingMatches() {
    accumulate();

    assertEquals(0, book.total(Quality.SPOILED));
    assertEquals(0, book.total(Source.DRINK));
  }

  @Test
  void totalOfContradictoryComponentsIsZero() {
    accumulate();

    assertEquals(0, book.total(Content.BLOOD, Content.WATER));
  }

  @Test
  void totalRejectsNullArguments() {
    assertThrows(NullPointerException.class, () -> book.total((RecordComponent[]) null));
    assertThrows(NullPointerException.class, () -> book.total((RecordQuery) null));
    assertThrows(NullPointerException.class, () -> book.total(Content.BLOOD, null));
  }

  @Test
  void resetZeroesOnlyMatchingRecords() {
    accumulate();

    book.reset(Context.BATTLE);

    assertEquals(0, book.value(battleTimes));
    assertEquals(0, book.value(battleAmount));
    assertEquals(0, book.total(Context.BATTLE));
  }

  @Test
  void resetKeepsLifetimeRecordsIntact() {
    accumulate();

    book.reset(Context.BATTLE);

    assertEquals(1, book.value(bankTimes));
    assertEquals(120, book.value(bankAmount));
    assertEquals(7, book.value(mapWater));
    assertEquals(121, book.total(Context.BANK));
  }

  @Test
  void resetKeepsSummingKeysAtZeroRatherThanRemovingThem() {
    accumulate();

    book.reset(Context.BATTLE);

    assertTrue(book.snapshot().containsKey(battleTimes));
    assertEquals(0, book.snapshot().get(battleTimes));
  }

  @Test
  void resetClearsRecordsWithNoMeaningfulZero() {
    book.record(fastestDonation, 90);
    book.record(biggestDrain, 31);

    book.reset(Content.BLOOD);

    // A zeroed best time could never be beaten, so the record is removed instead.
    assertFalse(book.isRecorded(fastestDonation));
    assertFalse(book.isRecorded(biggestDrain));

    book.record(fastestDonation, 120);

    assertEquals(120, book.value(fastestDonation));
  }

  @Test
  void resetLeavesUndefinedKeysAlone() {
    accumulate();

    book.reset(Context.BATTLE);

    assertEquals(5, book.value(UNDEFINED));
  }

  @Test
  void resetIsNoOpWhenNothingMatches() {
    accumulate();
    Map<RecordKey, Long> before = book.snapshot();

    book.reset(Quality.SPOILED);

    assertEquals(before, book.snapshot());
  }

  @Test
  void resetRejectsNullQuery() {
    assertThrows(NullPointerException.class, () -> book.reset((RecordComponent) null));
    assertThrows(NullPointerException.class, () -> book.reset((RecordQuery) null));
  }

  @Test
  void snapshotIsUnmodifiable() {
    book.record(bankTimes);

    assertThrows(UnsupportedOperationException.class, () -> book.snapshot().clear());
  }

  @Test
  void snapshotIsIndependentOfLaterWrites() {
    book.record(bankTimes);
    Map<RecordKey, Long> snapshot = book.snapshot();

    book.record(bankTimes);
    book.record(bankAmount, 120);

    assertEquals(Map.of(bankTimes, 1L), snapshot);
  }

  @Test
  void snapshotIsInFirstObservationOrder() {
    book.record(mapWater);
    book.record(bankTimes);

    assertEquals(List.of(mapWater, bankTimes), List.copyOf(book.snapshot().keySet()));
  }

  @Test
  void restoreRoundTripsASnapshot() {
    accumulate();
    book.record(biggestDrain, 31);
    Map<RecordKey, Long> saved = book.snapshot();

    RecordBookComponent loaded = new RecordBookComponent(keeper);
    loaded.restore(saved);

    assertEquals(saved, loaded.snapshot());
    // 1 + 120 bank, 3 + 46 battle, and the 31 high score — mixed aggregations sum alike in a total.
    assertEquals(201, loaded.total(Content.BLOOD));
    assertEquals(31, loaded.value(biggestDrain));
    assertEquals(5, loaded.value(UNDEFINED));
  }

  @Test
  void restoreReplacesEverythingAlreadyStored() {
    book.record(bankTimes, 9);

    book.restore(Map.of(mapWater, 4L));

    assertEquals(Map.of(mapWater, 4L), book.snapshot());
    assertFalse(book.isRecorded(bankTimes));
  }

  @Test
  void restoreDoesNotEmit() {
    AtomicInteger emissions = new AtomicInteger();
    book.onRecorded().connect(change -> emissions.incrementAndGet());

    book.restore(Map.of(bankTimes, 12L));

    assertEquals(0, emissions.get());
    assertEquals(12, book.value(bankTimes));
  }

  @Test
  void restoreRejectsNullArguments() {
    assertThrows(NullPointerException.class, () -> book.restore(null));
  }

  @Test
  void rejectsNullKeeper() {
    assertThrows(NullPointerException.class, () -> new RecordBookComponent(null));
  }

  @Test
  void rejectsNullKey() {
    assertThrows(NullPointerException.class, () -> book.record(null));
    assertThrows(NullPointerException.class, () -> book.record(null, 2));
    assertThrows(NullPointerException.class, () -> book.value(null));
    assertThrows(NullPointerException.class, () -> book.isRecorded(null));
  }

  @Test
  void attachesToWorldEntityAndIsRetrievableByType() {
    WorldEntity battler = new WorldEntity().with(RecordBookComponent.class, book);

    assertTrue(battler.has(RecordBookComponent.class));
    assertSame(book, battler.get(RecordBookComponent.class).orElseThrow());
  }

  @Test
  void isUsableThroughTheReadOnlyLedgerInterface() {
    accumulate();
    RecordLedger ledger = book;

    assertEquals(170, ledger.total(RecordQuery.has(Content.BLOOD)));
    assertEquals(120, ledger.value(bankAmount));
    assertTrue(ledger.isRecorded(bankAmount));
  }
}
