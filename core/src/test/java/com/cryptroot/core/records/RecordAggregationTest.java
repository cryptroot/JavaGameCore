package com.cryptroot.core.records;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class RecordAggregationTest {

  @Test
  void theFirstObservationAlwaysWinsOutright() {
    for (RecordAggregation aggregation : RecordAggregation.values()) {
      assertEquals(7, aggregation.combine(OptionalLong.empty(), 7), aggregation::name);
      assertEquals(0, aggregation.combine(OptionalLong.empty(), 0), aggregation::name);
      assertEquals(-3, aggregation.combine(OptionalLong.empty(), -3), aggregation::name);
    }
  }

  @Test
  void sumAccumulates() {
    assertEquals(12, RecordAggregation.SUM.combine(OptionalLong.of(5), 7));
  }

  @Test
  void maxKeepsTheLargest() {
    assertEquals(7, RecordAggregation.MAX.combine(OptionalLong.of(5), 7));
    assertEquals(5, RecordAggregation.MAX.combine(OptionalLong.of(5), 3));
  }

  @Test
  void minKeepsTheSmallest() {
    assertEquals(3, RecordAggregation.MIN.combine(OptionalLong.of(5), 3));
    assertEquals(5, RecordAggregation.MIN.combine(OptionalLong.of(5), 7));
  }

  @Test
  void lastOverwrites() {
    assertEquals(3, RecordAggregation.LAST.combine(OptionalLong.of(5), 3));
    assertEquals(7, RecordAggregation.LAST.combine(OptionalLong.of(5), 7));
  }

  @Test
  void onlySumRejectsNegativeObservations() {
    assertFalse(RecordAggregation.SUM.allowsNegativeObservations());
    assertTrue(RecordAggregation.MAX.allowsNegativeObservations());
    assertTrue(RecordAggregation.MIN.allowsNegativeObservations());
    assertTrue(RecordAggregation.LAST.allowsNegativeObservations());
  }

  @Test
  void onlySumTreatsZeroAsANoOp() {
    assertTrue(RecordAggregation.SUM.treatsZeroAsNoOp());
    assertFalse(RecordAggregation.MAX.treatsZeroAsNoOp());
    assertFalse(RecordAggregation.MIN.treatsZeroAsNoOp());
    assertFalse(RecordAggregation.LAST.treatsZeroAsNoOp());
  }

  @Test
  void onlySumHasAMeaningfulZeroToResetTo() {
    assertFalse(RecordAggregation.SUM.clearedByReset());
    assertTrue(RecordAggregation.MAX.clearedByReset());
    assertTrue(RecordAggregation.MIN.clearedByReset());
    assertTrue(RecordAggregation.LAST.clearedByReset());
  }

  @Test
  void definitionsDefaultToSum() {
    assertEquals(
        RecordAggregation.SUM,
        RecordDefinition.builder().components(Content.BLOOD).build().aggregation());
  }

  @Test
  void aggregationTakesNoPartInTheKey() {
    assertEquals(
        RecordDefinition.builder().components(Content.BLOOD).build().key(),
        RecordDefinition.builder()
            .components(Content.BLOOD)
            .aggregation(RecordAggregation.MAX)
            .build()
            .key());
  }
}
