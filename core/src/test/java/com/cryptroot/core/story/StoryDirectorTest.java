package com.cryptroot.core.story;

import static org.junit.jupiter.api.Assertions.*;

import com.cryptroot.core.dialogue.DialogueGraph;
import com.cryptroot.core.dialogue.DialogueRunner;
import com.cryptroot.core.dialogue.DialogueView;
import com.cryptroot.core.dialogue.WaitMode;
import com.cryptroot.core.event.EventBus;
import com.cryptroot.core.event.Signal;
import org.junit.jupiter.api.Test;

/**
 * Pure-Java tests for {@link StoryDirector} pause/resume and replay behaviour.
 *
 * <p>Uses a minimal {@code TestContext} fixture with a single {@code Signal<Integer>} as the
 * "advance" trigger so the tests remain completely independent of any game-module classes (no
 * {@code GameState}, no {@code GameClock}).
 */
class StoryDirectorTest {

  /** Minimal game context for testing requirement predicates. */
  private static final class TestContext {
    int day = 1;
    final Signal<Integer> onDayChanged = new Signal<>();

    void nextDay() {
      day++;
      onDayChanged.emit(day);
    }
  }

  /** Minimal {@link DialogueView} that records calls and starts bound runners. */
  private static final class FakeView implements DialogueView {
    DialogueRunner bound;
    int resetCount;

    @Override
    public void bind(DialogueRunner r) {
      bound = r;
      r.start();
    }

    @Override
    public void rebind(DialogueRunner r) {
      bound = r;
    }

    @Override
    public void reset() {
      resetCount++;
    }
  }

  private StoryDirector<TestContext> buildDirector(
      TestContext ctx, RequirementTracker<TestContext> tracker, FakeView view) {
    StoryState storyState = new StoryState();
    StoryDirector<TestContext> director =
        new StoryDirector<>(ctx, storyState, tracker, new EventBus(), view);
    // Wire poll explicitly (caller's responsibility in new design).
    new StoryPollBindings().bind(ctx.onDayChanged, director::poll);
    return director;
  }

  private RequirementTracker<TestContext> dayTracker() {
    return new RequirementTracker<TestContext>().register("reach_day_2", c -> c.day >= 2);
  }

  // -------------------------------------------------------------------------
  // Tests
  // -------------------------------------------------------------------------

  @Test
  void suspendActionParksWidgetThenResumesNextDay() {
    TestContext ctx = new TestContext();
    FakeView view = new FakeView();
    StoryDirector<TestContext> director = buildDirector(ctx, dayTracker(), view);

    // Also wire poll manually so the signal-driven resume path is exercised.
    ctx.onDayChanged.connect(d -> director.poll());

    DialogueGraph graph =
        DialogueGraph.builder()
            .action("gate", "reach_day_2", "come back tomorrow", WaitMode.SUSPEND, false, "end")
            .end("end")
            .build();

    director.play(graph, "done");

    // Day 1: requirement not met → parked, widget reset, flag not set yet.
    assertEquals(1, view.resetCount);
    assertFalse(director.storyState().flag("done"));
    assertFalse(director.isActive());

    ctx.nextDay(); // fires onDayChanged → poll() → resume → end
    assertTrue(director.storyState().flag("done"));
  }

  @Test
  void replayGuardSkipsAlreadySeenConversation() {
    TestContext ctx = new TestContext();
    FakeView view = new FakeView();

    StoryState storyState = new StoryState();
    storyState.setFlag("done", true);
    StoryDirector<TestContext> director =
        new StoryDirector<>(ctx, storyState, dayTracker(), new EventBus(), view);

    DialogueGraph graph = DialogueGraph.builder().end("end").build();

    director.play(graph, "done");
    assertNull(view.bound); // never bound — replay guard fired
    assertFalse(director.isActive());
  }

  @Test
  void linearConversationEndsImmediatelyAndFlags() {
    TestContext ctx = new TestContext();
    FakeView view = new FakeView();

    StoryState storyState = new StoryState();
    StoryDirector<TestContext> director =
        new StoryDirector<>(ctx, storyState, new RequirementTracker<>(), new EventBus(), view);

    DialogueGraph graph = DialogueGraph.builder().end("end").build();

    director.play(graph, "intro_done");
    assertTrue(storyState.flag("intro_done"));
    assertFalse(director.isActive());
  }

  @Test
  void inlineActionDoesNotResetWidgetWhileWaiting() {
    TestContext ctx = new TestContext(); // day 1 — requirement not yet met
    FakeView view = new FakeView();
    StoryState story = new StoryState();
    RequirementTracker<TestContext> tracker = dayTracker();
    StoryDirector<TestContext> director =
        new StoryDirector<>(ctx, story, tracker, new EventBus(), view);
    ctx.onDayChanged.connect(d -> director.poll());

    DialogueGraph graph =
        DialogueGraph.builder()
            .action("gate", "reach_day_2", "wait", WaitMode.INLINE, false, "end")
            .end("end")
            .build();

    director.play(graph, "done");

    // INLINE: widget stays open, not reset.
    assertEquals(0, view.resetCount);
    assertFalse(story.flag("done"));

    ctx.nextDay(); // requirement met → resume → end
    assertTrue(story.flag("done"));
  }

  @Test
  void storyStateAccessibleViaDirector() {
    TestContext ctx = new TestContext();
    FakeView view = new FakeView();
    StoryState state = new StoryState();
    state.setFlag("pre_set", true);
    StoryDirector<TestContext> director =
        new StoryDirector<>(ctx, state, new RequirementTracker<>(), new EventBus(), view);

    assertTrue(director.storyState().flag("pre_set"));
  }
}
