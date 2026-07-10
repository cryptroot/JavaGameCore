package com.cryptroot.core.story;

import com.cryptroot.core.dialogue.DialogueGraph;
import com.cryptroot.core.dialogue.DialogueNode;
import com.cryptroot.core.dialogue.DialogueRunner;
import com.cryptroot.core.dialogue.DialogueView;
import com.cryptroot.core.dialogue.WaitMode;
import com.cryptroot.core.event.DisposableConnection;
import com.cryptroot.core.event.EventBus;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates story conversations: it owns the active {@link DialogueRunner}, binds it to a shared
 * {@link DialogueView}, and pauses/resumes conversations whose {@link DialogueNode.Action}
 * requirements are not yet met.
 *
 * <p>This director is generic over the game-context type {@code C}: a {@link
 * RequirementTracker}{@code <C>} maps requirement keys to predicates that inspect the live context.
 * The director itself never reads from {@code C} directly — it delegates entirely to the tracker
 * and the {@link StoryState} blackboard.
 *
 * <h3>Signal wiring — caller responsibility</h3>
 *
 * This director does <em>not</em> auto-subscribe to any game signals. The owning screen is
 * responsible for calling {@link #poll()} whenever the game state changes (clock advance, cash
 * change, etc.). Use {@link StoryPollBindings} as a convenience wrapper:
 *
 * <pre>{@code
 * StoryPollBindings polls = new StoryPollBindings()
 *     .bind(state.clock().onDayChanged, director::poll)
 *     .bind(state.onCashChanged,        director::poll);
 * // in onHide():
 * polls.dispose();
 * }</pre>
 *
 * <h3>Requirement resolution</h3>
 *
 * The director wires the {@link StoryState} requirement resolver to the {@link RequirementTracker},
 * so the runner's pre-check and the director's polling share one source of truth.
 *
 * <h3>Pause / resume</h3>
 *
 * <ul>
 *   <li><b>INLINE</b> awaits keep the widget open showing the action hint; the director records the
 *       awaited key and resumes the active runner when it becomes satisfied.
 *   <li><b>SUSPEND</b> awaits close the widget and park the runner; the director resumes it —
 *       possibly much later — when its requirement becomes true.
 * </ul>
 *
 * <h3>Replay guard</h3>
 *
 * {@link #play(DialogueGraph, String)} no-ops when the completion flag is already set in {@link
 * StoryState}, so a conversation never replays once seen.
 *
 * <p>Call {@link #dispose()} on screen hide to release all runner subscriptions.
 *
 * @param <C> the game-context type whose state predicates interrogate
 */
public final class StoryDirector<C> {

  private final C context;
  private final StoryState storyState;
  private final RequirementTracker<C> tracker;
  private final EventBus eventBus;
  private final DialogueView widget;

  /** The currently displayed runner, or {@code null} when idle/suspended. */
  private DialogueRunner active;

  /** Requirement key the active (INLINE) runner is waiting on, or {@code null}. */
  private String activeAwaitKey;

  /** Parked SUSPEND runners keyed by the requirement they await. */
  private final Map<String, DialogueRunner> suspended = new LinkedHashMap<>();

  /** Completion flag to write when a given runner ends. */
  private final Map<DialogueRunner, String> completionFlags = new LinkedHashMap<>();

  /** Director-owned signal connections per runner (await/ended). */
  private final Map<DialogueRunner, List<DisposableConnection>> runnerConns = new LinkedHashMap<>();

  /**
   * Constructs a director.
   *
   * @param context the live game context; passed to {@link RequirementTracker} predicates
   * @param storyState the persistent dialogue blackboard
   * @param tracker maps requirement keys to predicates over {@code context}
   * @param eventBus event bus forwarded to each {@link DialogueRunner}
   * @param widget the presentation target that renders conversations
   */
  public StoryDirector(
      C context,
      StoryState storyState,
      RequirementTracker<C> tracker,
      EventBus eventBus,
      DialogueView widget) {
    this.context = context;
    this.storyState = storyState;
    this.tracker = tracker;
    this.eventBus = eventBus;
    this.widget = widget;

    // Single source of truth: the runner's requirement pre-check delegates
    // to the same tracker the director polls.
    storyState.setRequirementResolver(key -> tracker.isSatisfied(key, context));
  }

  // -------------------------------------------------------------------------
  // Playback
  // -------------------------------------------------------------------------

  /**
   * Begins playing {@code graph}. No-ops if {@code completionFlag} is non-null and already set (the
   * conversation has already been seen).
   *
   * @param graph the conversation to play
   * @param completionFlag story flag written on completion (may be {@code null})
   */
  public void play(DialogueGraph graph, String completionFlag) {
    if (completionFlag != null && storyState.flag(completionFlag)) {
      return; // replay guard
    }
    DialogueRunner runner = new DialogueRunner(graph, storyState, eventBus);
    if (completionFlag != null) completionFlags.put(runner, completionFlag);

    List<DisposableConnection> rc = new ArrayList<>();
    rc.add(runner.onAwaitAction.connect(action -> handleAwait(runner, action)));
    rc.add(runner.onEnded.connect(() -> handleEnded(runner)));
    runnerConns.put(runner, rc);

    active = runner;
    widget.bind(runner); // connects presentation signals and starts the runner
  }

  /** Returns {@code true} while a conversation is on screen. */
  public boolean isActive() {
    return active != null;
  }

  /** Returns the {@link StoryState} blackboard this director writes flags into. */
  public StoryState storyState() {
    return storyState;
  }

  // -------------------------------------------------------------------------
  // Await handling
  // -------------------------------------------------------------------------

  private void handleAwait(DialogueRunner runner, DialogueNode.Action action) {
    if (action.waitMode() == WaitMode.SUSPEND) {
      widget.reset();
      suspended.put(action.requirementKey(), runner);
      if (active == runner) active = null;
    } else {
      activeAwaitKey = action.requirementKey();
    }
    poll();
  }

  // -------------------------------------------------------------------------
  // Polling / resume
  // -------------------------------------------------------------------------

  /**
   * Polls all parked and inline-waiting runners against the current game context, resuming any
   * whose requirement is now satisfied.
   *
   * <p>This method is {@code public} so the owning screen can call it directly from game-signal
   * handlers. Use {@link StoryPollBindings} for convenient wiring.
   */
  public void poll() {
    // Resume any parked SUSPEND runner whose requirement is now satisfied.
    List<String> ready = new ArrayList<>();
    for (Map.Entry<String, DialogueRunner> e : suspended.entrySet()) {
      if (tracker.isSatisfied(e.getKey(), context)) ready.add(e.getKey());
    }
    for (String key : ready) {
      DialogueRunner runner = suspended.remove(key);
      active = runner;
      widget.rebind(runner);
      runner.onRequirementSatisfied(key);
    }

    // Resume the active INLINE runner if its awaited key is satisfied.
    if (active != null && activeAwaitKey != null && tracker.isSatisfied(activeAwaitKey, context)) {
      String key = activeAwaitKey;
      activeAwaitKey = null;
      active.onRequirementSatisfied(key);
    }
  }

  // -------------------------------------------------------------------------
  // Completion
  // -------------------------------------------------------------------------

  private void handleEnded(DialogueRunner runner) {
    String flag = completionFlags.remove(runner);
    if (flag != null) storyState.setFlag(flag, true);

    List<DisposableConnection> rc = runnerConns.remove(runner);
    if (rc != null) for (DisposableConnection c : rc) c.disconnect();

    if (active == runner) {
      active = null;
      activeAwaitKey = null;
    }
  }

  // -------------------------------------------------------------------------
  // Lifecycle
  // -------------------------------------------------------------------------

  /**
   * Disconnects all runner subscriptions. Call from screen {@code onHide()}. Does not disconnect
   * game-signal subscriptions (those are managed by {@link StoryPollBindings} returned at
   * construction time).
   */
  public void dispose() {
    for (List<DisposableConnection> rc : runnerConns.values()) {
      for (DisposableConnection c : rc) c.disconnect();
    }
    runnerConns.clear();
    suspended.clear();
    completionFlags.clear();
    active = null;
    activeAwaitKey = null;
  }
}
