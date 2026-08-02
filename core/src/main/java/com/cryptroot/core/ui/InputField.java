package com.cryptroot.core.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.cryptroot.core.event.Signal;
import com.cryptroot.core.event.Signal0;
import java.util.Objects;

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

  /** Scratch for measuring, so layout allocates nothing. */
  private final Vector2 scratch = new Vector2();

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

  /**
   * Y the text is drawn at, resolved in {@link #doBoundedLayout()} — the <em>top</em> of the cap
   * band, which is the coordinate {@code BitmapFont.draw} takes. Stored rather than recomputed in
   * {@code doAfterDraw} so the caret cannot drift away from the glyphs it sits among.
   */
  private float textBaseline;

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
    Objects.requireNonNull(skin, "skin must not be null");
    Objects.requireNonNull(pixel, "pixel must not be null");
    Objects.requireNonNull(placeholder, "placeholder must not be null");
    this.skin = skin;
    this.pixel = pixel;
    setBounds(x, y, width, UiHelper.barHeight(skin.font(), PADDING_V));

    visibleLabel = new TextLabel(skin.font(), "", 0f, 0f);
    placeholderLabel = new TextLabel(skin.font(), placeholder, 0f, 0f, COLOR_PLACEHOLDER);
    // Not registered as children — drawn manually in doDraw() so we control
    // exactly when each appears and the cursor is layered above in doAfterDraw().
  }

  /** Creates a field sized by its enclosing layout container. */
  public InputField(UiSkin skin, Texture pixel, String placeholder) {
    this(skin, pixel, 0f, 0f, 0f, placeholder);
    setBounds(0f, 0f, 0f, 0f);
  }

  /**
   * Natural size: the theme's minimum control width by one standard text bar. A field has no
   * content width of its own — it is meant to be stretched by its container.
   */
  @Override
  public Vector2 preferredSize(Vector2 out) {
    return out.set(skin.theme().minControlWidth(), UiHelper.barHeight(skin.font(), PADDING_V));
  }

  public String getText() {
    return text.toString();
  }

  /** Replaces current text and places the cursor at the end. */
  public void setText(String newText) {
    Objects.requireNonNull(newText, "newText must not be null");
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
    if (frame.width <= 0f || frame.height <= 0f) {
      Vector2 natural = preferredSize(scratch);
      if (frame.width <= 0f) frame.width = natural.x;
      if (frame.height <= 0f) frame.height = natural.y;
    }
    bounds.set(frame);

    fieldHeight = frame.height;
    textAreaX = frame.x + PADDING_H;
    textAreaW = Math.max(0f, frame.width - PADDING_H * 2f - CURSOR_WIDTH);

    // Vertically centre the text in the field via the shared metric helper.
    textBaseline =
        UiHelper.baselineIn(frame.y, frame.height, skin.font().getCapHeight(), Align.center);

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

    // The caret must span the same band as the glyphs: [baseline - capHeight, baseline]. batch.draw
    // grows upwards from the y it is given, while textBaseline is the *top* of the cap band, so the
    // rectangle starts a cap height below it. Passing textBaseline directly drew the caret one
    // whole
    // cap height above the text.
    float capHeight = skin.font().getCapHeight();
    batch.setColor(COLOR_CURSOR);
    batch.draw(pixel, cursorX, textBaseline - capHeight, CURSOR_WIDTH, capHeight);
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
   * Returns the index of the first visible character: the smallest {@code start} for which the
   * suffix {@code text[start..]} fits within {@code maxWidth}.
   *
   * <p>Found by binary search over the start index. The previous linear scan re-measured a fresh
   * suffix for every character it skipped, making this O(n) glyph layouts per call — and it was
   * called twice per frame, so a long value cost hundreds of text layouts per frame purely to draw
   * one field. Suffix width shrinks monotonically as {@code start} rises, so bisection is valid and
   * costs O(log n).
   */
  private int visibleStartIndex(float maxWidth) {
    String full = text.toString();
    glMeasure.setText(skin.font(), full);
    if (glMeasure.width <= maxWidth) return 0;

    int lo = 0;
    int hi = full.length(); // the empty suffix always fits
    while (lo < hi) {
      int mid = (lo + hi) >>> 1;
      glMeasure.setText(skin.font(), full.substring(mid));
      if (glMeasure.width <= maxWidth) {
        hi = mid; // fits — try to keep more characters
      } else {
        lo = mid + 1;
      }
    }
    return lo;
  }

  /** The largest suffix of the current text that fits within {@code maxWidth}. */
  private String visibleText(float maxWidth) {
    return text.substring(visibleStartIndex(maxWidth));
  }

  /**
   * Returns the portion of the visible window that precedes the cursor, used to place the caret
   * consistently with {@link #visibleText}.
   */
  private String visibleTextBeforeCursor(String beforeCursor, float maxWidth) {
    int startIndex = visibleStartIndex(maxWidth);
    int visibleLength = text.length() - startIndex;
    int visibleCursorPos = MathUtils.clamp(cursorPos - startIndex, 0, visibleLength);
    return text.substring(startIndex, startIndex + visibleCursorPos);
  }
}
