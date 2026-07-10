package com.cryptroot.core.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.cryptroot.core.event.Signal;
import com.cryptroot.core.event.Signal0;

/**
 * A single-line text input field.
 *
 * <p>This widget extends {@link CompositeWidget} and implements {@link Focusable}. {@link UiLayer}
 * grants it keyboard focus when it is hit; characters typed while focused are appended at the
 * cursor position. Clicking away, or hitting another widget, removes focus.
 *
 * <p>Supported editing keys:
 *
 * <ul>
 *   <li>Printable characters — insert at cursor
 *   <li>Backspace — delete character before cursor
 *   <li>Delete — delete character after cursor
 *   <li>Left / Right — move cursor
 *   <li>Home / End — move cursor to start / end
 *   <li>Enter — emit {@link #onConfirm}
 * </ul>
 *
 * <p>Text that overflows the visible area is left-truncated so the cursor position is always
 * visible.
 *
 * <pre>{@code
 * InputField field = new InputField(skin, pixel, 900f, 600f, 360f, "Enter name…");
 * field.onConfirm.connect(() -> log.add(field.getText()));
 * uiLayer.add(field, 0);
 * }</pre>
 */
public final class InputField extends BoundedWidget implements Focusable {

  private static final float PADDING_H = 10f;
  private static final float PADDING_V = 8f;
  private static final float CURSOR_WIDTH = 2f;
  private static final float BLINK_PERIOD = 0.5f;

  private static final Color COLOR_BG_NORMAL = new Color(0.12f, 0.12f, 0.18f, 1f);
  private static final Color COLOR_BG_FOCUSED = new Color(0.15f, 0.15f, 0.25f, 1f);
  private static final Color COLOR_PLACEHOLDER = new Color(0.55f, 0.55f, 0.55f, 1f);
  private static final Color COLOR_CURSOR = new Color(0.8f, 0.8f, 1.0f, 1f);

  /** Fires with the full current text on every character insertion or deletion. */
  public final Signal<String> onTextChanged = new Signal<>();

  /** Fires when Enter is pressed. */
  public final Signal0 onConfirm = new Signal0();

  private final UiSkin skin;
  private final Texture pixel;
  private final float x;
  private final float y;
  private final float width;

  /** Renders the visible (possibly left-truncated) input text. Not a registered child. */
  private final TextLabel visibleLabel;

  /** Renders the placeholder hint. Not a registered child. */
  private final TextLabel placeholderLabel;

  /**
   * Reusable {@link GlyphLayout} for left-truncation measurement and cursor X calculation — never
   * allocated per frame.
   */
  private final GlyphLayout glMeasure = new GlyphLayout();

  // Derived in doBoundedLayout()
  private float fieldHeight;
  private float textAreaX;
  private float textAreaW;

  private final StringBuilder text = new StringBuilder();
  private int cursorPos = 0;
  private float blinkTimer = 0f;
  private boolean cursorVisible = true;
  private boolean focused = false;

  /** Cached result of the last {@link #visibleText} call; avoids redundant {@code setText}. */
  private String lastVisible = "";

  /**
   * @param skin skin providing the font and nine-patch border
   * @param pixel 1×1 white texture for background and cursor drawing
   * @param x left edge of the field in world coordinates
   * @param y bottom edge of the field in world coordinates
   * @param width total field width in world coordinates
   * @param placeholder grey hint text shown when the field is empty and unfocused
   */
  public InputField(UiSkin skin, Texture pixel, float x, float y, float width, String placeholder) {
    this.skin = skin;
    this.pixel = pixel;
    this.x = x;
    this.y = y;
    this.width = width;

    visibleLabel = new TextLabel(skin.font(), "", 0f, 0f);
    placeholderLabel = new TextLabel(skin.font(), placeholder, 0f, 0f, COLOR_PLACEHOLDER);
    // Not registered as children — drawn manually in doDraw() so we control
    // exactly when each appears and the cursor is layered above in doAfterDraw().
  }

  public String getText() {
    return text.toString();
  }

  /** Replaces current text and places the cursor at the end. */
  public void setText(String newText) {
    text.setLength(0);
    text.append(newText);
    cursorPos = text.length();
    lastVisible = ""; // force recompute on next draw
  }

  // -------------------------------------------------------------------------
  // CompositeWidget template methods
  // -------------------------------------------------------------------------

  @Override
  protected void doBoundedLayout() {
    fieldHeight = UiHelper.barHeight(skin.font(), PADDING_V);
    textAreaX = x + PADDING_H;
    textAreaW = width - PADDING_H * 2f - CURSOR_WIDTH;
    float textBaseline = y + PADDING_V + skin.font().getCapHeight();

    bounds.set(x, y, width, fieldHeight);

    visibleLabel.setPosition(textAreaX, textBaseline);
    visibleLabel.layout();
    placeholderLabel.setPosition(textAreaX, textBaseline);
    placeholderLabel.layout();
  }

