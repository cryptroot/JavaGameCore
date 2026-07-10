package com.cryptroot.core.dialogue;

import static org.junit.jupiter.api.Assertions.*;

import com.cryptroot.core.event.EventBus;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pure-Java behavioural tests for {@link DialogueRunner}. No LibGDX is involved — {@link Speaker}
 * references in nodes are left {@code null} since the runner never touches them.
 */
class DialogueRunnerTest {

  private EventBus bus;
  private MapDialogueBlackboard bb;

  private DialogueRunner runnerFor(DialogueGraph graph) {
    bus = new EventBus();
    bb = new MapDialogueBlackboard();
    return new DialogueRunner(graph, bb, bus);
  }

  @Test
  void linearScriptEmitsLinesInOrderThenEnds() {
    DialogueGraph graph =
        DialogueGraph.builder()
            .line("a", null, "first", "b")
            .line("b", null, "second", "end")
            .end("end")
            .build();
    DialogueRunner runner = runnerFor(graph);

    List<String> seen = new ArrayList<>();
    boolean[] ended = {false};
    runner.onLine.connect(l -> seen.add(l.text()));
    runner.onEnded.connect(() -> ended[0] = true);

    runner.start();
    assertEquals(List.of("first"), seen);
    runner.advance();
    assertEquals(List.of("first", "second"), seen);
    runner.advance();
    assertTrue(ended[0]);
    assertNull(runner.currentNodeId());
  }

  @Test
  void selectRoutesToChosenBranch() {
    DialogueGraph graph =
        DialogueGraph.builder()
            .choice("q", null, "pick", List.of(Option.to("left", "L"), Option.to("right", "R")))
            .line("L", null, "went left", "end")
            .line("R", null, "went right", "end")
            .end("end")
            .build();
    DialogueRunner runner = runnerFor(graph);

    List<String> seen = new ArrayList<>();
    runner.onLine.connect(l -> seen.add(l.text()));

    runner.start();
    runner.select(1);
    assertEquals(List.of("went right"), seen);
  }

  @Test
  void disabledOptionIsIgnored() {
    DialogueGraph graph =
        DialogueGraph.builder()
            .choice(
                "q",
                null,
                "pick",
                List.of(
                    new Option("locked", "L", b -> b.flag("hasKey"), null), Option.to("open", "R")))
            .line("L", null, "locked path", "end")
            .line("R", null, "open path", "end")
            .end("end")
            .build();
    DialogueRunner runner = runnerFor(graph);

    List<String> seen = new ArrayList<>();
    runner.onLine.connect(l -> seen.add(l.text()));

    runner.start();
    runner.select(0); // disabled — ignored
    assertTrue(seen.isEmpty());
    assertEquals("q", runner.currentNodeId());

    runner.select(1); // enabled
    assertEquals(List.of("open path"), seen);
  }

  @Test
  void optionEffectMutatesBlackboard() {
    DialogueGraph graph =
        DialogueGraph.builder()
            .choice(
                "q", null, "pick", List.of(Option.to("set", "end", b -> b.setFlag("chose", true))))
            .end("end")
            .build();
    DialogueRunner runner = runnerFor(graph);

    runner.start();
    runner.select(0);
    assertTrue(bb.flag("chose"));
  }

  @Test
  void eventFiresOnceAndPassesThrough() {
    int[] count = {0};
    DialogueGraph graph =
        DialogueGraph.builder()
            .event("e", b -> count[0]++, "after")
            .line("after", null, "done", "end")
            .end("end")
            .build();
    DialogueRunner runner = runnerFor(graph);

    List<String> seen = new ArrayList<>();
    runner.onLine.connect(l -> seen.add(l.text()));

    runner.start();
    assertEquals(1, count[0]);
    assertEquals(List.of("done"), seen);
  }

  @Test
  void actionParksUntilRequirementSatisfied() {
    DialogueGraph graph =
        DialogueGraph.builder()
            .line("intro", null, "wait for it", "gate")
            .action("gate", "ready", "do the thing", WaitMode.INLINE, false, "after")
            .line("after", null, "thanks!", "end")
            .end("end")
            .build();
    DialogueRunner runner = runnerFor(graph);

    List<String> seen = new ArrayList<>();
    boolean[] awaited = {false};
    runner.onLine.connect(l -> seen.add(l.text()));
    runner.onAwaitAction.connect(a -> awaited[0] = true);

    runner.start();
    runner.advance(); // reach the action
    assertTrue(awaited[0]);
    assertTrue(runner.isAwaitingAction());
    assertEquals(List.of("wait for it"), seen);

    runner.onRequirementSatisfied("other"); // wrong key — no resume
    assertTrue(runner.isAwaitingAction());

    bb.setRequirementSatisfied("ready", true);
    runner.onRequirementSatisfied("ready");
    assertFalse(runner.isAwaitingAction());
    assertEquals(List.of("wait for it", "thanks!"), seen);
  }

  @Test
  void preSatisfiedActionPassesThrough() {
    DialogueGraph graph =
        DialogueGraph.builder()
            .action("gate", "ready", "hint", WaitMode.SUSPEND, false, "after")
            .line("after", null, "go", "end")
            .end("end")
            .build();
    DialogueRunner runner = runnerFor(graph);
    bb.setRequirementSatisfied("ready", true);

    List<String> seen = new ArrayList<>();
    boolean[] awaited = {false};
    runner.onLine.connect(l -> seen.add(l.text()));
    runner.onAwaitAction.connect(a -> awaited[0] = true);

    runner.start();
    assertFalse(awaited[0]);
    assertEquals(List.of("go"), seen);
  }

  @Test
  void resumeAtJumpsToNode() {
    DialogueGraph graph =
        DialogueGraph.builder()
            .line("a", null, "A", "b")
            .line("b", null, "B", "end")
            .end("end")
            .build();
    DialogueRunner runner = runnerFor(graph);

    List<String> seen = new ArrayList<>();
    runner.onLine.connect(l -> seen.add(l.text()));

    runner.resumeAt("b");
    assertEquals(List.of("B"), seen);
    assertEquals("b", runner.currentNodeId());
  }

  @Test
  void builderRejectsDanglingReference() {
    assertThrows(
        IllegalStateException.class,
        () -> DialogueGraph.builder().line("a", null, "A", "missing").end("end").build());
  }
}
