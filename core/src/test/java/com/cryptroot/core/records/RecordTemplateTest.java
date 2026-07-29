package com.cryptroot.core.records;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RecordTemplateTest {

  private static final RecordDimension SKILL = RecordDimension.of("skill");

  private final RecordKeeper keeper = new RecordKeeper();

  private final RecordTemplate skillUses =
      RecordTemplate.builder()
          .components(Action.USED, Measure.TIMES)
          .parameter(SKILL)
          .name(skill -> "Skill " + skill + " Uses")
          .description(skill -> "Times skill " + skill + " was used")
          .build();

  @Test
  void memberKeyIncludesTheParameterAsAnOrdinaryComponent() {
    assertEquals("action=used|measure=times|skill=3", skillUses.key(3).id());
    assertEquals("action=used|measure=times|skill=railgun", skillUses.key("railgun").id());
  }

  @Test
  void numericAndStringFormsAgree() {
    assertEquals(skillUses.key("3"), skillUses.key(3));
  }

  @Test
  void defineRegistersTheMember() {
    RecordKey key = skillUses.define(keeper, 3);

    assertTrue(keeper.isDefined(key));
    assertEquals("Skill 3 Uses", keeper.getName(key));
    assertEquals("Times skill 3 was used", keeper.getDescription(key));
  }

  @Test
  void defineIsIdempotent() {
    RecordKey first = skillUses.define(keeper, 3);
    RecordKey second = skillUses.define(keeper, 3);

    assertEquals(first, second);
    assertEquals(1, keeper.definitions().size());
  }

  @Test
  void aMintedKeyAnswersComponentQueries() {
    RecordKey skillThree = skillUses.define(keeper, 3);
    RecordBookComponent book = new RecordBookComponent(keeper);
    book.record(skillThree, 2);

    // The whole point: unlike a hand-rolled "skill_3" string, this participates in every query.
    assertEquals(2, book.total(Action.USED));
    assertEquals(2, book.total(RecordQuery.anything()));
    assertEquals(List.of(skillThree), keeper.keysMatching(Action.USED));
    assertTrue(keeper.get(skillThree).has(RecordComponent.of(SKILL, "3")));
  }

  @Test
  void aMintedKeyIsResettable() {
    RecordKey skillThree = skillUses.define(keeper, 3);
    RecordBookComponent book = new RecordBookComponent(keeper);
    book.record(skillThree, 2);

    book.reset(skillUses.family());

    assertEquals(0, book.value(skillThree));
  }

  @Test
  void familyMatchesEveryMemberAndNothingElse() {
    RecordKey skillThree = skillUses.define(keeper, 3);
    RecordKey skillSeven = skillUses.define(keeper, 7);
    RecordKey unrelated =
        keeper.define(RecordDefinition.builder().components(Action.EATEN, Measure.TIMES).build());
    // Shares the fixed components but carries no parameter, so it is not a family member.
    RecordKey nearMiss =
        keeper.define(RecordDefinition.builder().components(Action.USED, Measure.TIMES).build());

    assertEquals(List.of(skillThree, skillSeven), keeper.keysMatching(skillUses.family()));
    assertFalse(keeper.matches(unrelated, skillUses.family()));
    assertFalse(keeper.matches(nearMiss, skillUses.family()));
  }

  @Test
  void totalOverAFamilySumsEveryMember() {
    RecordBookComponent book = new RecordBookComponent(keeper);
    book.record(skillUses.define(keeper, 3), 2);
    book.record(skillUses.define(keeper, 7), 5);

    assertEquals(7, book.total(skillUses.family()));
    assertEquals(2, book.value(skillUses.key(3)));
  }

  @Test
  void aggregationCarriesToEveryMember() {
    RecordTemplate bestTime =
        RecordTemplate.builder()
            .components(Measure.AMOUNT)
            .parameter(RecordDimension.of("track"))
            .aggregation(RecordAggregation.MIN)
            .build();
    RecordKey circuit = bestTime.define(keeper, "circuit");
    RecordBookComponent book = new RecordBookComponent(keeper);

    book.record(circuit, 90);
    book.record(circuit, 120);

    assertEquals(RecordAggregation.MIN, keeper.get(circuit).aggregation());
    assertEquals(90, book.value(circuit));
  }

  @Test
  void namingIsOptional() {
    RecordTemplate bare = RecordTemplate.builder().components(Action.USED).parameter(SKILL).build();
    RecordKey key = bare.define(keeper, 3);

    assertThrows(IllegalStateException.class, () -> keeper.getName(key));
    assertThrows(IllegalStateException.class, () -> keeper.getDescription(key));
  }

  @Test
  void exposesItsFixedComponentsAndParameter() {
    assertEquals(SKILL, skillUses.parameter());
    assertEquals(
        List.of(RecordComponent.canonical(Action.USED), RecordComponent.canonical(Measure.TIMES)),
        List.copyOf(skillUses.components().values()));
  }

  @Test
  void rejectsAParameterThatIsAlsoFixed() {
    assertThrows(
        IllegalArgumentException.class,
        () -> RecordTemplate.builder().components(Action.USED).parameter(Action.DIMENSION).build());
  }

  @Test
  void rejectsATemplateWithNoParameter() {
    assertThrows(
        IllegalStateException.class,
        () -> RecordTemplate.builder().components(Action.USED).build());
  }

  @Test
  void rejectsTwoFixedComponentsOnOneDimension() {
    assertThrows(
        IllegalArgumentException.class,
        () -> RecordTemplate.builder().component(Action.USED).component(Action.EATEN));
  }

  @Test
  void rejectsNonCanonicalOrNegativeParameterValues() {
    assertThrows(IllegalArgumentException.class, () -> skillUses.key("Railgun"));
    assertThrows(IllegalArgumentException.class, () -> skillUses.key("rail gun"));
    assertThrows(IllegalArgumentException.class, () -> skillUses.key(-1));
    assertThrows(IllegalArgumentException.class, () -> skillUses.define(keeper, -1));
  }

  @Test
  void rejectsNullArguments() {
    assertThrows(NullPointerException.class, () -> skillUses.key((String) null));
    assertThrows(NullPointerException.class, () -> skillUses.definition(null));
    assertThrows(NullPointerException.class, () -> skillUses.define(null, "3"));
    assertThrows(NullPointerException.class, () -> RecordTemplate.builder().component(null));
    assertThrows(NullPointerException.class, () -> RecordTemplate.builder().parameter(null));
    assertThrows(NullPointerException.class, () -> RecordTemplate.builder().name(null));
    assertThrows(NullPointerException.class, () -> RecordTemplate.builder().description(null));
    assertThrows(NullPointerException.class, () -> RecordTemplate.builder().aggregation(null));
  }
}
