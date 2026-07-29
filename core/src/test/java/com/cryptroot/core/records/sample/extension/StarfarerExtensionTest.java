package com.cryptroot.core.records.sample.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptroot.core.event.DisposableConnection;
import com.cryptroot.core.records.RecordAggregation;
import com.cryptroot.core.records.RecordBookComponent;
import com.cryptroot.core.records.RecordChange;
import com.cryptroot.core.records.RecordComponent;
import com.cryptroot.core.records.RecordDefinition;
import com.cryptroot.core.records.RecordDimension;
import com.cryptroot.core.records.RecordKeeper;
import com.cryptroot.core.records.RecordKey;
import com.cryptroot.core.records.RecordLedger;
import com.cryptroot.core.records.RecordQuery;
import com.cryptroot.core.records.RecordTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The acceptance test for the record system's extensibility: a game declares its own axes, values,
 * records, record families and statistics without one edit to {@code com.cryptroot.core.records}.
 *
 * <p>The package is deliberately outside the module, so this also proves mechanically that no
 * package-private access is needed. Nothing here imports {@code …records.sample} either — a game
 * gets no vocabulary from the module and needs none.
 */
class StarfarerExtensionTest {

  // A brand-new axis with a closed vocabulary
  private enum Ship implements RecordComponent {
    INTERCEPTOR("interceptor"),
    FREIGHTER("freighter");

    static final RecordDimension DIMENSION = RecordDimension.of("starfarer", "ship");

    private final String value;

    Ship(String value) {
      this.value = value;
    }

    @Override
    public RecordDimension dimension() {
      return DIMENSION;
    }

    @Override
    public String value() {
      return value;
    }
  }

  // ...and brand-new axes with no enum at all, for values that come from content data.
  private static final RecordDimension EVENT = RecordDimension.of("starfarer", "event");
  private static final RecordDimension MEASURE = RecordDimension.of("measure");
  private static final RecordDimension WEAPON = RecordDimension.of("starfarer", "weapon");

  private static final RecordComponent JUMP = RecordComponent.of(EVENT, "hyperjump");
  private static final RecordComponent KILL = RecordComponent.of(EVENT, "kill");
  private static final RecordComponent TIMES = RecordComponent.of(MEASURE, "times");
  private static final RecordComponent LIGHT_YEARS = RecordComponent.of(MEASURE, "light-years");

  private final RecordKeeper keeper = new RecordKeeper();

  private final RecordKey jumps =
      keeper.define(
          RecordDefinition.builder()
              // An enum component and an of() component, mixed in one definition.
              .components(Ship.INTERCEPTOR, JUMP, TIMES)
              .name("Hyperjumps (Interceptor)")
              .description("Jumps completed in an interceptor")
              .build());

  private final RecordKey longestJump =
      keeper.define(
          RecordDefinition.builder()
              .components(Ship.INTERCEPTOR, JUMP, LIGHT_YEARS)
              .aggregation(RecordAggregation.MAX) // A non-monotonic statistic.
              .name("Longest Hyperjump")
              .build());

  private final RecordKey freighterKills =
      keeper.define(
          RecordDefinition.builder().components(Ship.FREIGHTER, KILL, TIMES).name("Hauls").build());

  // A parameterised family: one record per weapon, minted on first use, ids from content data.
  private final RecordTemplate weaponKills =
      RecordTemplate.builder()
          .components(KILL, TIMES)
          .parameter(WEAPON)
          .name(weapon -> "Kills with " + weapon)
          .build();

  private final RecordBookComponent book = new RecordBookComponent(keeper);

  @Test
  void keysAreDimensionQualifiedAndNamespacedByTheGame() {
    assertEquals("measure=times|starfarer:event=hyperjump|starfarer:ship=interceptor", jumps.id());
    assertEquals(
        "measure=light-years|starfarer:event=hyperjump|starfarer:ship=interceptor",
        longestJump.id());
  }

