package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.cryptroot.core.dialogue.DialogueGraph;
import com.cryptroot.core.dialogue.DialogueNode;
import com.cryptroot.core.dialogue.DialogueRunner;
import com.cryptroot.core.dialogue.DialogueView;
import com.cryptroot.core.dialogue.MapDialogueBlackboard;
import com.cryptroot.core.dialogue.MessageLine;
import com.cryptroot.core.dialogue.Speaker;
import com.cryptroot.core.dialogue.WaitMode;
import com.cryptroot.core.event.DisposableConnection;
import com.cryptroot.core.event.EventBus;
import com.cryptroot.core.event.Signal0;
import java.util.ArrayList;
import java.util.List;

/**
 * A composite widget that renders a conversation driven by a {@link DialogueRunner}. It displays
 * one or two {@link Speaker}s, a {@link MessageBox} for the active line, and a {@link ChoiceList}
 * for branching options.
 *
 * <p>The widget is a thin presentation layer: it subscribes to the runner's signals ({@code
 * onLine}, {@code onChoices}, {@code onAwaitAction}, {@code onEnded}) and translates clicks back
 * into {@code advance()} / {@code select(i)} calls. All branching, conditions, effects and
 * pause/resume logic live in the runner.
 *
 * <h3>Two ways to supply content</h3>
 *
 * <ul>
 *   <li>{@link #setScript(List)} — legacy linear script. The widget builds a line-only {@link
 *       DialogueGraph} and an internal runner, and <em>loops</em> on completion unless a listener
 *       of {@link #onConversationEnded} calls {@link #reset()} (preserving the original behaviour).
 *   <li>{@link #bind(DialogueRunner)} — attach an externally-owned runner (e.g. from a {@code
 *       StoryDirector}). Director-bound conversations do <em>not</em> loop; the director controls
 *       replay.
 * </ul>
 *
 * <h3>Modal mode</h3>
 *
 * {@link #setModal(boolean) setModal(true)} makes the widget consume every pointer event while
 * active — suppressing all lower-z widgets.
 *
 * <h3>Programmatic start</h3>
 *
 * {@link #start()} opens dialogue without requiring a widget click first.
 *
 * <h3>Reset</h3>
 *
 * {@link #reset()} hides the box/choices and returns to the pre-conversation state. Called
 * automatically by {@link UiLayer#reset()} on screen hide.
 */
public final class ConversationWidget extends BoundedWidget implements DialogueView {

  // -------------------------------------------------------------------------
  // Layout constants
  // -------------------------------------------------------------------------

  /** Fraction of the widget height allocated to the message box at the bottom. */
  private static final float MSG_BOX_H_RATIO = 0.28f;

  /** Vertical gap (world units) between the speaker area and the message box. */
  private static final float SPEAKER_GAP = 10f;

  /** For one speaker: centred at this fraction of widget width. */
  private static final float SINGLE_SPEAKER_X_RATIO = 0.22f;

  private static final float LEFT_SPEAKER_X_RATIO = 0.22f;
  private static final float RIGHT_SPEAKER_X_RATIO = 0.78f;

  /** Maximum speaker width as a fraction of total widget width. */
  private static final float SPEAKER_MAX_W_RATIO = 0.35f;

  /** Choice list horizontal inset and width as fractions of widget width. */
  private static final float CHOICE_X_RATIO = 0.12f;

  private static final float CHOICE_W_RATIO = 0.76f;

  // -------------------------------------------------------------------------
  // Fields
  // -------------------------------------------------------------------------

  /** Active participants derived from the bound graph (first-appearance order). */
  private List<Speaker> participants = new ArrayList<>();

  private boolean modal = false;
  private boolean autoStart = true;

  private final MessageBox messageBox;
  private final ChoiceList choiceList;

  private float cx, cy, cw, ch;

  private Speaker activeSpeaker = null;

