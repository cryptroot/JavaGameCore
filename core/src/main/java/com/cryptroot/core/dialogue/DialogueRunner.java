package com.cryptroot.core.dialogue;

import com.cryptroot.core.event.EventBus;
import com.cryptroot.core.event.Signal;
import com.cryptroot.core.event.Signal0;

/**
 * Pure-Java state machine that walks a {@link DialogueGraph}.
 *
 * <p>The runner owns a single position — {@link #currentNodeId()} — and emits signals describing
 * what the presentation layer should show. It has no LibGDX dependency and is fully unit-testable.
 *
 * <h3>Signals</h3>
 *
 * <ul>
 *   <li>{@link #onLine} — show a spoken line.
 *   <li>{@link #onChoices} — present branching options.
 *   <li>{@link #onAwaitAction} — the conversation is paused for a gameplay action.
 *   <li>{@link #onEnded} — the conversation finished.
 * </ul>
 *
 * <h3>Driving the runner</h3>
 *
 * <ul>
 *   <li>{@link #start()} — enter the graph's start node.
 *   <li>{@link #advance()} — from a {@link DialogueNode.Line}, go to its successor.
 *   <li>{@link #select(int)} — resolve a {@link DialogueNode.Choice}.
 *   <li>{@link #onRequirementSatisfied(String)} — resume a parked action.
 * </ul>
 *
 * <p>{@link DialogueNode.Event} nodes fire their {@link DialogueEvent} and pass through
 * automatically; {@link DialogueNode.Action} nodes pass through when the requirement is already
 * satisfied, otherwise they park and emit {@link #onAwaitAction}.
 */
public final class DialogueRunner {

  private final DialogueGraph graph;
  private final DialogueBlackboard blackboard;
  private final EventBus eventBus;

  /** Current node id, or {@code null} before {@link #start()} / after end. */
  private String currentNodeId;

  /** The action this runner is parked on, or {@code null} if not waiting. */
  private DialogueNode.Action pendingAction;

  public final Signal<DialogueNode.Line> onLine = new Signal<>();
  public final Signal<DialogueNode.Choice> onChoices = new Signal<>();
  public final Signal<DialogueNode.Action> onAwaitAction = new Signal<>();
  public final Signal0 onEnded = new Signal0();

  public DialogueRunner(DialogueGraph graph, DialogueBlackboard blackboard, EventBus eventBus) {
    this.graph = graph;
    this.blackboard = blackboard;
    this.eventBus = eventBus;
  }

  // -------------------------------------------------------------------------
  // Queries
  // -------------------------------------------------------------------------

  public DialogueGraph graph() {
    return graph;
  }

  public String currentNodeId() {
    return currentNodeId;
  }

  public boolean isAwaitingAction() {
    return pendingAction != null;
  }

  /**
   * Evaluates each option of {@code choice} against this runner's blackboard, returning a parallel
   * array of enabled flags for the presentation layer.
   */
  public boolean[] enabledFlags(DialogueNode.Choice choice) {
    boolean[] flags = new boolean[choice.options().size()];
    for (int i = 0; i < flags.length; i++) {
      flags[i] = choice.options().get(i).isEnabled(blackboard);
    }
    return flags;
  }

  // -------------------------------------------------------------------------
  // Driving
  // -------------------------------------------------------------------------

  /** Enters the graph at its start node. */
  public void start() {
    pendingAction = null;
    enter(graph.startId());
  }

  /**
   * Re-enters the graph at {@code nodeId}. Used to reconstruct a parked runner (forward-compatible
   * with future persistence).
   */
  public void resumeAt(String nodeId) {
    pendingAction = null;
    enter(nodeId);
  }

  /** From a {@link DialogueNode.Line}, advances to its successor. */
  public void advance() {
    if (currentNodeId == null || pendingAction != null) return;
    if (graph.node(currentNodeId) instanceof DialogueNode.Line line) {
      enter(line.nextId());
    }
  }

  /** Resolves the current {@link DialogueNode.Choice} by option index. */
  public void select(int optionIndex) {
    if (currentNodeId == null) return;
    if (!(graph.node(currentNodeId) instanceof DialogueNode.Choice choice)) return;
    if (optionIndex < 0 || optionIndex >= choice.options().size()) return;

    Option option = choice.options().get(optionIndex);
    if (!option.isEnabled(blackboard)) return; // ignore disabled options

    if (option.onSelect() != null) option.onSelect().apply(blackboard); // effect first
    enter(option.nextId());
  }

  /**
   * Resumes a parked action when its {@code key} becomes satisfied. No-op if the runner is not
   * waiting on exactly this key.
   */
  public void onRequirementSatisfied(String key) {
    if (pendingAction == null) return;
    if (!pendingAction.requirementKey().equals(key)) return;
    DialogueNode.Action action = pendingAction;
    pendingAction = null;
    enter(action.nextId());
  }

  // -------------------------------------------------------------------------
  // Internal traversal
  // -------------------------------------------------------------------------

  private void enter(String nodeId) {
    currentNodeId = nodeId;
    DialogueNode node = graph.node(nodeId);
    switch (node) {
      case DialogueNode.Line line -> onLine.emit(line);
      case DialogueNode.Choice ch -> onChoices.emit(ch);
      case DialogueNode.Event ev -> {
        if (ev.event() != null) ev.event().fire(eventBus);
        enter(ev.nextId());
      }
      case DialogueNode.Action ac -> {
        if (blackboard.requirementSatisfied(ac.requirementKey())) {
          enter(ac.nextId());
        } else {
          pendingAction = ac;
          onAwaitAction.emit(ac);
        }
      }
      case DialogueNode.End ignored -> {
        currentNodeId = null;
        onEnded.emit();
      }
    }
  }
}
