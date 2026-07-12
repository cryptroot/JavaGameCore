package com.cryptroot.core.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A typed, multi-listener delegate.
 *
 * <p>Listeners are called in the order they were connected. Emission snapshots the listener list
 * before iterating, so a listener may safely add or remove other listeners during dispatch without
 * causing a {@link java.util.ConcurrentModificationException}.
 *
 * <p>To disconnect a listener, store the {@link DisposableConnection} returned by {@link
 * #connect(Consumer)} — lambda listeners do not override {@code equals()}, so this is the only way
 * to identify a specific listener for removal.
 *
 * @param <T> the payload type carried by each emission
 */
public final class Signal<T> {

  private final List<Consumer<T>> listeners = new ArrayList<>();

  /**
   * Adds {@code listener} to this signal.
   *
   * @return a {@link DisposableConnection} that removes {@code listener} when {@link
   *     DisposableConnection#disconnect()} is called. Store this handle if disconnection may ever
   *     be needed.
   */
  public DisposableConnection connect(Consumer<T> listener) {
    Objects.requireNonNull(listener, "listener must not be null");
    listeners.add(listener);
    return () -> listeners.remove(listener);
  }

  /**
   * Calls all currently connected listeners with {@code payload}. The listener list is snapshotted
   * before iteration; any additions or removals made during emission take effect on the next call
   * to {@code emit}.
   */
  public void emit(T payload) {
    for (Consumer<T> listener : List.copyOf(listeners)) {
      listener.accept(payload);
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