  // ---- Runner binding ----
  private DialogueRunner runner;
  private final List<DisposableConnection> runnerConns = new ArrayList<>();

  /** {@code true} once {@link #start()} (or autoStart) has opened the dialogue. */
  private boolean started = false;

  /** When {@code true}, the conversation restarts on end (legacy script path). */
  private boolean loopOnEnd = false;

  private boolean showingChoices = false;
  private boolean awaitingAction = false;

  /**
   * Fires when the conversation reaches its end node. For script-driven (looping) conversations, a
   * listener may call {@link #reset()} to stop the loop.
   */
  public final Signal0 onConversationEnded = new Signal0();

  // -------------------------------------------------------------------------
  // Constructor
  // -------------------------------------------------------------------------

  public ConversationWidget(float x, float y, float w, float h, Texture pixel, BitmapFont font) {
    this.cx = x;
    this.cy = y;
    this.cw = w;
    this.ch = h;

    messageBox = new MessageBox(pixel, font, x, y, w, h * MSG_BOX_H_RATIO);
    addChild(messageBox);

    choiceList = new ChoiceList(pixel, font);
    choiceList.hide();
    addChild(choiceList);
    choiceList.onSelect.connect(this::onChoiceSelected);
  }

  // -------------------------------------------------------------------------
  // Configuration
  // -------------------------------------------------------------------------

  public ConversationWidget setModal(boolean modal) {
    this.modal = modal;
    return this;
  }

  public ConversationWidget setAutoStart(boolean autoStart) {
    this.autoStart = autoStart;
    return this;
  }

  /** Returns {@code true} while dialogue is showing. */
  public boolean isActive() {
    return started;
  }

  // -------------------------------------------------------------------------
  // Content binding
  // -------------------------------------------------------------------------

  /**
   * Sets (or replaces) a legacy linear script. Internally builds a line-only {@link DialogueGraph}
   * with a private blackboard and event bus, and binds a looping runner that does not auto-start
   * (it begins on click when {@code autoStart} is enabled, or via {@link #start()}).
   */
  public void setScript(List<MessageLine> newScript) {
    if (newScript == null || newScript.isEmpty()) {
      disconnectRunner();
      runner = null;
      clearParticipants();
      started = false;
      return;
    }
    DialogueGraph graph = buildLinearGraph(newScript);
    DialogueRunner r = new DialogueRunner(graph, new MapDialogueBlackboard(), new EventBus());
    loopOnEnd = true;
    connectRunner(r);
    started = false;
  }

  /**
   * Connects an externally-owned {@link DialogueRunner} without starting it. Used to resume a
   * parked runner (the director then calls {@link DialogueRunner#onRequirementSatisfied(String)}).
   */
  @Override
  public void rebind(DialogueRunner r) {
    loopOnEnd = false;
    connectRunner(r);
    started = true;
  }

  /**
   * Binds an externally-owned {@link DialogueRunner} and starts it immediately. Director-bound
   * conversations do not loop on end.
   */
  @Override
  public void bind(DialogueRunner r) {
    rebind(r);
    r.start();
  }

  /**
   * Binds a graph with the given blackboard and event bus (non-looping), to be started via {@link
   * #start()}.
   */
  public void setGraph(DialogueGraph graph, MapDialogueBlackboard blackboard, EventBus eventBus) {
    DialogueRunner r = new DialogueRunner(graph, blackboard, eventBus);
    loopOnEnd = false;
    connectRunner(r);
    started = false;
  }

  /** Programmatically starts (or restarts) the bound conversation from its start node. */
  public void start() {
    if (runner == null) return;
    if (activeSpeaker != null) {
      activeSpeaker.stopSpeaking();
      activeSpeaker = null;
    }
    started = true;
    runner.start();
  }

  public void setBounds(float x, float y, float w, float h) {
    this.cx = x;
    this.cy = y;
    this.cw = w;
    this.ch = h;
  }

