package com.cryptroot.core.ui;

/**
 * Marker interface for {@link UiWidget} implementations that accept keyboard text input (currently
 * only {@link InputField}).
 *
 * <p>{@link UiLayer} checks {@code widget instanceof Focusable} after a successful {@code hit()}
 * call and grants focus via {@link UiLayer#setFocus(Focusable)} if the widget implements this
 * interface. Only one widget holds focus at a time; the previous focus holder receives {@link
 * #onFocusLost()} before the new one receives {@link #onFocusGained()}.
 *
 * <p>Key-routing contract: keyboard events arriving at {@link UiLayer#inputProcessor()} are
 * forwarded to the focused widget <em>before</em> reaching the screen's own keyboard {@link
 * com.badlogic.gdx.InputAdapter InputAdapter}. Any key not consumed here (returns {@code false}
 * from {@link #focusedKeyDown}) will propagate normally.
 */
public interface Focusable {

  /**
   * Called by {@link UiLayer} when a printable character is typed whilst this widget has focus.
   * Non-printable characters (control codes, backspace, etc.) arrive via {@link
   * #focusedKeyDown(int)} instead.
   */
  void keyTyped(char character);

  /**
   * Called by {@link UiLayer} for a key-down event whilst this widget has focus.
   *
   * <p>Implement handlers for: {@link com.badlogic.gdx.Input.Keys#BACKSPACE}, {@link
   * com.badlogic.gdx.Input.Keys#FORWARD_DEL}, {@link com.badlogic.gdx.Input.Keys#LEFT}, {@link
   * com.badlogic.gdx.Input.Keys#RIGHT}, {@link com.badlogic.gdx.Input.Keys#HOME}, {@link
   * com.badlogic.gdx.Input.Keys#END}, {@link com.badlogic.gdx.Input.Keys#ENTER}.
   *
   * <p>Return {@code false} for {@link com.badlogic.gdx.Input.Keys#ESCAPE}, {@link
   * com.badlogic.gdx.Input.Keys#TAB}, or any screen-navigation key so the screen's keyboard handler
   * still receives it.
   *
   * @return {@code true} if the key event was consumed by this widget.
   */
  boolean focusedKeyDown(int keycode);

  /** Called by {@link UiLayer} when this widget gains keyboard focus. */
  void onFocusGained();

  /** Called by {@link UiLayer} when focus moves to another widget or is cleared. */
  void onFocusLost();
}
