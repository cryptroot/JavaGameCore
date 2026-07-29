package com.cryptroot.core.records.sample;

import com.cryptroot.core.records.RecordDefinition;
import com.cryptroot.core.records.RecordKeeper;
import com.cryptroot.core.records.RecordKey;
import java.util.Objects;

/**
 * The statically defined blood-bank record catalogue: it registers its records into a {@link
 * RecordKeeper} on construction and exposes the keys they were minted under.
 *
 * <p>This is the game-content half of the system — a catalogue object rather than a bag of
 * constants, because the keeper it defines into is injected, not global. Construct it once during
 * start-up, alongside the keeper, and pass it wherever a record key is needed:
 *
 * <pre>{@code
 * RecordKeeper keeper = new RecordKeeper();
 * BloodBankRecords bloodBank = new BloodBankRecords(keeper);
 * book.record(bloodBank.playerBankBloodAmount(), donatedHp);
 * }</pre>
 */
public final class BloodBankRecords {

  private final RecordKey playerBankBloodTimes;
  private final RecordKey playerBankBloodAmount;

  /**
   * Defines every blood-bank record into {@code keeper}.
   *
   * @throws NullPointerException if {@code keeper} is {@code null}
   * @throws IllegalStateException if these records are already defined in {@code keeper} — a
   *     catalogue is registered exactly once per keeper
   */
  public BloodBankRecords(RecordKeeper keeper) {
    Objects.requireNonNull(keeper, "keeper must not be null");
    this.playerBankBloodTimes =
        donation(
            keeper,
            Measure.TIMES,
            "Donated Blood Times",
            "Number of times actor has donated blood");
    this.playerBankBloodAmount =
        donation(
            keeper, Measure.AMOUNT, "Donated Blood Amount", "Cumulative HP donated at blood bank");
  }

  /** Key of "number of times the player donated blood at the bank". */
  public RecordKey playerBankBloodTimes() {
    return playerBankBloodTimes;
  }

  /** Key of "cumulative HP the player donated at the blood bank". */
  public RecordKey playerBankBloodAmount() {
    return playerBankBloodAmount;
  }

  /**
   * Defines one player blood-donation record differing only in how it is measured — the shared
   * shape of every record in this catalogue.
   */
  private static RecordKey donation(
      RecordKeeper keeper, Measure measure, String name, String description) {
    return keeper.define(
        RecordDefinition.builder()
            .components(Context.BANK, Content.BLOOD, Origin.PLAYER, measure)
            .name(name)
            .description(description)
            .build());
  }
}
