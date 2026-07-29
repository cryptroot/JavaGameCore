package com.cryptroot.core.records;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RecordKeyTest {

  @Test
  void keyIsDimensionQualifiedPairsOrderedByDimension() {
    assertEquals(
        "content=blood|context=bank|measure=times|origin=player",
        RecordKey.of(Context.BANK, Content.BLOOD, Origin.PLAYER, Measure.TIMES).id());
  }

  @Test
  void relatedRecordsShareAPrefix() {
    RecordKey times = RecordKey.of(Context.BANK, Content.BLOOD, Origin.PLAYER, Measure.TIMES);
    RecordKey amount = RecordKey.of(Context.BANK, Content.BLOOD, Origin.PLAYER, Measure.AMOUNT);

    // Ordering by dimension rather than by value: changing the measure no longer reshuffles the
    // key.
    assertEquals("content=blood|context=bank|measure=", commonPrefix(times.id(), amount.id()));
  }

  @Test
  void keyIsIndependentOfComponentOrder() {
    RecordKey forward = RecordKey.of(Context.BANK, Content.BLOOD, Origin.PLAYER, Measure.TIMES);
    RecordKey reversed = RecordKey.of(Measure.TIMES, Origin.PLAYER, Content.BLOOD, Context.BANK);
    RecordKey shuffled = RecordKey.of(Origin.PLAYER, Measure.TIMES, Context.BANK, Content.BLOOD);

    assertEquals(forward, reversed);
    assertEquals(forward, shuffled);
  }

  @Test
  void componentsRoundTripThroughTheKey() {
    List<List<RecordComponent>> cases =
        List.of(
            List.of(Content.BLOOD),
            List.of(Content.BLOOD, Measure.TIMES),
            List.of(Context.BANK, Content.BLOOD, Origin.PLAYER, Measure.TIMES),
            List.of(RecordComponent.of(RecordDimension.of("starfarer", "ship"), "interceptor")),
            List.of(
                RecordComponent.of(RecordDimension.of("a.b-c"), "d.e-f"),
                RecordComponent.of(RecordDimension.of("ns", "x"), "0")));

    for (List<RecordComponent> components : cases) {
      RecordKey key = RecordKey.of(components);

      assertEquals(key, RecordKey.parse(key.id()));
      assertEquals(RecordKey.of(components).components(), RecordKey.parse(key.id()).components());
      assertEquals(components.size(), key.components().size());
      for (RecordComponent component : components) {
        assertEquals(
            RecordComponent.canonical(component), key.components().get(component.dimension()));
      }
    }
  }

  @Test
  void theSameValueOnTwoAxesDoesNotCollide() {
    RecordDimension content = RecordDimension.of("content");
    RecordDimension source = RecordDimension.of("source");

    RecordKey asContent = RecordKey.of(RecordComponent.of(content, "gold"), Measure.TIMES);
    RecordKey asSource = RecordKey.of(RecordComponent.of(source, "gold"), Measure.TIMES);

    assertNotEquals(asContent, asSource);
    assertEquals("content=gold|measure=times", asContent.id());
    assertEquals("measure=times|source=gold", asSource.id());
  }

  @Test
  void singleComponentKeyIsOnePair() {
    assertEquals("content=blood", RecordKey.of(Content.BLOOD).id());
  }

  @Test
  void collectionAndVarargsFormsAgree() {
    assertEquals(
        RecordKey.of(Content.WATER, Quality.SPOILED),
        RecordKey.of(List.of(Content.WATER, Quality.SPOILED)));
  }

  @Test
  void keyReadsAsItsId() {
    RecordKey key = RecordKey.of(Content.BLOOD);

    assertEquals(key.id(), key.toString());
  }

  @Test
  void keysOrderByDimension() {
    assertEquals(
        List.of(
            RecordKey.of(Content.BLOOD), RecordKey.of(Content.WATER), RecordKey.of(Measure.TIMES)),
        List.of(
                RecordKey.of(Measure.TIMES),
                RecordKey.of(Content.WATER),
                RecordKey.of(Content.BLOOD))
            .stream()
            .sorted()
            .toList());
  }

  @Test
  void undeclaredComponentsNeedNoEnum() {
    RecordDimension action = RecordDimension.of("action");

    assertEquals(
        "action=donated|context=bank",
        RecordKey.of(RecordComponent.of(action, "donated"), Context.BANK).id());
  }

  @Test
  void rejectsEmptyComponents() {
    assertThrows(IllegalArgumentException.class, () -> RecordKey.of(List.of()));
    assertThrows(IllegalArgumentException.class, () -> RecordKey.of(new RecordComponent[0]));
  }

  @Test
  void rejectsNullComponents() {
    assertThrows(NullPointerException.class, () -> RecordKey.of((List<RecordComponent>) null));
    assertThrows(NullPointerException.class, () -> RecordKey.of((RecordComponent[]) null));
  }

  @Test
  void rejectsNullComponentElement() {
    assertThrows(
        NullPointerException.class, () -> RecordKey.of(Arrays.asList(Content.BLOOD, null)));
    assertThrows(NullPointerException.class, () -> RecordKey.of(Content.BLOOD, null));
  }

  @Test
  void rejectsTwoComponentsOnOneDimension() {
    assertThrows(IllegalArgumentException.class, () -> RecordKey.of(Content.BLOOD, Content.WATER));
  }

  @Test
  void parseRejectsMalformedKeys() {
    for (String malformed :
        List.of(
            "",
            "blood",
            "content=blood|water",
            "content=blood|",
            "|content=blood",
            "=blood",
            "content=",
            "Content=blood",
            "content=BLOOD",
            "content=blood|content=water")) {
      assertThrows(
          IllegalArgumentException.class,
          () -> RecordKey.parse(malformed),
          () -> "expected \"" + malformed + "\" to be rejected");
    }
  }

  @Test
  void parseRejectsAKeyOrderedByAnythingButDimension() {
    assertThrows(
        IllegalArgumentException.class, () -> RecordKey.parse("context=bank|content=blood"));
  }

  @Test
  void parseRejectsNull() {
    assertThrows(NullPointerException.class, () -> RecordKey.parse(null));
    assertThrows(NullPointerException.class, () -> new RecordKey(null));
  }

  @Test
  void componentsViewIsUnmodifiable() {
    Map<RecordDimension, RecordComponent> components = RecordKey.of(Content.BLOOD).components();

    assertThrows(
        UnsupportedOperationException.class,
        () -> components.put(Measure.DIMENSION, Measure.TIMES));
  }

  private static String commonPrefix(String left, String right) {
    int shared = 0;
    while (shared < Math.min(left.length(), right.length())
        && left.charAt(shared) == right.charAt(shared)) {
      shared++;
    }
    return left.substring(0, shared);
  }
}
