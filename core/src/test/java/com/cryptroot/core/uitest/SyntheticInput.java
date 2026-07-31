package com.cryptroot.core.uitest;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.input.NativeInputConfiguration;
import java.util.Objects;

/**
 * A {@link Input} decorator that reports a <em>scripted</em> pointer while forwarding everything
 * else to the real backend input.
 *
 * <h3>Why this exists</h3>
 *
 * Dispatching events straight to an {@link InputProcessor} is only half of "moving the mouse".
 * Plenty of framework code <em>polls</em> the cursor instead of listening for it:
 *
 * <ul>
 *   <li>{@code UiLayer.update(float)} unprojects {@code Gdx.input.getX()/getY()} once per frame to
 *       drive hover state — so a synthetic {@code mouseMoved} would be overwritten by the real
 *       cursor on the very next frame, and hover visuals (a {@code Button}'s hover darkening) would
 *       never appear in a capture.
 *   <li>{@code UiLayer}'s {@code scrolled} handler reads the cursor position rather than the
 *       event's.
 *   <li>{@code BaseGameScreen} unprojects {@code Gdx.input} for world-space hover.
 * </ul>
 *
 * Overriding the poll at the {@code Gdx.input} seam fixes all of them at once, which a {@code
 * UiLayer}-only injection point could not do.
 *
 * <h3>Cross-platform note</h3>
 *
 * This is what keeps the harness free of OS-level input injection. No {@code java.awt.Robot}, no
 * {@code glfwSetCursorPos} (Wayland has no pointer warping and rejects synthetic X11 input into
 * native surfaces), no compositor permissions — the "cursor" never leaves the JVM, so Win32, X11,
 * XWayland and native Wayland all behave identically. It also means the real mouse is not moved out
 * from under a developer watching the test run.
 *
 * <h3>What is scripted and what is not</h3>
 *
 * Only the polled pointer surface is overridden: position, deltas, touch/button state. Keys are
 * <em>not</em> faked here — {@link UiRobot} delivers key events to the {@link InputProcessor}, and
 * {@code isKeyPressed} keeps telling the truth about the physical keyboard. {@link
 * #setInputProcessor(InputProcessor)} and {@link #getInputProcessor()} forward to the real input,
 * so {@code BaseScreen.show()}'s install still reaches the backend and the harness reads back
 * exactly the processor the screen chose, multiplexer and all.
 *
 * <p>Delegation is written out by hand rather than generated with a {@link java.lang.reflect.Proxy}
 * so that a libGDX upgrade which changes {@link Input} breaks the build instead of the tests.
 */
public final class SyntheticInput implements Input {

  private final Input delegate;

  /** Current scripted pointer, in y-down screen coordinates. */
  private int x;

  private int y;

  /** Previous scripted pointer, so {@code getDeltaX/Y} report the last movement. */
  private int previousX;

  private int previousY;

  private boolean leftButtonDown;

  /**
   * Set by {@link #pressLeft()}, cleared by {@link #endFrame()} — mirrors libGDX's per-frame flag.
   */
  private boolean justTouched;

  public SyntheticInput(Input delegate) {
    this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
  }

  /** The real backend input this decorator wraps, so it can be restored on teardown. */
  public Input delegate() {
    return delegate;
  }

  // -------------------------------------------------------------------------
  // Scripting API (called by UiRobot, on the GL thread)
  // -------------------------------------------------------------------------

  /** Moves the scripted pointer to {@code (screenX, screenY)} in y-down screen coordinates. */
  public void moveTo(int screenX, int screenY) {
    this.previousX = this.x;
    this.previousY = this.y;
    this.x = screenX;
    this.y = screenY;
  }

  /** Marks the left button as held, as a real {@code touchDown} would. */
  public void pressLeft() {
    leftButtonDown = true;
    justTouched = true;
  }

  /** Marks the left button as released. */
  public void releaseLeft() {
    leftButtonDown = false;
  }

  /**
   * Clears the one-frame {@link #justTouched()} flag. Called once per frame by {@link
   * ScenarioDriver}, mirroring what the backend does at the end of its own frame.
   */
  public void endFrame() {
    justTouched = false;
    previousX = x;
    previousY = y;
  }

  // -------------------------------------------------------------------------
  // Input — overridden (the polled pointer surface)
  // -------------------------------------------------------------------------

  @Override
  public int getX() {
    return x;
  }

  @Override
  public int getX(int pointer) {
    return pointer == 0 ? x : 0;
  }

  @Override
  public int getY() {
    return y;
  }

  @Override
  public int getY(int pointer) {
    return pointer == 0 ? y : 0;
  }

  @Override
  public int getDeltaX() {
    return x - previousX;
  }

  @Override
  public int getDeltaX(int pointer) {
    return pointer == 0 ? getDeltaX() : 0;
  }

  @Override
  public int getDeltaY() {
    return y - previousY;
  }

  @Override
  public int getDeltaY(int pointer) {
    return pointer == 0 ? getDeltaY() : 0;
  }

  @Override
  public boolean isTouched() {
    return leftButtonDown;
  }

