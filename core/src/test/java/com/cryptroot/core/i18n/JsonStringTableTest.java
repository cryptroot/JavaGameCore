package com.cryptroot.core.i18n;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * Behavioural tests for {@link JsonStringTable}. Loads the bundled engine string table ({@code
 * assets/i18n/core_en.json}) from the classpath.
 */
class JsonStringTableTest {

  private JsonStringTable enTable() {
    return new JsonStringTable(Locale.ENGLISH).merge("assets/i18n/core");
  }

  @Test
  void resolvesAKnownKey() {
    assertEquals("Energy", enTable().get("roster.stat.energy"));
  }

  @Test
  void missingKeyReturnsTheKeyItself() {
    assertEquals("no.such.key", enTable().get("no.such.key"));
  }

  @Test
  void getOrDefaultFallsBackForMissingKeys() {
    JsonStringTable t = enTable();
    assertEquals("Energy", t.getOrDefault("roster.stat.energy", "FALLBACK"));
    assertEquals("FALLBACK", t.getOrDefault("no.such.key", "FALLBACK"));
  }

  @Test
  void formatSubstitutesPositionalArguments() {
    // task.reject.busy = "{0} is already busy."
    assertEquals("Laura is already busy.", enTable().format("task.reject.busy", "Laura"));
  }

  @Test
  void mergeOverlaysLaterTablesOverEarlierKeys() {
    JsonStringTable t = new JsonStringTable(Locale.ENGLISH);
    assertFalse(t.has("roster.stat.energy"));
    t.merge("assets/i18n/core");
    assertTrue(t.has("roster.stat.energy"));
  }

  @Test
  void mergingAnAbsentTableIsANoOp() {
    JsonStringTable t = new JsonStringTable(Locale.ENGLISH).merge("assets/i18n/does_not_exist");
    assertFalse(t.has("roster.stat.energy"));
  }
}
