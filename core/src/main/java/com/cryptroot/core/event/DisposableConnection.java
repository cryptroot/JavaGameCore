package com.cryptroot.core.event;

/**
 * Handle returned by {@link Signal#connect(java.util.function.Consumer)}, {@link
 * Signal0#connect(Runnable)}, and {@link EventBus#subscribe(Class, java.util.function.Consumer)}.
 *
 * <p>Call {@link #disconnect()} to remove the associated listener. If you do not need to disconnect
 * before the owning signal or bus is garbage-collected, you may discard this handle — the listener
 * will remain connected until {@link Signal#clear()} / {@link Signal0#clear()} or the bus is
 * collected.
 *
 * <p><b>Disconnect-by-reference:</b> lambda listeners do not override {@code equals()}, so this
 * handle is the only way to identify and remove a specific listener. Store it whenever
 * disconnection may be needed:
 *
 * <pre>{@code
 * DisposableConnection conn = mySignal.connect(this::onEvent);
 * // later, when no longer interested:
 * conn.disconnect();
 * }</pre>
 */
@FunctionalInterface
public interface DisposableConnection {
  void disconnect();
}
