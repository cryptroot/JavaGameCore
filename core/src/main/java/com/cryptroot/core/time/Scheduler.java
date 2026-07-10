package com.cryptroot.core.time;

import java.util.ArrayList;
import java.util.List;

/**
 * Ticks a set of {@link Sequence}s not bound to a world entity — e.g. a day/wave resolve loop that
 * outlives any single unit. A game instantiates one and holds it on its {@code GameContext}
 * subclass (there are no static singletons in {@code core}), then calls {@link #update(float)} once
 * per frame. Sequences that finish are dropped automatically.
 */
public final class Scheduler {

  private final List<Sequence> active = new ArrayList<>();

  /** Registers {@code s} to be ticked; returns it for convenience. */
  public Sequence submit(Sequence s) {
    active.add(s);
    return s;
  }

  /** Ticks every active sequence and removes the finished ones. */
  public void update(float delta) {
    for (int i = active.size() - 1; i >= 0; i--) {
      Sequence s = active.get(i);
      s.update(delta);
      if (s.isDone()) active.remove(i);
    }
  }

  /** Cancels and forgets all sequences. */
  public void clear() {
    active.clear();
  }

  /** Number of sequences still running. */
  public int active() {
    return active.size();
  }
}
