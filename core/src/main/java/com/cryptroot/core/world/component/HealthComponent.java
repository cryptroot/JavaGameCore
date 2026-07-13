package com.cryptroot.core.world.component;

import com.cryptroot.core.event.Signal;
import com.cryptroot.core.event.Signal0;
import com.cryptroot.core.world.EntityComponent;

/**
 * Generic current/max hit-point tracker for any damageable entity (an enemy, a player unit, a
 * destructible prop, a tower's own health, …) — the framework form of the hand-rolled {@code hp}
 * field every damageable game object ends up needing.
 *
 * <p>Pure data/logic: this is <em>not</em> a {@link com.cryptroot.core.world.RenderComponent} or
 * {@link com.cryptroot.core.world.UpdateComponent} — hit points only change in response to explicit
 * {@link #damage}/{@link #heal} calls, never on a per-frame tick. Pair it with a {@link
 * WorldHealthBarComponent} (feed {@link #fraction()} into {@link
 * WorldHealthBarComponent#setFraction}) or a {@link TintFlashRenderComponent} (trigger {@link
 * TintFlashRenderComponent#flash} from {@link #onChanged()}) to visualise it — this class knows
 * about neither.
 *
 * <p>Once {@link #hp()} reaches zero the entity is permanently dead: {@link #damage} and {@link
 * #heal} both become no-ops (documented fail-soft behaviour — there is no revive path). {@link
 * #onDeath()} fires exactly once, on the call that brings {@link #hp()} to zero.
 */
public final class HealthComponent implements EntityComponent {

  private final int maxHp;
  private final Signal<HealthComponent> onChanged = new Signal<>();
  private final Signal0 onDeath = new Signal0();
  private int hp;

  /** Constructs a component starting at full health. */
  public HealthComponent(int maxHp) {
    if (maxHp <= 0) {
      throw new IllegalArgumentException("maxHp must be positive: " + maxHp);
    }
    this.maxHp = maxHp;
    this.hp = maxHp;
  }

  /** Current hit points, in {@code [0, maxHp]}. */
  public int hp() {
    return hp;
  }

  /** Maximum hit points this entity can have. */
  public int maxHp() {
    return maxHp;
  }

  /** Current hit points as a fraction of max, in {@code [0, 1]} — feeds a health-bar widget. */
  public float fraction() {
    return (float) hp / maxHp;
  }

  /** {@code true} until {@link #hp()} reaches zero; never becomes {@code true} again after. */
  public boolean isAlive() {
    return hp > 0;
  }

  /**
   * Applies {@code amount} damage, clamped so {@link #hp()} never drops below zero.
   *
   * <p>Fail-soft, documented: a zero amount or a call after death is a silent no-op rather than an
   * error, since callers (e.g. a projectile that already checked {@link #isAlive()}) routinely hit
   * this path. A negative amount is rejected — that indicates a caller bug (accidentally healing
   * through {@code damage()}), not a legitimate no-op.
   *
   * @throws IllegalArgumentException if {@code amount} is negative
   */
  public void damage(int amount) {
    if (amount < 0) {
      throw new IllegalArgumentException("amount must not be negative: " + amount);
    }
    if (amount == 0 || hp <= 0) return;
    hp = Math.max(0, hp - amount);
    onChanged.emit(this);
    if (hp == 0) onDeath.emit();
  }

  /**
   * Restores {@code amount} hit points, clamped so {@link #hp()} never exceeds {@link #maxHp()}.
   *
   * <p>Fail-soft, documented: a zero amount or a call after death is a silent no-op — death is
   * permanent, there is no revive-via-heal path. A negative amount is rejected as a caller bug.
   *
   * @throws IllegalArgumentException if {@code amount} is negative
   */
  public void heal(int amount) {
    if (amount < 0) {
      throw new IllegalArgumentException("amount must not be negative: " + amount);
    }
    if (amount == 0 || hp <= 0) return;
    hp = Math.min(maxHp, hp + amount);
    onChanged.emit(this);
  }

  /** Fires with {@code this} after every successful {@link #damage} or {@link #heal} call. */
  public Signal<HealthComponent> onChanged() {
    return onChanged;
  }

  /** Fires exactly once, on the {@link #damage} call that brings {@link #hp()} to zero. */
  public Signal0 onDeath() {
    return onDeath;
  }
}
