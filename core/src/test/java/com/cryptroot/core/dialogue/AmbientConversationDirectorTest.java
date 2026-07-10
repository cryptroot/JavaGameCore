package com.cryptroot.core.dialogue;

import static org.junit.jupiter.api.Assertions.*;

import com.cryptroot.core.event.EventBus;
import com.cryptroot.core.event.Signal0;
import org.junit.jupiter.api.Test;

/**
 * Pure-Java tests for {@link AmbientConversationDirector}. A {@link FakeView} stands in for the
 * non-modal {@code ConversationWidget} (starting the runner on bind, exposing it so tests can
 * advance it), and a {@link FakePrompt} records show/hide calls.
 */
class AmbientConversationDirectorTest {

  /** Drives the runner like the real widget and exposes it for advancing. */
  private static final class FakeView implements DialogueView {
    DialogueRunner bound;
    int bindCount;
    int resetCount;

    @Override
    public void bind(DialogueRunner r) {
      bound = r;
      bindCount++;
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

  /** Records prompt visibility transitions. */
  private static final class FakePrompt implements InteractionPrompt {
    boolean visible;
    String text;

    @Override
    public void show(String t) {
      visible = true;
      text = t;
    }

    @Override
    public void hide() {
      visible = false;
    }
  }

  private DialogueGraph oneLineGraph() {
    return DialogueGraph.builder().line("l0", null, "Hi there!", "end").end("end").build();
  }

  private AmbientConversationDirector newDirector(FakeView view, FakePrompt prompt) {
    return new AmbientConversationDirector(
        view, new MapDialogueBlackboard(), new EventBus(), prompt);
  }

  @Test
  void armShowsPromptWithoutStartingConversation() {
    FakeView view = new FakeView();
    FakePrompt prompt = new FakePrompt();
    AmbientConversationDirector director = newDirector(view, prompt);

    director.arm(new Signal0(), oneLineGraph(), "Click on Laura to chat");

    assertTrue(director.isArmed());
    assertFalse(director.isConversing());
    assertTrue(prompt.visible);
    assertEquals("Click on Laura to chat", prompt.text);
    assertEquals(0, view.bindCount);
  }

  @Test
  void clickStartsConversationAndHidesPrompt() {
    FakeView view = new FakeView();
    FakePrompt prompt = new FakePrompt();
    AmbientConversationDirector director = newDirector(view, prompt);
    Signal0 click = new Signal0();

    director.arm(click, oneLineGraph(), "Click on Laura to chat");
    click.emit();

    assertTrue(director.isConversing());
    assertFalse(prompt.visible);
    assertEquals(1, view.bindCount);
    assertNotNull(view.bound);
  }

  @Test
  void clickWhileConversingIsIgnored() {
    FakeView view = new FakeView();
    AmbientConversationDirector director = newDirector(view, new FakePrompt());
    Signal0 click = new Signal0();

    director.arm(click, oneLineGraph(), "chat");
    click.emit(); // starts conversation
    click.emit(); // should be ignored while conversing

    assertEquals(1, view.bindCount);
  }

  @Test
  void conversationEndReshowsPromptAndIsRepeatable() {
    FakeView view = new FakeView();
    FakePrompt prompt = new FakePrompt();
    AmbientConversationDirector director = newDirector(view, prompt);
    Signal0 click = new Signal0();

    director.arm(click, oneLineGraph(), "chat");
    click.emit(); // start
    view.bound.advance(); // single line -> end

    assertFalse(director.isConversing());
    assertTrue(prompt.visible); // prompt back for a repeat chat
    assertTrue(director.isArmed());

    click.emit(); // chat again
    assertTrue(director.isConversing());
    assertEquals(2, view.bindCount);
  }

  @Test
  void disarmHidesPromptAndDisconnectsClick() {
    FakeView view = new FakeView();
    FakePrompt prompt = new FakePrompt();
    AmbientConversationDirector director = newDirector(view, prompt);
    Signal0 click = new Signal0();

    director.arm(click, oneLineGraph(), "chat");
    director.disarm();

    assertFalse(director.isArmed());
    assertFalse(prompt.visible);

    click.emit(); // disconnected -> no conversation
    assertEquals(0, view.bindCount);
    assertFalse(director.isConversing());
  }

  @Test
  void disarmDuringConversationResetsView() {
    FakeView view = new FakeView();
    FakePrompt prompt = new FakePrompt();
    AmbientConversationDirector director = newDirector(view, prompt);
    Signal0 click = new Signal0();

    director.arm(click, oneLineGraph(), "chat");
    click.emit(); // conversing
    director.disarm();

    assertFalse(director.isConversing());
    assertFalse(prompt.visible);
    assertEquals(1, view.resetCount);
  }
}
