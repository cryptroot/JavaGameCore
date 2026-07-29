package com.cryptroot.core.records.sample;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class RecordsDemoTest {

  @Test
  void mainRunsHeadlessly() {
    ByteArrayOutputStream captured = new ByteArrayOutputStream();
    PrintStream original = System.out;

    try {
      System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
      RecordsDemo.main(new String[0]);
    } finally {
      System.setOut(original);
    }

    String output = captured.toString(StandardCharsets.UTF_8);

    // Anchored on display names, not key strings: this test should not be collateral damage every
    // time the key format is touched.
    assertTrue(output.contains("Donated Blood Times"), output);
    assertTrue(output.contains("Donated Blood Amount"), output);
    // A templated record reports a real name rather than reading as an unexplained auto-generated
    // key.
    assertTrue(output.contains("Skill 3 Uses"), output);
    assertTrue(output.contains("Biggest Drain"), output);
    assertTrue(output.contains("after resetting battle records"), output);
  }
}
