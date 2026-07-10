package com.cryptroot.core.time;

import com.cryptroot.core.world.UpdateComponent;

/**
 * Attaches a {@link Sequence} to a world entity so it is ticked by the standard {@code
 * UpdateSystem} along with every other {@link UpdateComponent} — the natural home for entity-scoped
 * routines like a hit-flash or a floating message that live and die with their entity.
 */
public final class SequenceComponent implements UpdateComponent {

  private Sequence sequence;

  public SequenceComponent(Sequence sequence) {
    this.sequence = sequence;
  }

  /** Replaces the running sequence (e.g. restart a flash on a fresh hit). */
  public void setSequence(Sequence sequence) {
    this.sequence = sequence;
  }

  @Override
  public void update(float delta) {
    if (sequence != null) sequence.update(delta);
  }

  public boolean isDone() {
    return sequence == null || sequence.isDone();
  }
}
