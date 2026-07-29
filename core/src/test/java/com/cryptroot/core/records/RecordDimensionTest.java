package com.cryptroot.core.records;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

class RecordDimensionTest {

  @Test
  void aDimensionIsJustItsId() {
    assertEquals("ship", RecordDimension.of("ship").id());
    assertEquals("ship", RecordDimension.of("ship").toString());
  }

  @Test
  void equalityIsByValueNotIdentity() {
    assertEquals(RecordDimension.of("ship"), RecordDimension.of("ship"));
    assertEquals(RecordDimension.of("ship").hashCode(), RecordDimension.of("ship").hashCode());
    assertNotEquals(RecordDimension.of("ship"), RecordDimension.of("shop"));
  }

  @Test
  void namespacedFormJoinsWithAColon() {
    assertEquals("starfarer:ship", RecordDimension.of("starfarer", "ship").id());
    assertEquals(RecordDimension.of("starfarer:ship"), RecordDimension.of("starfarer", "ship"));
  }

  @Test
  void namespacingKeepsTwoGamesApart() {
    assertNotEquals(
        RecordDimension.of("starfarer", "ship"), RecordDimension.of("cryptroot", "ship"));
  }

  @Test
  void acceptsCanonicalIds() {
    for (String id :
        List.of("a", "0", "ship", "ship_class", "ship.class", "ship-class", "ns:ship")) {
      assertEquals(id, RecordDimension.of(id).id());
    }
  }

  @Test
  void rejectsNonCanonicalIds() {
    for (String id : List.of("", " ", "Ship", "SHIP", "ship class", "_ship", ":ship", "ship!")) {
      assertThrows(
          IllegalArgumentException.class,
          () -> RecordDimension.of(id),
          () -> "expected \"" + id + "\" to be rejected");
    }
  }

  @Test
  void rejectsIdsCarryingKeySyntax() {
    assertThrows(IllegalArgumentException.class, () -> RecordDimension.of("ship=class"));
    assertThrows(IllegalArgumentException.class, () -> RecordDimension.of("ship|class"));
  }

  @Test
  void rejectsNullId() {
    assertThrows(NullPointerException.class, () -> RecordDimension.of(null));
    assertThrows(NullPointerException.class, () -> RecordDimension.of(null, "ship"));
    assertThrows(NullPointerException.class, () -> RecordDimension.of("starfarer", null));
  }

  @Test
  void rejectsASeparatorInsideEitherNamespacedSegment() {
    assertThrows(IllegalArgumentException.class, () -> RecordDimension.of("star:farer", "ship"));
    assertThrows(
        IllegalArgumentException.class, () -> RecordDimension.of("starfarer", "ship:hull"));
  }

  @Test
  void ordersById() {
    SortedSet<RecordDimension> sorted =
        new TreeSet<>(
            List.of(
                RecordDimension.of("origin"),
                RecordDimension.of("content"),
                RecordDimension.of("measure")));

    assertEquals(
        List.of(
            RecordDimension.of("content"),
            RecordDimension.of("measure"),
            RecordDimension.of("origin")),
        List.copyOf(sorted));
  }

  @Test
  void bundledSampleDimensionsAreOrdinaryValues() {
    assertEquals(RecordDimension.of("content"), Content.DIMENSION);
    assertEquals(Content.DIMENSION, Content.BLOOD.dimension());
    assertTrue(Content.DIMENSION.compareTo(Context.DIMENSION) < 0);
  }
}