  // -------------------------------------------------------------------------
  // Runner wiring
  // -------------------------------------------------------------------------

  private void connectRunner(DialogueRunner r) {
    disconnectRunner();
    this.runner = r;
    runnerConns.add(r.onLine.connect(this::handleLine));
    runnerConns.add(r.onChoices.connect(this::handleChoices));
    runnerConns.add(r.onAwaitAction.connect(this::handleAwait));
    runnerConns.add(r.onEnded.connect(this::handleEnded));
    deriveParticipants(r.graph());
    showingChoices = false;
    awaitingAction = false;
    if (cw > 0 && ch > 0) layout();
  }

  private void disconnectRunner() {
    for (DisposableConnection c : runnerConns) c.disconnect();
    runnerConns.clear();
  }

  private void onChoiceSelected(int index) {
    if (runner != null) runner.select(index);
  }

  // -------------------------------------------------------------------------
  // Runner signal handlers
  // -------------------------------------------------------------------------

  private void handleLine(DialogueNode.Line line) {
    showingChoices = false;
    awaitingAction = false;
    choiceList.hide();
    if (activeSpeaker != null && activeSpeaker != line.speaker()) {
      activeSpeaker.stopSpeaking();
    }
    activeSpeaker = line.speaker();
    activeSpeaker.startSpeaking();
    messageBox.setContent(activeSpeaker, line.text());
  }

  private void handleChoices(DialogueNode.Choice choice) {
    awaitingAction = false;
    if (activeSpeaker != null && activeSpeaker != choice.speaker()) {
      activeSpeaker.stopSpeaking();
    }
    activeSpeaker = choice.speaker();
    activeSpeaker.startSpeaking();
    messageBox.setContent(activeSpeaker, choice.prompt());

    List<String> labels = new ArrayList<>();
    for (var option : choice.options()) labels.add(option.text());
    choiceList.setOptions(labels, runner.enabledFlags(choice));
    choiceList.show();
    showingChoices = true;
  }

  private void handleAwait(DialogueNode.Action action) {
    showingChoices = false;
    choiceList.hide();
    // SUSPEND-mode actions close the conversation entirely; the owning
    // StoryDirector resets this widget.  Only INLINE awaits keep it open.
    if (action.waitMode() == WaitMode.SUSPEND) {
      awaitingAction = false;
      return;
    }
    awaitingAction = true;
    if (activeSpeaker != null && action.hintText() != null && !action.hintText().isEmpty()) {
      messageBox.setContent(activeSpeaker, action.hintText());
    }
  }

  private void handleEnded() {
    showingChoices = false;
    awaitingAction = false;
    choiceList.hide();
    if (activeSpeaker != null) {
      activeSpeaker.stopSpeaking();
      activeSpeaker = null;
    }
    messageBox.clearContent();

    onConversationEnded.emit();
    // If a listener called reset(), started is now false — do not loop.
    if (loopOnEnd && started && runner != null) {
      runner.start();
    } else {
      started = false;
    }
  }

  // -------------------------------------------------------------------------
  // Participant derivation
  // -------------------------------------------------------------------------

  private void deriveParticipants(DialogueGraph graph) {
    clearParticipants();
    List<Speaker> unique = new ArrayList<>();
    for (DialogueNode node : graph.nodes()) {
      Speaker s =
          switch (node) {
            case DialogueNode.Line l -> l.speaker();
            case DialogueNode.Choice c -> c.speaker();
            default -> null;
          };
      if (s != null && !unique.contains(s)) unique.add(s);
    }
    participants = List.copyOf(unique);
    if (participants.size() >= 2) participants.get(1).setMirrorX(true);
    for (Speaker s : participants) s.stopSpeaking();
  }

  private void clearParticipants() {
    for (Speaker s : participants) s.setMirrorX(false);
    participants = new ArrayList<>();
  }