  @Override
  protected void doDraw(PolygonSpriteBatch batch) {
    // Background
    batch.setColor(focused ? COLOR_BG_FOCUSED : COLOR_BG_NORMAL);
    batch.draw(pixel, bounds.x, bounds.y, bounds.width, bounds.height);
    batch.setColor(Color.WHITE);

    // Border
    skin.normalSlice().draw(batch, bounds.x, bounds.y, bounds.width, bounds.height);

    if (text.length() == 0 && !focused) {
      placeholderLabel.draw(batch);
    } else {
      String visible = visibleText(textAreaW);
      if (!visible.equals(lastVisible)) {
        visibleLabel.setText(visible);
        lastVisible = visible;
      }
      visibleLabel.draw(batch);
    }
  }

  /** Draws the cursor above the text once children (none registered) have been drawn. */
  @Override
  protected void doAfterDraw(PolygonSpriteBatch batch) {
    if (!focused || !cursorVisible) return;

    String beforeCursor = text.substring(0, cursorPos);
    String visibleBeforeCursor = visibleTextBeforeCursor(beforeCursor, textAreaW);
    glMeasure.setText(skin.font(), visibleBeforeCursor);
    float cursorX = textAreaX + glMeasure.width;

    batch.setColor(COLOR_CURSOR);
    batch.draw(pixel, cursorX, y + PADDING_V, CURSOR_WIDTH, skin.font().getCapHeight());
    batch.setColor(Color.WHITE);
  }

  @Override
  protected void doBoundedReset() {
    focused = false;
    blinkTimer = 0f;
    cursorVisible = true;
  }

  // -------------------------------------------------------------------------
  // UiWidget overrides
  // -------------------------------------------------------------------------

  @Override
  public void updateHover(float worldX, float worldY) {}

  // hit() inherited from BoundedWidget: return bounds.contains(worldX, worldY)

  @Override
  public boolean update(float delta) {
    if (focused) {
      blinkTimer += delta;
      if (blinkTimer >= BLINK_PERIOD) {
        blinkTimer -= BLINK_PERIOD;
        cursorVisible = !cursorVisible;
      }
    }
    return false;
  }

  // -------------------------------------------------------------------------
  // Focusable
  // -------------------------------------------------------------------------

  @Override
  public void onFocusGained() {
    focused = true;
    blinkTimer = 0f;
    cursorVisible = true;
  }

  @Override
  public void onFocusLost() {
    focused = false;
    cursorVisible = false;
  }

  @Override
  public void keyTyped(char character) {
    text.insert(cursorPos, character);
    cursorPos++;
    lastVisible = ""; // invalidate visible-text cache
    onTextChanged.emit(text.toString());
  }

  @Override
  public boolean focusedKeyDown(int keycode) {
    return switch (keycode) {
      case Input.Keys.BACKSPACE -> {
        if (cursorPos > 0) {
          text.deleteCharAt(cursorPos - 1);
          cursorPos--;
          lastVisible = "";
          onTextChanged.emit(text.toString());
        }
        yield true;
      }
      case Input.Keys.FORWARD_DEL -> {
        if (cursorPos < text.length()) {
          text.deleteCharAt(cursorPos);
          lastVisible = "";
          onTextChanged.emit(text.toString());
        }
        yield true;
      }
      case Input.Keys.LEFT -> {
        cursorPos = Math.max(0, cursorPos - 1);
        yield true;
      }
      case Input.Keys.RIGHT -> {
        cursorPos = Math.min(text.length(), cursorPos + 1);
        yield true;
      }
      case Input.Keys.HOME -> {
        cursorPos = 0;
        yield true;
      }
      case Input.Keys.END -> {
        cursorPos = text.length();
        yield true;
      }
      case Input.Keys.ENTER, Input.Keys.NUMPAD_ENTER -> {
        onConfirm.emit();
        yield true;
      }
      default -> false;
    };
  }

  // -------------------------------------------------------------------------
  // Internal helpers
  // -------------------------------------------------------------------------

  /**
   * Returns the largest suffix of the current text that fits within {@code maxWidth} using the
   * cached {@link #glMeasure} — no object allocation.
   */
  private String visibleText(float maxWidth) {
    String full = text.toString();
    glMeasure.setText(skin.font(), full);
    if (glMeasure.width <= maxWidth) return full;

    int start = 0;
    while (start < full.length()) {
      String sub = full.substring(start);
      glMeasure.setText(skin.font(), sub);
      if (glMeasure.width <= maxWidth) return sub;
      start++;
    }
    return "";
  }

  /**
   * Returns the portion of {@code beforeCursor} that maps to the visible window, used to calculate
   * the cursor X position consistently with {@link #visibleText}.
   */
  private String visibleTextBeforeCursor(String beforeCursor, float maxWidth) {
    String visible = visibleText(maxWidth);
    String full = text.toString();
    int startIndex = 0;
    if (!full.isEmpty() && !visible.isEmpty()) {
      startIndex = full.indexOf(visible);
      if (startIndex < 0) startIndex = 0;
    }
    int visibleCursorPos = MathUtils.clamp(cursorPos - startIndex, 0, visible.length());
    return visible.substring(0, visibleCursorPos);
  }
}
