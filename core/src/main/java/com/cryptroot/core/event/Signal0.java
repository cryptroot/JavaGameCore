package com.cryptroot.core.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A zero-argument, multi-listener delegate.
 *
 * <p>Use this instead of {@code Signal<Void>} when no payload is needed — it avoids the awkward
 * {@code emit(null)} / {@code accept(null)} idiom. Typical uses: button clicked, screen shown,
 * dialog dismissed, turn ended.
 *
 * <p>Listeners are called in the order they were connected. Emission snapshots the listener list
 * before iterating, so a listener may safely add or remove other listeners during dispatch without
 * causing a {@link java.util.ConcurrentModificationException}.
 *
 * <p>To disconnect a listener, store the {@link DisposableConnection} returned by {@link
 * #connect(Runnable)} — lambda listeners do not override {@code equals()}, so this is the only way
 * to identify a specific listener for removal.
 */
public final class Signal0 {

  private final List<Runnable> listeners = new ArrayList<>();

  /**
   * Adds {@code listener} to this signal.
   *
   * @return a {@link DisposableConnection} that removes {@code listener} when {@link
   *     DisposableConnection#disconnect()} is called. Store this handle if disconnection may ever
   *     be needed.
   */
  public DisposableConnection connect(Runnable listener) {
    Objects.requireNonNull(listener, "listener must not be null");
    listeners.add(listener);
    return () -> listeners.remove(listener);
  }

  /**
   * Calls all currently connected listeners. The listener list is snapshotted before iteration; any
   * additions or removals made during emission take effect on the next call to {@code emit}.
   */
  public void emit() {
    for (Runnable listener : List.copyOf(listeners)) {
      listener.run();
    }
  }

  /**
   * Removes all connected listeners. Equivalent to calling {@link
   * DisposableConnection#disconnect()} on every active connection.
   */
  public void clear() {
    listeners.clear();
  }
}
