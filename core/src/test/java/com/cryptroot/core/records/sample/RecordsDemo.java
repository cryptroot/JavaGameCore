package com.cryptroot.core.records.sample;

import com.cryptroot.core.records.RecordAggregation;
import com.cryptroot.core.records.RecordBookComponent;
import com.cryptroot.core.records.RecordDefinition;
import com.cryptroot.core.records.RecordDimension;
import com.cryptroot.core.records.RecordKeeper;
import com.cryptroot.core.records.RecordKey;
import com.cryptroot.core.records.RecordQuery;
import com.cryptroot.core.records.RecordTemplate;
import com.cryptroot.core.world.TagComponent;
import com.cryptroot.core.world.World;
import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.core.world.component.DefaultTagComponent;
import java.util.Map;

/**
 * Headless walkthrough of the record system: defines a catalogue and a templated record family,
 * gives two world entities a record book each, accumulates records of two different aggregations,
 * resets the per-battle ones and prints a report.
 *
 * <p>Deliberately GL-free — no {@code GameContext}, no batch, no window — so it runs anywhere:
 *
 * <pre>{@code
 * mvn -pl core -q test -Dtest=com.cryptroot.core.records.sample.RecordsDemoTest
 * }</pre>
 */
public final class RecordsDemo {

  /** The axis distinguishing one skill from another within the skill-use record family. */
  private static final RecordDimension SKILL = RecordDimension.of("skill");

  private RecordsDemo() {}

  /** Runs the walkthrough, printing to standard output. */
  public static void main(String[] args) {
    RecordKeeper keeper = new RecordKeeper();
    BloodBankRecords bloodBank = new BloodBankRecords(keeper);

    // Records that only make sense inside a fight: these are the ones reset(Context.BATTLE) wipes.
    RecordKey enemyBloodDrawnAmount =
        keeper.define(
            RecordDefinition.builder()
                .components(Context.BATTLE, Content.BLOOD, Origin.ENEMY, Measure.AMOUNT)
                .name("Enemy Blood Drawn")
                .description("HP drained from enemies during the current battle")
                .build());
    RecordKey enemyBloodDrawnTimes =
        keeper.define(
            RecordDefinition.builder()
                .components(Context.BATTLE, Content.BLOOD, Origin.ENEMY, Measure.TIMES)
                .name("Enemy Blood Drawn Times")
                .description("Drain hits landed during the current battle")
                .build());
    // A high score rather than a counter: repeated observations keep the best, not the sum.
    RecordKey biggestDrain =
        keeper.define(
            RecordDefinition.builder()
                .components(Content.BLOOD, Origin.ENEMY, Quality.NORMAL, Measure.AMOUNT)
                .aggregation(RecordAggregation.MAX)
                .name("Biggest Drain")
                .description("Most HP drained by a single hit")
                .build());

    // One record per skill, minted on first use — no enumeration of skill ids anywhere.
    RecordTemplate skillUses =
        RecordTemplate.builder()
            .components(Action.USED, Measure.TIMES)
            .parameter(SKILL)
            .name(skill -> "Skill " + skill + " Uses")
            .description(skill -> "Times skill " + skill + " was used")
            .build();

    World world = new World();
    WorldEntity hero = world.add(battler(keeper, "hero"));
    WorldEntity rival = world.add(battler(keeper, "rival"));

    RecordBookComponent heroBook = book(hero);
    RecordBookComponent rivalBook = book(rival);

    heroBook
        .onRecorded()
        .connect(
            change ->
                System.out.println(
                    "  [signal] hero recorded "
                        + keeper.getName(change.key())
                        + " "
                        + change.previous()
                        + " -> "
                        + change.current()));

    heroBook.record(bloodBank.playerBankBloodTimes());
    heroBook.record(bloodBank.playerBankBloodAmount(), 120);
    heroBook.record(enemyBloodDrawnTimes, 3);
    heroBook.record(enemyBloodDrawnAmount, 46);
    heroBook.record(biggestDrain, 31);
    heroBook.record(biggestDrain, 12); // Silent: 12 does not beat 31.
    // Templated records are ordinary definitions, so they answer every query below.
    heroBook.record(skillUses.define(keeper, 3), 2);

    rivalBook.record(bloodBank.playerBankBloodTimes(), 4);
    rivalBook.record(enemyBloodDrawnAmount, 9);
    rivalBook.record(skillUses.define(keeper, 7));

    System.out.println();
    report(keeper, world, "after the battle");

    System.out.println("hero blood total (any context) : " + heroBook.total(Content.BLOOD));
    System.out.println(
        "hero bank donations (player)   : " + heroBook.total(Context.BANK, Origin.PLAYER));
    System.out.println("hero skill 3 uses              : " + heroBook.value(skillUses.key(3)));
    System.out.println("hero skill uses (any skill)    : " + heroBook.total(skillUses.family()));
    System.out.println("hero biggest single drain      : " + heroBook.value(biggestDrain));
    System.out.println(
        "hero every defined record      : " + heroBook.total(RecordQuery.anything()));

    for (WorldEntity entity : world.entities()) {
      book(entity).reset(Context.BATTLE);
    }

    System.out.println();
    report(keeper, world, "after resetting battle records");
  }

  private static WorldEntity battler(RecordKeeper keeper, String tag) {
    return new WorldEntity()
        .with(TagComponent.class, new DefaultTagComponent(tag))
        .with(RecordBookComponent.class, new RecordBookComponent(keeper));
  }

  private static RecordBookComponent book(WorldEntity entity) {
    return entity
        .get(RecordBookComponent.class)
        .orElseThrow(() -> new IllegalStateException("entity carries no record book"));
  }

  private static void report(RecordKeeper keeper, World world, String moment) {
    System.out.println("=== Records " + moment + " ===");
    for (WorldEntity entity : world.entities()) {
      System.out.println(entity.get(TagComponent.class).orElseThrow().tags());
      for (Map.Entry<RecordKey, Long> record : book(entity).snapshot().entrySet()) {
        RecordDefinition definition = keeper.get(record.getKey());
        System.out.printf(
            "  %-24s %4d  (%s)%n", definition.name(), record.getValue(), definition.aggregation());
      }
    }
    System.out.println();
  }
}
