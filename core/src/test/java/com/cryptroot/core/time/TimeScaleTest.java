package com.cryptroot.core.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TimeScaleTest {

  @Test
  void defaultsToIdentity() {
    TimeScale timeScale = new TimeScale();
    assertEquals(1f, timeScale.scale());
    assertFalse(timeScale.isPaused());
    assertEquals(0.5f, timeScale.apply(0.5f));
  }

  @Test
  void scaleMultipliesDelta() {
    TimeScale timeScale = new TimeScale();
    timeScale.setScale(2f);
    assertEquals(1f, timeScale.apply(0.5f));
  }

  @Test
  void rejectsNegativeScale() {
    TimeScale timeScale = new TimeScale();
    assertThrows(IllegalArgumentException.class, () -> timeScale.setScale(-1f));
  }

  @Test
  void pausedAppliesZeroRegardlessOfScale() {
    TimeScale timeScale = new TimeScale();
    timeScale.setScale(2f);
    timeScale.setPaused(true);

    assertEquals(0f, timeScale.apply(1f));
    assertEquals(2f, timeScale.scale(), "pausing does not reset the stored scale");
  }

  @Test
  void togglePauseFlipsState() {
    TimeScale timeScale = new TimeScale();
    assertFalse(timeScale.isPaused());

    timeScale.togglePause();
    assertTrue(timeScale.isPaused());

    timeScale.togglePause();
    assertFalse(timeScale.isPaused());
  }
}
