package com.cryptroot.core.story;

import com.cryptroot.core.event.DisposableConnection;
import com.cryptroot.core.event.Signal;
import com.cryptroot.core.event.Signal0;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Convenience wrapper that binds game signals to {@link StoryDirector#poll()} calls, freeing screen
 * code from managing individual connection objects.
 *
 * <h3>Usage</h3>
 *
 * <pre>{@code
 * StoryPollBindings polls = new StoryPollBindings()
 *     .bind(state.clock().onDayChanged,     director::poll)
 *     .bind(state.clock().onMinutesAdvanced, director::poll)
 *     .bind(state.onCashChanged,             director::poll)
 *     .bind(state.onReputationChanged,       director::poll);
 *
 * // in onHide():
 * polls.dispose();
 * }</pre>
 *
 * <p>Each {@code bind} overload returns {@code this} for chaining. {@link #dispose()} disconnects
 * all registered listeners in one call.
 */
public final class StoryPollBindings {

  private final List<DisposableConnection> conns = new ArrayList<>();

  /** Binds a {@link Signal0} (no payload) to {@code handler}. */
  public StoryPollBindings bind(Signal0 signal, Runnable handler) {
    Objects.requireNonNull(signal, "signal must not be null");
    Objects.requireNonNull(handler, "handler must not be null");
    conns.add(signal.connect(handler));
    return this;
  }

  /** Binds a {@link Signal}{@code <T>} (with payload) to {@code handler}, ignoring the value. */
  public <T> StoryPollBindings bind(Signal<T> signal, Runnable handler) {
    Objects.requireNonNull(signal, "signal must not be null");
    Objects.requireNonNull(handler, "handler must not be null");
    conns.add(signal.connect(ignored -> handler.run()));
    return this;
  }

  /** Disconnects all registered listeners. Call from screen {@code onHide()}. */
  public void dispose() {
    conns.forEach(DisposableConnection::disconnect);
    conns.clear();
  }
}
