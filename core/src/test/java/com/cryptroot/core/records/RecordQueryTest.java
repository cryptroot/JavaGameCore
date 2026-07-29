package com.cryptroot.core.records;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RecordQueryTest {

  private static final RecordDefinition BANK_BLOOD =
      RecordDefinition.builder()
          .components(Context.BANK, Content.BLOOD, Origin.PLAYER, Measure.TIMES)
          .name("Donated Blood Times")
          .build();

  private static final RecordDefinition MAP_WATER =
      RecordDefinition.builder().components(Context.MAP, Content.WATER).build();

  @Test
  void hasMatchesAnExactComponent() {
    assertTrue(RecordQuery.has(Content.BLOOD).test(BANK_BLOOD));
    assertFalse(RecordQuery.has(Content.WATER).test(BANK_BLOOD));
    assertFalse(RecordQuery.has(Quality.NORMAL).test(BANK_BLOOD));
  }

  @Test
  void hasComparesByValueNotIdentity() {
    assertTrue(RecordQuery.has(RecordComponent.of(Content.DIMENSION, "blood")).test(BANK_BLOOD));
  }

  @Test
  void hasDimensionMatchesAnyValueOnTheAxis() {
    assertTrue(RecordQuery.hasDimension(Measure.DIMENSION).test(BANK_BLOOD));
    assertFalse(RecordQuery.hasDimension(Measure.DIMENSION).test(MAP_WATER));
    assertFalse(RecordQuery.hasDimension(Quality.DIMENSION).test(BANK_BLOOD));
  }

  @Test
  void anythingAndNothingAreTheIdentities() {
    assertTrue(RecordQuery.anything().test(BANK_BLOOD));
    assertTrue(RecordQuery.anything().test(MAP_WATER));
    assertFalse(RecordQuery.nothing().test(BANK_BLOOD));
    assertEquals(RecordQuery.anything(), RecordQuery.allOf());
    assertEquals(RecordQuery.nothing(), RecordQuery.anyOf());
  }

  @Test
  void allOfRequiresEveryQuery() {
    assertTrue(
        RecordQuery.allOf(RecordQuery.has(Content.BLOOD), RecordQuery.has(Context.BANK))
            .test(BANK_BLOOD));
    assertFalse(
        RecordQuery.allOf(RecordQuery.has(Content.BLOOD), RecordQuery.has(Context.MAP))
            .test(BANK_BLOOD));
  }

  @Test
  void anyOfRequiresOneQuery() {
    assertTrue(
        RecordQuery.anyOf(RecordQuery.has(Content.WATER), RecordQuery.has(Context.BANK))
            .test(BANK_BLOOD));
    assertFalse(
        RecordQuery.anyOf(RecordQuery.has(Content.WATER), RecordQuery.has(Context.MAP))
            .test(BANK_BLOOD));
  }

  @Test
  void notInverts() {
    assertFalse(RecordQuery.not(RecordQuery.has(Content.BLOOD)).test(BANK_BLOOD));
    assertTrue(RecordQuery.not(RecordQuery.has(Content.WATER)).test(BANK_BLOOD));
  }

  @Test
  void combinatorsCompose() {
    RecordQuery bloodButNotAtTheBank =
        RecordQuery.has(Content.BLOOD).and(RecordQuery.has(Context.BANK).negate());

    assertFalse(bloodButNotAtTheBank.test(BANK_BLOOD));
    assertTrue(
        bloodButNotAtTheBank.test(
            RecordDefinition.builder().components(Context.BATTLE, Content.BLOOD).build()));
  }

  @Test
  void orIsNotExclusive() {
    RecordQuery either = RecordQuery.has(Content.BLOOD).or(RecordQuery.has(Content.WATER));

    assertTrue(either.test(BANK_BLOOD));
    assertTrue(either.test(MAP_WATER));
  }

  @Test
  void whereIsTheEscapeHatch() {
    assertTrue(RecordQuery.where(definition -> definition.name() != null).test(BANK_BLOOD));
    assertFalse(RecordQuery.where(definition -> definition.name() != null).test(MAP_WATER));
  }

  @Test
  void equalQueriesAreEqual() {
    assertEquals(RecordQuery.has(Content.BLOOD), RecordQuery.has(Content.BLOOD));
    assertEquals(
        RecordQuery.has(Content.BLOOD),
        RecordQuery.has(RecordComponent.of(Content.DIMENSION, "blood")));
    assertEquals(
        RecordQuery.has(Content.BLOOD).and(RecordQuery.has(Context.BANK)),
        RecordQuery.allOf(RecordQuery.has(Content.BLOOD), RecordQuery.has(Context.BANK)));
  }

  @Test
  void rejectsNullArguments() {
    assertThrows(NullPointerException.class, () -> RecordQuery.has(null));
    assertThrows(NullPointerException.class, () -> RecordQuery.hasDimension(null));
    assertThrows(NullPointerException.class, () -> RecordQuery.not(null));
    assertThrows(NullPointerException.class, () -> RecordQuery.where(null));
    assertThrows(NullPointerException.class, () -> RecordQuery.allOf((RecordQuery[]) null));
    assertThrows(NullPointerException.class, () -> RecordQuery.anyOf((RecordQuery[]) null));
    assertThrows(NullPointerException.class, () -> RecordQuery.has(Content.BLOOD).test(null));
  }
}