  // -------------------------------------------------------------------------
  // Linear-graph builder for the legacy script path
  // -------------------------------------------------------------------------

  private static DialogueGraph buildLinearGraph(List<MessageLine> script) {
    DialogueGraph.Builder b = DialogueGraph.builder();
    String endId = "END";
    for (int i = 0; i < script.size(); i++) {
      MessageLine line = script.get(i);
      String id = "L" + i;
      String nextId = (i + 1 < script.size()) ? "L" + (i + 1) : endId;
      b.line(id, line.speaker(), line.text(), nextId);
    }
    b.end(endId);
    return b.start("L0").build();
  }

  // -------------------------------------------------------------------------
  // BoundedWidget lifecycle
  // -------------------------------------------------------------------------

  @Override
  protected void doBoundedLayout() {
    bounds.set(cx, cy, cw, ch);

    float msgH = ch * MSG_BOX_H_RATIO;
    messageBox.setBounds(cx, cy, cw, msgH);

    // Choice list occupies the area above the message box.
    float choiceBot = cy + msgH + SPEAKER_GAP;
    float choiceH = (cy + ch) - choiceBot;
    choiceList.setBounds(cx + cw * CHOICE_X_RATIO, choiceBot, cw * CHOICE_W_RATIO, choiceH);

    if (participants.isEmpty()) return;

    float speakerAreaTop = cy + ch;
    float speakerAreaBot = cy + msgH + SPEAKER_GAP;
    float speakerAreaH = speakerAreaTop - speakerAreaBot;
    float maxSpeakerW = cw * SPEAKER_MAX_W_RATIO;

    if (participants.size() == 1) {
      Speaker s = participants.get(0);
      s.setPosition(cx + cw * SINGLE_SPEAKER_X_RATIO, speakerAreaBot);
      s.fitHeight(speakerAreaH, maxSpeakerW);
    } else {
      Speaker left = participants.get(0);
      Speaker right = participants.get(1);
      left.setPosition(cx + cw * LEFT_SPEAKER_X_RATIO, speakerAreaBot);
      right.setPosition(cx + cw * RIGHT_SPEAKER_X_RATIO, speakerAreaBot);
      left.fitHeight(speakerAreaH, maxSpeakerW);
      right.fitHeight(speakerAreaH, maxSpeakerW);
    }
  }

  @Override
  protected void doBoundedReset() {
    if (activeSpeaker != null) {
      activeSpeaker.stopSpeaking();
      activeSpeaker = null;
    }
    started = false;
    showingChoices = false;
    awaitingAction = false;
    // messageBox.reset() and choiceList.reset() run via the child cascade.
  }

  // -------------------------------------------------------------------------
  // UiWidget overrides
  // -------------------------------------------------------------------------

  @Override
  public boolean blocksPointer(float worldX, float worldY) {
    return started && bounds.contains(worldX, worldY);
  }

  @Override
  public void updateHover(float worldX, float worldY) {
    hovered = bounds.contains(worldX, worldY);
    choiceList.updateHover(worldX, worldY);
  }

  @Override
  public boolean hit(float worldX, float worldY) {
    if (!started) {
      if (!autoStart || runner == null || !bounds.contains(worldX, worldY)) return false;
      start();
      return true;
    }

    boolean inside = bounds.contains(worldX, worldY);

    if (showingChoices) {
      if (choiceList.hit(worldX, worldY)) return true; // option selected
      return modal || inside; // otherwise consume
    }
    if (awaitingAction) {
      return modal || inside; // consume, no advance
    }
    // Active line: advance on click.
    if (!modal && !inside) return false;
    if (runner != null) runner.advance();
    return true;
  }

  @Override
  public boolean update(float delta) {
    for (Speaker s : participants) s.update(delta);
    return super.update(delta);
  }

  @Override
  protected void doDraw(PolygonSpriteBatch batch) {
    if (!started) return;
    for (Speaker s : participants) s.draw(batch);
  }
}
