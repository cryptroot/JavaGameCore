package com.cryptroot.core.records;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RecordKeeperTest {

  /** A well-formed key nothing ever defines — the shape of a key restored from an older save. */
  private static final RecordKey UNDEFINED =
      RecordKey.of(RecordComponent.of(RecordDimension.of("skill"), "3"));

  private final RecordKeeper keeper = new RecordKeeper();

  private static RecordDefinition definition(String name, RecordComponent... components) {
    RecordDefinition.Builder builder = RecordDefinition.builder().components(components);
    if (name != null) {
      builder.name(name).description(name + " description");
    }
    return builder.build();
  }

  @Test
  void defineReturnsGeneratedKey() {
    RecordKey key = keeper.define(definition("Donated Blood Times", Context.BANK, Content.BLOOD));

    assertEquals(RecordKey.parse("content=blood|context=bank"), key);
  }

  @Test
  void defineStoresRetrievableDefinition() {
    RecordDefinition definition = definition("Donated Blood Times", Context.BANK, Content.BLOOD);

    RecordKey key = keeper.define(definition);

    assertTrue(keeper.isDefined(key));
    assertSame(definition, keeper.get(key));
    assertEquals(Optional.of(definition), keeper.find(key));
  }

  @Test
  void defineRejectsDuplicateKey() {
    keeper.define(definition("First", Context.BANK, Content.BLOOD));

    assertThrows(
        IllegalStateException.class,
        () -> keeper.define(definition("Second", Context.BANK, Content.BLOOD)));
  }

  @Test
  void defineRejectsDuplicateRegardlessOfComponentOrder() {
    keeper.define(definition("First", Context.BANK, Content.BLOOD, Origin.PLAYER));

    assertThrows(
        IllegalStateException.class,
        () -> keeper.define(definition("Second", Origin.PLAYER, Content.BLOOD, Context.BANK)));
  }

  @Test
  void firstDefinitionSurvivesARejectedDuplicate() {
    RecordKey key = keeper.define(definition("First", Context.BANK, Content.BLOOD));

    assertThrows(
        IllegalStateException.class,
        () -> keeper.define(definition("Second", Context.BANK, Content.BLOOD)));

    assertEquals("First", keeper.getName(key));
  }

  @Test
  void defineIfAbsentRegistersWhenAbsent() {
    RecordKey key = keeper.defineIfAbsent(definition("First", Context.BANK, Content.BLOOD));

    assertTrue(keeper.isDefined(key));
    assertEquals("First", keeper.getName(key));
  }

  @Test
  void defineIfAbsentKeepsTheExistingDefinition() {
    RecordKey key = keeper.define(definition("First", Context.BANK, Content.BLOOD));

    RecordKey again = keeper.defineIfAbsent(definition("Second", Context.BANK, Content.BLOOD));

    assertEquals(key, again);
    assertEquals("First", keeper.getName(key));
    assertEquals(1, keeper.definitions().size());
  }

  @Test
  void isDefinedIsFalseForUnknownKey() {
    assertFalse(keeper.isDefined(UNDEFINED));
  }

  @Test
  void getThrowsForUnknownKey() {
    assertThrows(IllegalArgumentException.class, () -> keeper.get(UNDEFINED));
  }

  @Test
  void findReturnsEmptyForUnknownKey() {
    assertEquals(Optional.empty(), keeper.find(UNDEFINED));
  }

  @Test
  void getNameReturnsName() {
    RecordKey key = keeper.define(definition("Donated Blood Times", Context.BANK, Content.BLOOD));

    assertEquals("Donated Blood Times", keeper.getName(key));
  }

  @Test
  void getNameThrowsWhenDefinitionHasNoName() {
    RecordKey key = keeper.define(definition(null, Context.BANK, Content.BLOOD));

    assertThrows(IllegalStateException.class, () -> keeper.getName(key));
  }

  @Test
  void getNameThrowsForUnknownKey() {
    assertThrows(IllegalArgumentException.class, () -> keeper.getName(UNDEFINED));
  }

  @Test
  void getDescriptionReturnsDescription() {
    RecordKey key = keeper.define(definition("Donated Blood Times", Context.BANK, Content.BLOOD));

    assertEquals("Donated Blood Times description", keeper.getDescription(key));
  }

  @Test
  void getDescriptionThrowsWhenDefinitionHasNoDescription() {
    RecordKey key = keeper.define(definition(null, Context.BANK, Content.BLOOD));

    assertThrows(IllegalStateException.class, () -> keeper.getDescription(key));
  }

  @Test
  void getDescriptionThrowsForUnknownKey() {
    assertThrows(IllegalArgumentException.class, () -> keeper.getDescription(UNDEFINED));
  }

  @Test
  void keysMatchingFiltersByComponent() {
    RecordKey bank = keeper.define(definition("Bank", Context.BANK, Content.BLOOD));
    RecordKey battle = keeper.define(definition("Battle", Context.BATTLE, Content.BLOOD));
    keeper.define(definition("Map water", Context.MAP, Content.WATER));

    assertEquals(List.of(battle), keeper.keysMatching(Context.BATTLE));
    assertEquals(List.of(bank, battle), keeper.keysMatching(Content.BLOOD));
    assertEquals(List.of(), keeper.keysMatching(Quality.SPOILED));
  }

  @Test
  void keysMatchingAcceptsAComposedQuery() {
    keeper.define(definition("Bank", Context.BANK, Content.BLOOD));
    RecordKey battle = keeper.define(definition("Battle", Context.BATTLE, Content.BLOOD));

    assertEquals(
        List.of(battle),
        keeper.keysMatching(
            RecordQuery.has(Content.BLOOD).and(RecordQuery.has(Context.BANK).negate())));
  }

  @Test
  void keysMatchingSkipsUnknownKeys() {
    RecordKey battle = keeper.define(definition("Battle", Context.BATTLE, Content.BLOOD));

    List<RecordKey> matching = keeper.keysMatching(Context.BATTLE, List.of(UNDEFINED, battle));

    assertEquals(List.of(battle), matching);
  }

  @Test
  void keysMatchingOverSuppliedKeysIgnoresUnrelatedRegistryEntries() {
    RecordKey firstBattle = keeper.define(definition("A", Context.BATTLE, Content.BLOOD));
    RecordKey secondBattle = keeper.define(definition("B", Context.BATTLE, Content.WATER));

    assertEquals(List.of(firstBattle), keeper.keysMatching(Context.BATTLE, List.of(firstBattle)));
    assertEquals(
        List.of(secondBattle, firstBattle),
        keeper.keysMatching(Context.BATTLE, List.of(secondBattle, firstBattle)));
  }

  @Test
  void matchesRequiresEveryQueriedComponent() {
    RecordKey key = keeper.define(definition("Bank", Context.BANK, Content.BLOOD, Origin.PLAYER));

    assertTrue(keeper.matches(key, RecordQuery.anything()));
    assertTrue(
        keeper.matches(
            key, RecordQuery.allOf(RecordQuery.has(Context.BANK), RecordQuery.has(Origin.PLAYER))));
    assertFalse(
        keeper.matches(
            key, RecordQuery.allOf(RecordQuery.has(Context.BANK), RecordQuery.has(Origin.ENEMY))));
  }

  @Test
  void matchesReturnsFalseForUnknownKey() {
    assertFalse(keeper.matches(UNDEFINED, RecordQuery.anything()));
    assertFalse(keeper.matches(UNDEFINED, RecordQuery.has(Context.BATTLE)));
  }

  @Test
  void definitionsViewIsUnmodifiable() {
    RecordDefinition definition = definition("Bank", Context.BANK, Content.BLOOD);
    RecordKey key = keeper.define(definition);

    Map<RecordKey, RecordDefinition> definitions = keeper.definitions();

    assertEquals(Map.of(key, definition), definitions);
    assertThrows(UnsupportedOperationException.class, () -> definitions.remove(key));
  }

  @Test
  void rejectsNullArguments() {
    assertThrows(NullPointerException.class, () -> keeper.define(null));
    assertThrows(NullPointerException.class, () -> keeper.defineIfAbsent(null));
    assertThrows(NullPointerException.class, () -> keeper.isDefined(null));
    assertThrows(NullPointerException.class, () -> keeper.get(null));
    assertThrows(NullPointerException.class, () -> keeper.find(null));
    assertThrows(NullPointerException.class, () -> keeper.getName(null));
    assertThrows(NullPointerException.class, () -> keeper.getDescription(null));
    assertThrows(NullPointerException.class, () -> keeper.matches(null, RecordQuery.anything()));
    assertThrows(NullPointerException.class, () -> keeper.matches(UNDEFINED, null));
    assertThrows(NullPointerException.class, () -> keeper.keysMatching((RecordQuery) null));
    assertThrows(NullPointerException.class, () -> keeper.keysMatching(Content.BLOOD, null));
  }
}
