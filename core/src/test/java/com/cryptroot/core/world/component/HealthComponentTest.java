package com.cryptroot.core.world.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class HealthComponentTest {

  @Test
  void rejectsNonPositiveMaxHp() {
    assertThrows(IllegalArgumentException.class, () -> new HealthComponent(0));
    assertThrows(IllegalArgumentException.class, () -> new HealthComponent(-1));
  }

  @Test
  void startsAtFullHealth() {
    HealthComponent health = new HealthComponent(100);
    assertEquals(100, health.hp());
    assertEquals(100, health.maxHp());
    assertEquals(1f, health.fraction());
    assertTrue(health.isAlive());
  }

  @Test
  void damageReducesHpAndEmitsOnChanged() {
    HealthComponent health = new HealthComponent(100);
    AtomicInteger changes = new AtomicInteger();
    health.onChanged().connect(h -> changes.incrementAndGet());

    health.damage(34);

    assertEquals(66, health.hp());
    assertEquals(0.66f, health.fraction(), 1e-6f);
    assertEquals(1, changes.get());
    assertTrue(health.isAlive());
  }

  @Test
  void damageClampsAtZeroAndFiresOnDeathExactlyOnce() {
    HealthComponent health = new HealthComponent(50);
    AtomicInteger deaths = new AtomicInteger();
    health.onDeath().connect(deaths::incrementAndGet);

    health.damage(1000);

    assertEquals(0, health.hp());
    assertFalse(health.isAlive());
    assertEquals(1, deaths.get());
  }

  @Test
  void damageAfterDeathIsNoOp() {
    HealthComponent health = new HealthComponent(10);
    health.damage(10);
    AtomicInteger changes = new AtomicInteger();
    AtomicInteger deaths = new AtomicInteger();
    health.onChanged().connect(h -> changes.incrementAndGet());
    health.onDeath().connect(deaths::incrementAndGet);

    health.damage(5);

    assertEquals(0, health.hp());
    assertEquals(0, changes.get());
    assertEquals(0, deaths.get());
  }

  @Test
  void damageRejectsNegativeAmount() {
    HealthComponent health = new HealthComponent(10);
    assertThrows(IllegalArgumentException.class, () -> health.damage(-1));
  }

  @Test
  void zeroDamageIsNoOpButDoesNotThrow() {
    HealthComponent health = new HealthComponent(10);
    AtomicInteger changes = new AtomicInteger();
    health.onChanged().connect(h -> changes.incrementAndGet());

    health.damage(0);

    assertEquals(10, health.hp());
    assertEquals(0, changes.get());
  }

  @Test
  void healRestoresHpClampedAtMax() {
    HealthComponent health = new HealthComponent(100);
    health.damage(80);

    health.heal(1000);

    assertEquals(100, health.hp());
  }

  @Test
  void healAfterDeathIsNoOp() {
    HealthComponent health = new HealthComponent(10);
    health.damage(10);

    health.heal(5);

    assertEquals(0, health.hp());
    assertFalse(health.isAlive());
  }

  @Test
  void healRejectsNegativeAmount() {
    HealthComponent health = new HealthComponent(10);
    assertThrows(IllegalArgumentException.class, () -> health.heal(-1));
  }
}
