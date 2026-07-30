package com.cryptroot.core.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.ScreenUtils;
import com.cryptroot.core.GameContext;
import com.cryptroot.core.debug.ScreenCapture;
import com.cryptroot.core.ui.UiLayer;
import java.util.Objects;

/**
 * Abstract base for all screens in any game built on {@code myjourney-core}.
 *
 * <p>Parameterised on {@code C} so each game supplies its own concrete {@link GameContext} subclass
 * without casting.
 *
 * <p>Provides:
 *
 * <ul>
 *   <li>Shared {@link #context} and per-screen {@link #uiLayer}.
 *   <li>A final {@link #render(float)} template: runs the {@code uiLayer.update} early-return
 *       guard, clears the screen, applies the viewport, calls the {@link #onRender(float)} hook for
 *       scene content, then draws the {@link #uiLayer} as the final pass.
 *   <li>Final {@link #show()} — runs {@link #onShow()} then installs {@link #inputProcessor()}.
 *   <li>Final {@link #resize(int, int)} — updates the viewport, resizes the selection-outline FBO,
 *       and triggers a UI layout pass.
 *   <li>Final {@link #hide()} — clears the input processor and resets the UI layer. Subclasses may
 *       add teardown via the {@link #onHide()} hook.
 * </ul>
 *
 * <p><b>The UI pass belongs to this class.</b> Subclasses draw scene content in {@link
 * #onRender(float)} and never touch {@link #uiLayer}'s draw calls, so the UI is always rendered
 * with the same camera it was laid out and hit-tested against. A UI-only screen therefore overrides
 * no render hook at all — see {@link
 * UiLayer#render(com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch,
 * com.cryptroot.core.render.SelectionOutlineRenderer)}.
 *
 * <p>Navigation (the "navigator" reference) is intentionally absent from this class. Each game's
 * own intermediate base class adds it with the correct navigator type.
 *
 * @param <C> the concrete {@link GameContext} subclass for this game
 */
public abstract class BaseScreen<C extends GameContext> extends ScreenAdapter {

  protected final C context;
  protected final UiLayer uiLayer;

  /** Pending one-shot debug capture, or {@code null} when none is requested. */
  private String capturePath;

  private int captureCountdown;
  private boolean captureExit;

  protected BaseScreen(C context) {
    Objects.requireNonNull(context, "context must not be null");
    this.context = context;
    this.uiLayer = new UiLayer(context.viewport(), context.camera());
  }

  // -------------------------------------------------------------------------
  // Hooks
  // -------------------------------------------------------------------------

  /** The clear colour used at the start of every frame. Default is {@link Color#BLACK}. */
  protected Color clearColor() {
    return Color.BLACK;
  }

  /**
   * Draws world / scene content for this frame.
   *
   * <p>Do <em>not</em> draw {@link #uiLayer} here — {@link #render(float)} draws it for you
   * afterwards, with the correct projection and its own {@code begin()}/{@code end()} block. An
   * implementation must leave the batch not drawing.
   *
   * <p>The default is a no-op, so a screen whose entire content is UI widgets does not implement
   * this at all.
   */
  protected void onRender(float delta) {}

  /**
   * Called at the start of every {@link #show()}, before {@link #inputProcessor()} is queried.
   * Populate the {@link #uiLayer} and any world state here.
   */
  protected void onShow() {}

  /**
   * The {@link InputProcessor} installed on every {@link #show()}. Defaults to {@link
   * UiLayer#inputProcessor()}.
   *
   * <p>Override to compose screen-level keyboard handling with the UI routing, e.g.:
   *
   * <pre>{@code
   * protected InputProcessor inputProcessor() {
   *     return new InputMultiplexer(uiLayer.inputProcessor(), keyboardAdapter());
   * }
   * }</pre>
   *
   * <p>Called once per {@code show()}, which satisfies {@link UiLayer#inputProcessor()}'s "exactly
   * once per screen show" contract.
   */
  protected InputProcessor inputProcessor() {
    return uiLayer.inputProcessor();
  }

  /** Called after the base {@link #hide()} cleanup. Override for additional teardown. */
  protected void onHide() {}

  // -------------------------------------------------------------------------
  // ScreenAdapter
  // -------------------------------------------------------------------------

  /**
   * Final frame template: the {@code uiLayer.update} early-return guard, clear, viewport apply,
   * {@link #onRender(float)} for scene content, then the UI pass.
   *
   * <p>The UI pass is owned here rather than left to subclasses because it is the only way to
   * guarantee the UI is drawn with the same camera it is laid out and hit-tested against.
   */
  @Override
  public final void render(float delta) {
    if (uiLayer.update(delta)) return;
    ScreenUtils.clear(clearColor());
    context.viewport().apply();
    onRender(delta);
    uiLayer.render(context.batch(), context.outlineRenderer());
    serviceCapture();
  }

  // -------------------------------------------------------------------------
  // Debug capture
  // -------------------------------------------------------------------------

  /**
   * Captures the next fully drawn frame to {@code path}. See {@link #requestCapture(String, int,
   * boolean)}.
   */
  public final void requestCapture(String path) {
    requestCapture(path, 1, false);
  }

  /**
   * Captures a frame to {@code path} once {@code afterFrames} frames have been drawn, optionally
   * exiting the application afterwards.
   *
   * <p>Waiting a few frames matters: the first frame after {@code show()} may precede the initial
   * {@code resize()}, and font/texture uploads can land a frame late. {@code afterFrames} of 3–5 is
   * a safe default for a screenshot meant to represent the steady state.
   *
   * <p>{@code exitAfter} makes the render pipeline verifiable with no human in the loop — the
   * process renders, writes the PNG, and terminates. Only one capture can be pending at a time; a
   * second call replaces the first.
   *
   * @param path destination path, resolved as a libGDX local file
   * @param afterFrames number of frames to draw before capturing; clamped to at least 1
   * @param exitAfter whether to call {@link com.badlogic.gdx.Application#exit()} once written
   */
  public final void requestCapture(String path, int afterFrames, boolean exitAfter) {
    this.capturePath = Objects.requireNonNull(path, "path must not be null");
    this.captureCountdown = Math.max(1, afterFrames);
    this.captureExit = exitAfter;
  }

  /**
   * Writes the pending capture, if this is the frame it was due. Runs after the UI pass so the
   * image contains the completed frame.
   */
  private void serviceCapture() {
    if (capturePath == null) return;
    if (--captureCountdown > 0) return;
    String path = capturePath;
    boolean exit = captureExit;
    capturePath = null; // one-shot: clear before capturing so a throw cannot loop
    FileHandle written = ScreenCapture.capture(path);
    Gdx.app.log("ScreenCapture", "wrote " + written.file().getAbsolutePath());
    if (exit) Gdx.app.exit();
  }

  /**
   * Final {@code show()}: runs {@link #onShow()}, then installs {@link #inputProcessor()}.
   *
   * <p>Sealed so that the processor is always re-installed on re-entry — {@link #hide()} nulls it
   * out, so a screen that installed its processor anywhere other than {@code show()} would be
   * permanently unresponsive the second time it is shown.
   */
  @Override
  public final void show() {
    onShow();
    Gdx.input.setInputProcessor(inputProcessor());
  }

  @Override
  public final void resize(int width, int height) {
    context.viewport().update(width, height, true);
    context.outlineRenderer().resize(context.viewport());
    uiLayer.layout();
  }

  @Override
  public final void hide() {
    Gdx.input.setInputProcessor(null);
    uiLayer.reset();
    onHide();
  }
}
