package com.cryptroot.core.records.sample;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptroot.core.records.RecordDefinition;
import com.cryptroot.core.records.RecordKeeper;
import com.cryptroot.core.records.RecordKey;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BloodBankRecordsTest {

  private final RecordKeeper keeper = new RecordKeeper();
  private final BloodBankRecords records = new BloodBankRecords(keeper);

  @Test
  void definesBothDonationRecords() {
    assertTrue(keeper.isDefined(records.playerBankBloodTimes()));
    assertTrue(keeper.isDefined(records.playerBankBloodAmount()));
    assertEquals(2, keeper.definitions().size());
    assertNotEquals(records.playerBankBloodTimes(), records.playerBankBloodAmount());
  }

  @Test
  void timesKeyIsDimensionQualifiedAndOrderedByDimension() {
    assertEquals(
        "content=blood|context=bank|measure=times|origin=player",
        records.playerBankBloodTimes().id());
  }

  @Test
  void amountKeyDiffersOnlyInItsMeasure() {
    assertEquals(
        "content=blood|context=bank|measure=amount|origin=player",
        records.playerBankBloodAmount().id());
  }

  @Test
  void namesAndDescriptionsAreRegistered() {
    assertEquals("Donated Blood Times", keeper.getName(records.playerBankBloodTimes()));
    assertEquals(
        "Number of times actor has donated blood",
        keeper.getDescription(records.playerBankBloodTimes()));
    assertEquals("Donated Blood Amount", keeper.getName(records.playerBankBloodAmount()));
    assertEquals(
        "Cumulative HP donated at blood bank",
        keeper.getDescription(records.playerBankBloodAmount()));
  }

  @Test
  void bothRecordsShareBloodBankPlayerComponents() {
    for (RecordKey key : List.of(records.playerBankBloodTimes(), records.playerBankBloodAmount())) {
      RecordDefinition definition = keeper.get(key);

      assertTrue(definition.has(Context.BANK));
      assertTrue(definition.has(Content.BLOOD));
      assertTrue(definition.has(Origin.PLAYER));
      assertEquals(Optional.empty(), definition.component(Quality.DIMENSION));
    }
    assertTrue(keeper.get(records.playerBankBloodTimes()).has(Measure.TIMES));
    assertTrue(keeper.get(records.playerBankBloodAmount()).has(Measure.AMOUNT));
  }

  @Test
  void bothRecordsAreFoundByComponentQuery() {
    assertEquals(
        List.of(records.playerBankBloodTimes(), records.playerBankBloodAmount()),
        keeper.keysMatching(Context.BANK));
  }

  @Test
  void secondCatalogAgainstSameKeeperThrows() {
    assertThrows(IllegalStateException.class, () -> new BloodBankRecords(keeper));
  }

  @Test
  void rejectsNullKeeper() {
    assertThrows(NullPointerException.class, () -> new BloodBankRecords(null));
  }
}
