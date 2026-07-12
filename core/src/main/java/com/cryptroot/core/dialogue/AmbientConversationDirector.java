package com.cryptroot.core.dialogue;

import com.cryptroot.core.event.DisposableConnection;
import com.cryptroot.core.event.EventBus;
import com.cryptroot.core.event.Signal0;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Drives <em>ambient</em> (optional, repeatable) conversations that sit outside the main story flow
 * — for example a character barking <em>"Click on Laura to chat"</em> mid-task and offering a short
 * repeatable exchange when the player chooses to interact.
 *
 * <h3>How it differs from {@link com.cryptroot.core.dialogue.DialogueRunner}-driven story playback
 * </h3>
 *
 * <ul>
 *   <li><b>Non-forcing</b> — the conversation only opens when the player clicks the supplied {@link
 *       Signal0} click source; gameplay is never blocked.
 *   <li><b>Repeatable</b> — there is no completion flag. When a conversation ends, the prompt
 *       reappears so the player may chat again, until the barker is {@linkplain #disarm()
 *       disarmed}.
 *   <li><b>Non-modal presentation</b> — it expects a {@link DialogueView} backed by a non-modal
 *       widget, so clicks elsewhere pass through to the game.
 * </ul>
 *
 * <h3>Lifecycle</h3>
 *
 * <ol>
 *   <li>A gameplay system decides an interaction is available and calls {@link #arm(Signal0,
 *       DialogueGraph, String)} — the prompt is shown and the click source is wired.
 *   <li>The player clicks the source → the prompt hides and the graph plays through the {@link
 *       DialogueView}.
 *   <li>The conversation ends → the prompt reappears (still armed).
 *   <li>The opportunity passes → the gameplay system calls {@link #disarm()}.
 * </ol>
 *
 * <p>Holds no LibGDX resources; uses only core dialogue/event types so it can be unit-tested
 * without a graphics context. Call {@link #dispose()} on screen hide.
 */
public final class AmbientConversationDirector {

  private final DialogueView view;
  private final DialogueBlackboard blackboard;
  private final EventBus eventBus;
  private final InteractionPrompt prompt;

  /** Graph offered while armed, or {@code null} when disarmed. */
  private DialogueGraph graph;

  private String promptText;

  private boolean armed;
  private boolean conversing;

  private DisposableConnection clickConn;
  private final List<DisposableConnection> runnerConns = new ArrayList<>();

  public AmbientConversationDirector(
      DialogueView view,
      DialogueBlackboard blackboard,
      EventBus eventBus,
      InteractionPrompt prompt) {
    this.view = Objects.requireNonNull(view, "view must not be null");
    this.blackboard = Objects.requireNonNull(blackboard, "blackboard must not be null");
    this.eventBus = Objects.requireNonNull(eventBus, "eventBus must not be null");
    this.prompt = prompt != null ? prompt : InteractionPrompt.NONE;
  }

  // -------------------------------------------------------------------------
  // Arming
  // -------------------------------------------------------------------------

  /**
   * Offers {@code graph} as an optional conversation: shows {@code promptText} and plays the graph
   * whenever {@code clickSource} fires. Replaces any conversation that was previously armed
   * (closing it if mid-flight).
   *
   * @param clickSource fired when the player clicks the interaction target
   * @param graph the (repeatable) conversation to play on click
   * @param promptText the on-screen hint, e.g. {@code "Click on Laura to chat"}
   */
  public void arm(Signal0 clickSource, DialogueGraph graph, String promptText) {
    Objects.requireNonNull(clickSource, "clickSource must not be null");
    Objects.requireNonNull(graph, "graph must not be null");
    Objects.requireNonNull(promptText, "promptText must not be null");
    disarm();
    this.graph = graph;
    this.promptText = promptText;
    this.armed = true;
    this.clickConn = clickSource.connect(this::onClicked);
    prompt.show(promptText);
  }

  /**
   * Removes the offer: hides the prompt, disconnects the click source, and closes any in-progress
   * conversation.
   */
  public void disarm() {
    armed = false;
    graph = null;
    promptText = null;

    if (clickConn != null) {
      clickConn.disconnect();
      clickConn = null;
    }
    disconnectRunner();
    prompt.hide();

    if (conversing) {
      view.reset();
      conversing = false;
    }
  }

  // -------------------------------------------------------------------------
  // Interaction
  // -------------------------------------------------------------------------

  private void onClicked() {
    if (!armed || conversing) return; // ignore clicks while already chatting
    conversing = true;
    prompt.hide();

    DialogueRunner runner = new DialogueRunner(graph, blackboard, eventBus);
    runnerConns.add(runner.onEnded.connect(this::onConversationEnded));
    view.bind(runner); // non-modal widget connects signals and starts the runner
  }

  private void onConversationEnded() {
    disconnectRunner();
    conversing = false;
    // Repeatable: while still armed, re-show the prompt for another chat.
    if (armed) prompt.show(promptText);
  }

  // -------------------------------------------------------------------------
  // Queries / lifecycle
  // -------------------------------------------------------------------------

  /** {@code true} while a conversation is being offered (prompt may be visible). */
  public boolean isArmed() {
    return armed;
  }

  /** {@code true} while an ambient conversation is currently on screen. */
  public boolean isConversing() {
    return conversing;
  }

  /** Disarms and releases all subscriptions. */
  public void dispose() {
    disarm();
  }

  private void disconnectRunner() {
    for (DisposableConnection c : runnerConns) c.disconnect();
    runnerConns.clear();
  }
}
