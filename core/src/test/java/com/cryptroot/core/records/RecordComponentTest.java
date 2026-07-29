package com.cryptroot.core.records;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RecordComponentTest {

  private static final RecordDimension SHIP = RecordDimension.of("starfarer", "ship");

  /** A third-party implementation that is neither an enum nor a {@link RecordComponent.Basic}. */
  private static final class Foreign implements RecordComponent {

    @Override
    public RecordDimension dimension() {
      return SHIP;
    }

    @Override
    public String value() {
      return "interceptor";
    }
  }

  @Test
  void ofNeedsNoDeclaration() {
    RecordComponent ship = RecordComponent.of(SHIP, "interceptor");

    assertEquals(SHIP, ship.dimension());
    assertEquals("interceptor", ship.value());
    assertEquals("starfarer:ship=interceptor", ship.toString());
  }

  @Test
  void equalityIsByDimensionAndValue() {
    assertEquals(RecordComponent.of(SHIP, "interceptor"), RecordComponent.of(SHIP, "interceptor"));
    assertNotEquals(RecordComponent.of(SHIP, "interceptor"), RecordComponent.of(SHIP, "freighter"));
    assertNotEquals(
        RecordComponent.of(SHIP, "interceptor"),
        RecordComponent.of(RecordDimension.of("cryptroot", "ship"), "interceptor"));
  }

  @Test
  void rejectsNonCanonicalValue() {
    assertThrows(IllegalArgumentException.class, () -> RecordComponent.of(SHIP, "Interceptor"));
    assertThrows(IllegalArgumentException.class, () -> RecordComponent.of(SHIP, ""));
    assertThrows(
        IllegalArgumentException.class, () -> RecordComponent.of(SHIP, "fast interceptor"));
    assertThrows(IllegalArgumentException.class, () -> RecordComponent.of(SHIP, "a=b"));
    assertThrows(IllegalArgumentException.class, () -> RecordComponent.of(SHIP, "a|b"));
  }

  @Test
  void rejectsNullArguments() {
    assertThrows(NullPointerException.class, () -> RecordComponent.of(null, "interceptor"));
    assertThrows(NullPointerException.class, () -> RecordComponent.of(SHIP, null));
    assertThrows(NullPointerException.class, () -> RecordComponent.canonical(null));
  }

  @Test
  void canonicalReturnsBasicUnchanged() {
    RecordComponent basic = RecordComponent.of(SHIP, "interceptor");

    assertSame(basic, RecordComponent.canonical(basic));
  }

  @Test
  void canonicalFlattensAnyImplementation() {
    assertEquals(RecordComponent.of(SHIP, "interceptor"), RecordComponent.canonical(new Foreign()));
    assertEquals(
        RecordComponent.of(Content.DIMENSION, "blood"), RecordComponent.canonical(Content.BLOOD));
  }

  @Test
  void canonicalRejectsANonCanonicalImplementation() {
    RecordComponent shouty =
        new RecordComponent() {
          @Override
          public RecordDimension dimension() {
            return SHIP;
          }

          @Override
          public String value() {
            return "INTERCEPTOR";
          }
        };

    assertThrows(IllegalArgumentException.class, () -> RecordComponent.canonical(shouty));
  }

  @Test
  void anEnumAndAForeignImplementationAreInterchangeableInEveryQuery() {
    RecordKeeper keeper = new RecordKeeper();
    // Defined with a foreign implementation...
    RecordKey key =
        keeper.define(
            RecordDefinition.builder()
                .components(new Foreign(), Measure.TIMES)
                .name("Jumps")
                .build());

    // ...and queried with an of() component naming the same axis and value.
    RecordComponent equivalent = RecordComponent.of(SHIP, "interceptor");

    assertEquals(key, RecordKey.of(equivalent, Measure.TIMES));
    assertTrue(keeper.get(key).has(equivalent));
    assertEquals(List.of(key), keeper.keysMatching(equivalent));
    assertFalse(keeper.get(key).has(RecordComponent.of(SHIP, "freighter")));
  }
}