  @Test
  void keysRoundTripBackIntoTheGamesOwnComponents() {
    RecordKey parsed = RecordKey.parse(jumps.id());

    assertEquals(jumps, parsed);
    assertEquals(
        Map.of(
            Ship.DIMENSION,
            RecordComponent.canonical(Ship.INTERCEPTOR),
            EVENT,
            JUMP,
            MEASURE,
            TIMES),
        parsed.components());
  }

  @Test
  void aNamespacedAxisCannotCollideWithAnotherGames() {
    RecordDimension otherGamesShip = RecordDimension.of("cryptroot", "ship");

    assertFalse(keeper.get(jumps).has(RecordComponent.of(otherGamesShip, "interceptor")));
  }

  @Test
  void recordsAccumulateAndAggregatePerDefinition() {
    book.record(jumps, 50);
    book.record(longestJump, 14);
    book.record(longestJump, 9); // MAX, so 9 does not beat 14.

    assertEquals(50, book.value(jumps));
    assertEquals(14, book.value(longestJump));
  }

  @Test
  void templatedRecordsAreOrdinaryRecords() {
    RecordKey railgunKills = weaponKills.define(keeper, "railgun");
    RecordKey laserKills = weaponKills.define(keeper, "laser");

    book.record(railgunKills, 100);
    book.record(laserKills, 20);

    assertEquals("Kills with railgun", keeper.getName(railgunKills));
    assertEquals(120, book.total(weaponKills.family()));
    assertEquals(120, book.total(KILL));
    assertEquals(railgunKills, weaponKills.define(keeper, "railgun"));
  }

  @Test
  void queriesCompose() {
    book.record(weaponKills.define(keeper, "railgun"), 100);
    book.record(freighterKills, 7);

    long notFromAFreighter =
        book.total(RecordQuery.has(KILL).and(RecordQuery.has(Ship.FREIGHTER).negate()));

    assertEquals(107, book.total(KILL));
    assertEquals(100, notFromAFreighter);
  }

  @Test
  void changesCarryEnoughToDriveAnAchievement() {
    List<String> unlocked = new ArrayList<>();
    DisposableConnection wiring =
        book.onRecorded()
            .connect(
                change -> {
                  if (change.key().equals(jumps)
                      && change.previous() < 50
                      && change.current() >= 50) {
                    unlocked.add("starfarer:trailblazer");
                  }
                });

    book.record(jumps, 49);
    assertEquals(List.of(), unlocked);

    book.record(jumps, 1);
    assertEquals(List.of("starfarer:trailblazer"), unlocked);

    wiring.disconnect();
    book.record(jumps, 100);

    assertEquals(List.of("starfarer:trailblazer"), unlocked);
  }

  @Test
  void readOnlyConsumersNeedOnlyTheLedger() {
    book.record(jumps, 50);

    assertTrue(isVeteran(book));
  }

  @Test
  void savingAndLoadingNeedsNoSerializationDependency() {
    book.record(jumps, 50);
    book.record(longestJump, 14);
    book.record(weaponKills.define(keeper, "railgun"), 100);

    Map<RecordKey, Long> saved = book.snapshot();
    RecordBookComponent loaded = new RecordBookComponent(keeper);
    loaded.restore(saved);

    assertEquals(saved, loaded.snapshot());
    assertEquals(50, loaded.value(jumps));
    assertEquals(14, loaded.value(longestJump));
    assertEquals(100, loaded.total(weaponKills.family()));
  }

  /** A read-only consumer: no component, no world, no keeper — just the ledger. */
  private static boolean isVeteran(RecordLedger records) {
    return records.total(RecordQuery.has(JUMP)) >= 50;
  }

  @Test
  void changePayloadIsTheWholeTransition() {
    List<RecordChange> changes = new ArrayList<>();
    book.onRecorded().connect(changes::add);

    book.record(jumps, 20);
    book.record(jumps, 30);

    assertEquals(List.of(new RecordChange(jumps, 0, 20), new RecordChange(jumps, 20, 50)), changes);
    assertEquals(30, changes.get(1).delta());
  }
}
