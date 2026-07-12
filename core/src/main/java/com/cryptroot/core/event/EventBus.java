package com.cryptroot.core.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A lightweight typed event bus for game-wide broadcast communication.
 *
 * <p>Events are dispatched to listeners by their <em>exact runtime type</em>: {@link
 * #publish(Object)} uses {@link Object#getClass()} as the lookup key. A listener subscribed to
 * {@code Foo.class} will <em>not</em> receive events whose runtime type is a subtype of {@code
 * Foo}. Event record types must therefore not be subclassed; document this constraint on each event
 * class.
 *
 * <p>Use {@code EventBus} when the publisher and subscriber do not share a direct reference — for
 * example, a screen publishing a {@code BattleStartRequested} event that a game-system owned by
 * {@link com.cryptroot.core.MyDemoGame} consumes. For local widget-to-screen communication, prefer
 * {@link Signal0} or {@link Signal}.
 *
 * <p>Emission snapshots the subscriber list for a given type before iterating, so a listener may
 * safely subscribe or unsubscribe other listeners during dispatch without causing a {@link
 * java.util.ConcurrentModificationException}.
 *
 * <p>This class holds no native LibGDX resources and does not need to be disposed.
 */
public final class EventBus {

  private final Map<Class<?>, List<Consumer<?>>> subscriptions = new HashMap<>();

  /**
   * Subscribes {@code listener} to events of exactly type {@code T}.
   *
   * @return a {@link DisposableConnection} that removes {@code listener} when {@link
   *     DisposableConnection#disconnect()} is called. Store this handle if disconnection may ever
   *     be needed.
   */
  public <T> DisposableConnection subscribe(Class<T> type, Consumer<T> listener) {
    Objects.requireNonNull(type, "type must not be null");
    Objects.requireNonNull(listener, "listener must not be null");
    subscriptions.computeIfAbsent(type, k -> new ArrayList<>()).add(listener);
    return () -> {
      List<Consumer<?>> list = subscriptions.get(type);
      if (list != null) {
        list.remove(listener);
      }
    };
  }

  /**
   * Publishes {@code event} to all listeners subscribed to {@code event.getClass()}. The
   * subscription list is snapshotted before iteration.
   *
   * <p>Dispatch is by exact runtime type only — see the class-level note on subtype matching.
   */
  @SuppressWarnings("unchecked")
  public <T> void publish(T event) {
    Objects.requireNonNull(event, "event must not be null");
    List<Consumer<?>> list = subscriptions.get(event.getClass());
    if (list == null || list.isEmpty()) return;
    // Safe: subscribe() ensures the stored Consumer<T> was registered for exactly T.
    for (Consumer<?> consumer : List.copyOf(list)) {
      ((Consumer<T>) consumer).accept(event);
    }
  }
}
