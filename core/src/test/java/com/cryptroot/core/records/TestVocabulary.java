package com.cryptroot.core.records;

/**
 * Test-only vocabulary for the record machinery.
 *
 * <p>The production package deliberately ships <em>no</em> dimensions or component values — every
 * axis the module blessed would be an axis a game had to route around, so the bundled example lives
 * in {@code com.cryptroot.core.records.sample} instead. The tests still need something to count, so
 * they declare their own here rather than reaching into the sample: a core test that imported the
 * sample would quietly stop proving the core is vocabulary-free.
 *
 * <p>Kept package-private and gathered in one file so it reads as a fixture rather than as API.
 * Each enum doubles as a worked example of the recommended style: a private token, an explicit
 * {@code DIMENSION} constant, and no reliance on {@code name()}.
 */
final class TestVocabulary {

  private TestVocabulary() {}
}

/** What was done. */
enum Action implements RecordComponent {
  USED("used"),
  EATEN("eaten");

  static final RecordDimension DIMENSION = RecordDimension.of("action");

  private final String value;

  Action(String value) {
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

/** What a record is about. */
enum Content implements RecordComponent {
  BLOOD("blood"),
  WATER("water"),
  UNKNOWN("unknown");

  static final RecordDimension DIMENSION = RecordDimension.of("content");

  private final String value;

  Content(String value) {
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

/** Where a record was accumulated. */
enum Context implements RecordComponent {
  MAP("map"),
  BATTLE("battle"),
  BANK("bank");

  static final RecordDimension DIMENSION = RecordDimension.of("context");

  private final String value;

  Context(String value) {
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

/** How a record is counted. */
enum Measure implements RecordComponent {
  AMOUNT("amount"),
  TIMES("times");

  static final RecordDimension DIMENSION = RecordDimension.of("measure");

  private final String value;

  Measure(String value) {
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

/** Who a record originated from. */
enum Origin implements RecordComponent {
  PLAYER("player"),
  NPC("npc"),
  ENEMY("enemy");

  static final RecordDimension DIMENSION = RecordDimension.of("origin");

  private final String value;

  Origin(String value) {
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

/** The condition of what a record is about. */
enum Quality implements RecordComponent {
  NORMAL("normal"),
  SPOILED("spoiled");

  static final RecordDimension DIMENSION = RecordDimension.of("quality");

  private final String value;

  Quality(String value) {
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

/** What the content was obtained from. */
enum Source implements RecordComponent {
  FOOD("food"),
  DRINK("drink");

  static final RecordDimension DIMENSION = RecordDimension.of("source");

  private final String value;

  Source(String value) {
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
