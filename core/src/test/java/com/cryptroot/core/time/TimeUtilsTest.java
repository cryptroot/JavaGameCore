package com.cryptroot.core.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class TimeUtilsTest {

  // ---- Timer -------------------------------------------------------------

  @Test
  void timerFiresExactlyOnceOnExpiry() {
    Timer t = new Timer(1f);
    assertFalse(t.update(0.5f));
    assertFalse(t.isExpired());
    assertEquals(0.5f, t.remaining(), 1e-4f);
    assertTrue(t.update(0.6f), "crosses zero this tick");
    assertTrue(t.isExpired());
    assertFalse(t.update(1f), "does not fire again");
  }

  @Test
  void timerRestartReArms() {
    Timer t = new Timer(0.1f);
    assertTrue(t.update(0.2f));
    t.restart(0.5f);
    assertFalse(t.isExpired());
    assertFalse(t.update(0.4f));
    assertTrue(t.update(0.2f));
  }

  // ---- Cadence -----------------------------------------------------------

  @Test
  void cadenceFiresImmediatelyThenPerInterval() {
    Cadence c = new Cadence(1f);
    assertTrue(c.consumeReady(), "fresh cadence is ready at once");
    assertFalse(c.consumeReady());
    c.update(1f);
    assertTrue(c.consumeReady());
    assertFalse(c.consumeReady());
  }

  @Test
  void cadenceCatchesUpOverLargeDelta() {
    Cadence c = new Cadence(1f);
    assertTrue(c.consumeReady()); // initial
    c.update(3.5f);
    int fired = 0;
    while (c.consumeReady()) fired++;
    assertEquals(3, fired, "3 whole intervals accrued in 3.5s");
  }

  // ---- Motion ------------------------------------------------------------

  @Test
  void moveTowardsSnapsWhenWithinReach() {
    Vector2 p = new Vector2(0f, 0f);
    assertTrue(Motion.moveTowards(p, 3f, 4f, 10f), "5 units <= maxDelta 10");
    assertEquals(3f, p.x, 1e-4f);
    assertEquals(4f, p.y, 1e-4f);
  }

  @Test
  void moveTowardsPartialStepNoOvershoot() {
    Vector2 p = new Vector2(0f, 0f);
    assertFalse(Motion.moveTowards(p, 10f, 0f, 4f));
    assertEquals(4f, p.x, 1e-4f);
    assertEquals(0f, p.y, 1e-4f);
  }

  // ---- Sequence ----------------------------------------------------------

  @Test
  void sequenceRunsStepsInOrderWithTiming() {
    List<String> log = new ArrayList<>();
    Sequence s =
        Sequence.builder().run(() -> log.add("a")).waitSeconds(1f).run(() -> log.add("b")).build();

    s.update(0f); // run "a" immediately, then block on wait
    assertEquals(List.of("a"), log);
    assertFalse(s.isDone());
    s.update(0.5f);
    assertEquals(List.of("a"), log);
    s.update(0.6f); // wait elapses, "b" chains same tick
    assertEquals(List.of("a", "b"), log);
    assertTrue(s.isDone());
  }

  @Test
  void sequenceWaitUntilGatesOnPredicate() {
    AtomicBoolean gate = new AtomicBoolean(false);
    AtomicInteger done = new AtomicInteger();
    Sequence s = Sequence.builder().waitUntil(gate::get).run(done::incrementAndGet).build();

    s.update(1f);
    assertEquals(0, done.get());
    gate.set(true);
    s.update(0f);
    assertEquals(1, done.get());
    assertTrue(s.isDone());
  }

  @Test
  void sequenceCancelHalts() {
    AtomicInteger done = new AtomicInteger();
    Sequence s = Sequence.builder().waitSeconds(1f).run(done::incrementAndGet).build();
    s.cancel();
    assertTrue(s.isDone());
    s.update(5f);
    assertEquals(0, done.get());
  }

  // ---- Scheduler ---------------------------------------------------------

  @Test
  void schedulerTicksAndDropsFinished() {
    Scheduler sch = new Scheduler();
    AtomicInteger hits = new AtomicInteger();
    sch.submit(Sequence.builder().run(hits::incrementAndGet).build());
    sch.submit(Sequence.builder().waitSeconds(1f).run(hits::incrementAndGet).build());

    sch.update(0f); // first finishes immediately
    assertEquals(1, hits.get());
    assertEquals(1, sch.active(), "the waiting one remains");
    sch.update(1f);
    assertEquals(2, hits.get());
    assertEquals(0, sch.active());
  }

  // ---- Components --------------------------------------------------------

  @Test
  void sequenceComponentTicksItsSequence() {
    AtomicInteger hits = new AtomicInteger();
    SequenceComponent c =
        new SequenceComponent(Sequence.builder().run(hits::incrementAndGet).build());
    assertFalse(c.isDone());
    c.update(0f);
    assertEquals(1, hits.get());
    assertTrue(c.isDone());
  }

  @Test
  void timerComponentEmitsOnceOnExpiry() {
    TimerComponent c = new TimerComponent();
    AtomicInteger fired = new AtomicInteger();
    c.onExpire().connect(fired::incrementAndGet);

    c.update(1f); // not triggered yet -> nothing
    assertEquals(0, fired.get());

    c.trigger(0.5f);
    c.update(0.5f);
    assertEquals(1, fired.get());
    c.update(1f);
    assertEquals(1, fired.get(), "fires once per trigger");

    c.trigger(0.2f); // re-arm
    c.update(0.2f);
    assertEquals(2, fired.get());
  }
}
