package com.cryptroot.core.records;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class RecordDefinitionTest {

  private static final RecordKey BLOOD_KEY = RecordKey.of(Content.BLOOD);

  private static RecordDefinition.Builder bankDonation() {
    return RecordDefinition.builder()
        .components(Context.BANK, Content.BLOOD, Origin.PLAYER, Measure.TIMES);
  }

  private static SortedMap<RecordDimension, RecordComponent> sorted(
      Map<RecordDimension, RecordComponent> components) {
    return new TreeMap<>(components);
  }

  @Test
  void derivesKeyFromComponents() {
    assertEquals(
        "content=blood|context=bank|measure=times|origin=player",
        bankDonation().build().key().id());
  }

  @Test
  void builderIsOrderInsensitive() {
    RecordDefinition forward =
        RecordDefinition.builder()
            .component(Context.BANK)
            .component(Content.BLOOD)
            .component(Origin.PLAYER)
            .component(Measure.TIMES)
            .build();
    RecordDefinition reversed =
        RecordDefinition.builder()
            .component(Measure.TIMES)
            .component(Origin.PLAYER)
            .component(Content.BLOOD)
            .component(Context.BANK)
            .build();

    assertEquals(forward.key(), reversed.key());
    assertEquals(forward.components(), reversed.components());
    assertEquals(forward, reversed);
  }

  @Test
  void componentsIterateInDimensionOrder() {
    assertEquals(
        List.of(Content.DIMENSION, Context.DIMENSION, Measure.DIMENSION, Origin.DIMENSION),
        List.copyOf(bankDonation().build().components().keySet()));
  }

  @Test
  void keepsNameAndDescription() {
    RecordDefinition definition =
        bankDonation().name("Donated Blood Times").description("How often").build();

    assertEquals("Donated Blood Times", definition.name());
    assertEquals("How often", definition.description());
  }

  @Test
  void nameAndDescriptionAreOptional() {
    RecordDefinition definition = bankDonation().build();

    assertNull(definition.name());
    assertNull(definition.description());
  }

  @Test
  void nameAndDescriptionTakeNoPartInTheKey() {
    assertEquals(
        bankDonation().build().key(),
        bankDonation().name("Anything").description("At all").build().key());
  }

  @Test
  void rejectsTwoComponentsForSameDimension() {
    assertThrows(
        IllegalArgumentException.class,
        () -> RecordDefinition.builder().component(Content.BLOOD).component(Content.WATER));
  }

  @Test
  void rejectsBuildWithNoComponents() {
    assertThrows(IllegalArgumentException.class, () -> RecordDefinition.builder().build());
  }

  @Test
  void rejectsNullComponent() {
    assertThrows(NullPointerException.class, () -> RecordDefinition.builder().component(null));
    assertThrows(
        NullPointerException.class,
        () -> RecordDefinition.builder().components((RecordComponent[]) null));
  }

  @Test
  void rejectsBlankNameOrDescription() {
    assertThrows(IllegalArgumentException.class, () -> bankDonation().name(" "));
    assertThrows(IllegalArgumentException.class, () -> bankDonation().description(""));
    assertThrows(NullPointerException.class, () -> bankDonation().name(null));
  }

  @Test
  void componentsAreStoredCanonicalised() {
    RecordDefinition definition = bankDonation().build();

    assertEquals(
        Optional.of(RecordComponent.of(Content.DIMENSION, "blood")),
        definition.component(Content.DIMENSION));
  }

  @Test
  void enumAndUndeclaredComponentsAreInterchangeable() {
    RecordDefinition fromEnums = bankDonation().build();
    RecordDefinition fromValues =
        RecordDefinition.builder()
            .components(
                RecordComponent.of(Context.DIMENSION, "bank"),
                RecordComponent.of(Content.DIMENSION, "blood"),
                RecordComponent.of(Origin.DIMENSION, "player"),
                RecordComponent.of(Measure.DIMENSION, "times"))
            .build();

    assertEquals(fromEnums.key(), fromValues.key());
    assertEquals(fromEnums, fromValues);
    assertTrue(fromValues.has(Context.BANK));
    assertTrue(fromEnums.has(RecordComponent.of(Context.DIMENSION, "bank")));
  }

  @Test
  void componentsMapIsDefensivelyCopied() {
    SortedMap<RecordDimension, RecordComponent> components = new TreeMap<>();
    components.put(Content.DIMENSION, Content.BLOOD);
    RecordDefinition definition =
        new RecordDefinition(BLOOD_KEY, null, null, RecordAggregation.SUM, components);

    components.put(Quality.DIMENSION, Quality.SPOILED);

    assertEquals(
        Map.of(Content.DIMENSION, RecordComponent.of(Content.DIMENSION, "blood")),
        definition.components());
  }

  @Test
  void componentsMapIsUnmodifiable() {
    RecordDefinition definition = bankDonation().build();

    assertThrows(
        UnsupportedOperationException.class,
        () -> definition.components().put(Quality.DIMENSION, Quality.SPOILED));
  }

  @Test
  void rejectsKeyThatDoesNotMatchComponents() {
    SortedMap<RecordDimension, RecordComponent> components =
        sorted(Map.of(Content.DIMENSION, Content.BLOOD));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new RecordDefinition(
                RecordKey.of(Content.WATER), null, null, RecordAggregation.SUM, components));
  }

  @Test
  void rejectsComponentFiledUnderTheWrongDimension() {
    SortedMap<RecordDimension, RecordComponent> components =
        sorted(Map.of(Quality.DIMENSION, Content.BLOOD));

    assertThrows(
        IllegalArgumentException.class,
        () -> new RecordDefinition(BLOOD_KEY, null, null, RecordAggregation.SUM, components));
  }

  @Test
  void rejectsNullOrEmptyConstructorArguments() {
    assertThrows(
        NullPointerException.class,
        () -> new RecordDefinition(null, null, null, RecordAggregation.SUM, new TreeMap<>()));
    assertThrows(
        NullPointerException.class,
        () -> new RecordDefinition(BLOOD_KEY, null, null, RecordAggregation.SUM, null));
    assertThrows(
        NullPointerException.class,
        () -> new RecordDefinition(BLOOD_KEY, null, null, null, new TreeMap<>()));
    assertThrows(
        IllegalArgumentException.class,
        () -> new RecordDefinition(BLOOD_KEY, null, null, RecordAggregation.SUM, new TreeMap<>()));
  }

  @Test
  void componentLookupByDimension() {
    RecordDefinition definition = bankDonation().build();

    assertEquals(Optional.empty(), definition.component(Quality.DIMENSION));
    assertTrue(definition.has(Context.BANK));
    assertFalse(definition.has(Context.BATTLE));
    assertFalse(definition.has(Quality.NORMAL));
  }

  @Test
  void matchesRequiresEveryQueriedComponent() {
    RecordDefinition definition = bankDonation().build();

    assertTrue(definition.matches(RecordQuery.anything()));
    assertTrue(definition.matches(RecordQuery.has(Content.BLOOD)));
    assertTrue(
        definition.matches(
            RecordQuery.allOf(RecordQuery.has(Content.BLOOD), RecordQuery.has(Context.BANK))));
    assertFalse(
        definition.matches(
            RecordQuery.allOf(RecordQuery.has(Content.BLOOD), RecordQuery.has(Context.BATTLE))));
    assertFalse(definition.matches(RecordQuery.has(Quality.NORMAL)));
    assertThrows(NullPointerException.class, () -> definition.matches(null));
  }
}