  @Override
  public boolean isTouched(int pointer) {
    return pointer == 0 && leftButtonDown;
  }

  @Override
  public boolean justTouched() {
    return justTouched;
  }

  @Override
  public boolean isButtonPressed(int button) {
    return button == Buttons.LEFT && leftButtonDown;
  }

  @Override
  public boolean isButtonJustPressed(int button) {
    return button == Buttons.LEFT && justTouched;
  }

  @Override
  public float getPressure() {
    return leftButtonDown ? 1f : 0f;
  }

  @Override
  public float getPressure(int pointer) {
    return pointer == 0 ? getPressure() : 0f;
  }

  /**
   * Ignored on purpose. Warping the real cursor is the one thing this class exists to avoid; a
   * scripted move goes through {@link #moveTo(int, int)} instead.
   */
  @Override
  public void setCursorPosition(int x, int y) {
    // no-op — see the class comment.
  }

  // -------------------------------------------------------------------------
  // Input — plain delegation
  // -------------------------------------------------------------------------

  @Override
  public float getAccelerometerX() {
    return delegate.getAccelerometerX();
  }

  @Override
  public float getAccelerometerY() {
    return delegate.getAccelerometerY();
  }

  @Override
  public float getAccelerometerZ() {
    return delegate.getAccelerometerZ();
  }

  @Override
  public float getGyroscopeX() {
    return delegate.getGyroscopeX();
  }

  @Override
  public float getGyroscopeY() {
    return delegate.getGyroscopeY();
  }

  @Override
  public float getGyroscopeZ() {
    return delegate.getGyroscopeZ();
  }

  @Override
  public int getMaxPointers() {
    return delegate.getMaxPointers();
  }

  @Override
  public boolean isKeyPressed(int key) {
    return delegate.isKeyPressed(key);
  }

  @Override
  public boolean isKeyJustPressed(int key) {
    return delegate.isKeyJustPressed(key);
  }

  @Override
  public void getTextInput(TextInputListener listener, String title, String text, String hint) {
    delegate.getTextInput(listener, title, text, hint);
  }

  @Override
  public void getTextInput(
      TextInputListener listener,
      String title,
      String text,
      String hint,
      OnscreenKeyboardType type) {
    delegate.getTextInput(listener, title, text, hint, type);
  }

  @Override
  public void setOnscreenKeyboardVisible(boolean visible) {
    delegate.setOnscreenKeyboardVisible(visible);
  }

  @Override
  public void setOnscreenKeyboardVisible(boolean visible, OnscreenKeyboardType type) {
    delegate.setOnscreenKeyboardVisible(visible, type);
  }

  @Override
  public void openTextInputField(NativeInputConfiguration configuration) {
    delegate.openTextInputField(configuration);
  }

  @Override
  public void closeTextInputField(boolean sendReturn) {
    delegate.closeTextInputField(sendReturn);
  }

  @Override
  public void setKeyboardHeightObserver(KeyboardHeightObserver observer) {
    delegate.setKeyboardHeightObserver(observer);
  }

  @Override
  public void vibrate(int milliseconds) {
    delegate.vibrate(milliseconds);
  }

  @Override
  public void vibrate(int milliseconds, boolean fallback) {
    delegate.vibrate(milliseconds, fallback);
  }

  @Override
  public void vibrate(int milliseconds, int amplitude, boolean fallback) {
    delegate.vibrate(milliseconds, amplitude, fallback);
  }

  @Override
  public void vibrate(VibrationType vibrationType) {
    delegate.vibrate(vibrationType);
  }

  @Override
  public float getAzimuth() {
    return delegate.getAzimuth();
  }

  @Override
  public float getPitch() {
    return delegate.getPitch();
  }

  @Override
  public float getRoll() {
    return delegate.getRoll();
  }

  @Override
  public void getRotationMatrix(float[] matrix) {
    delegate.getRotationMatrix(matrix);
  }

  @Override
  public long getCurrentEventTime() {
    return delegate.getCurrentEventTime();
  }

  @Override
  public void setCatchKey(int keycode, boolean catchKey) {
    delegate.setCatchKey(keycode, catchKey);
  }

  @Override
  public boolean isCatchKey(int keycode) {
    return delegate.isCatchKey(keycode);
  }

  @Override
  public void setInputProcessor(InputProcessor processor) {
    delegate.setInputProcessor(processor);
  }

  @Override
  public InputProcessor getInputProcessor() {
    return delegate.getInputProcessor();
  }

  @Override
  public boolean isPeripheralAvailable(Peripheral peripheral) {
    return delegate.isPeripheralAvailable(peripheral);
  }

  @Override
  public int getRotation() {
    return delegate.getRotation();
  }

  @Override
  public Orientation getNativeOrientation() {
    return delegate.getNativeOrientation();
  }

  @Override
  public void setCursorCatched(boolean catched) {
    delegate.setCursorCatched(catched);
  }

  @Override
  public boolean isCursorCatched() {
    return delegate.isCursorCatched();
  }
}
