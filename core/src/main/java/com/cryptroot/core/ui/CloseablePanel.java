package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Rectangle;
import com.cryptroot.core.event.Signal0;
import java.util.Objects;

/**
 * A {@link Panel} with a title bar and a close ("×") button in the top-right corner.
 *
 * <p>The panel starts <em>invisible</em> ({@link #setVisible(boolean) setVisible(false)}). Call
 * {@link #open()} to show it and {@link #close()} to hide it. The built-in close button fires
 * {@link #onClose} when clicked; {@code onClose} is pre-wired to call {@link #close()} so callers
 * do not need to connect that themselves.
 *
 * <h3>Layout</h3>
 *
 * <pre>
 * ┌──────────────────────────────────────────────────────────┐  y + h  (top)
 * │  Title                                              [×]  │  ← title bar (TITLE_BAR_H tall)
 * ├──────────────────────────────────────────────────────────┤  y + h – TITLE_BAR_H
 * │                                                          │
 * │         content area  (see {@link #getContentBounds()})  │
 * │                                                          │
 * └──────────────────────────────────────────────────────────┘  y  (bottom)
 * </pre>
 *
 * <h3>Nesting</h3>
 *
 * Sub-menus can be built by placing another {@code CloseablePanel} on top of this one (higher draw
 * order in the owning {@link TabbedPanel} content list) and wiring a trigger button inside this
 * panel to call {@link #open()} on the sub-panel.
 *
 * <h3>Usage</h3>
 *
 * <pre>{@code
 * CloseablePanel panel = new CloseablePanel(pixel, skin, "Audio", 300f, 120f, 1200f, 690f);
 * Rectangle cb = panel.getContentBounds();
 * panel.addWidget(new Slider(pixel, font, cb.x, cb.y + cb.height - 60f, cb.width, 0, 100, 70));
 * tabbedPanel.addWidget(tabId, panel);
 *
 * Button openBtn = new Button(skin, "Open Audio", 80f, 500f);
 * openBtn.onClick.connect(panel::open);
 * }</pre>
 */
public class CloseablePanel extends Panel {

  // -------------------------------------------------------------------------
  // Layout constants
  // -------------------------------------------------------------------------

  private static final float TITLE_BAR_H = 40f;
  private static final float TITLE_PAD_H = 14f; // left inset for title text
  private static final float CLOSE_PAD = 8f; // right inset for × button
  private static final float CONTENT_PAD = 12f; // inset for the content area

  // -------------------------------------------------------------------------
  // Visual constants
  // -------------------------------------------------------------------------

  private static final Color TITLE_BAR_BG_COLOR = new Color(0.12f, 0.12f, 0.22f, 0.98f);
  private static final Color DIVIDER_COLOR = new Color(0.35f, 0.35f, 0.50f, 1f);
  private static final Color TITLE_COLOR = new Color(0.85f, 0.85f, 1.0f, 1f);

  // -------------------------------------------------------------------------
  // Public signal
  // -------------------------------------------------------------------------

  /**
   * Fires when the built-in "×" close button is clicked. Also fires when {@link #close()} is called
   * programmatically (callers may connect additional listeners here).
   *
   * <p>The panel's own {@link #close()} method is pre-wired; no need to connect that separately.
   */
  public final Signal0 onClose = new Signal0();

  // -------------------------------------------------------------------------
  // Children
  // -------------------------------------------------------------------------

  private final PixelRect titleBarBg;
  private final TextLabel titleLabel;
  private final PixelBorder divider;
  private final Button closeButton;

  // Stored for getContentBounds() and doBoundedLayout() —
  // values are set once at construction and do not change.
  private final float px;
  private final float py;
  private final float pw;
  private final float ph;

  // -------------------------------------------------------------------------
  // Constructor
  // -------------------------------------------------------------------------

  /**
   * Creates a closeable panel at the given world-space position and size. The panel starts hidden;
   * call {@link #open()} to show it.
   *
   * @param pixel 1×1 white texture for solid-rect drawing
   * @param skin skin used for the title font and the close button nine-patch
   * @param title text shown in the title bar
   * @param x left edge in world coordinates
   * @param y bottom edge in world coordinates
   * @param w panel width
   * @param h panel height
   */
  public CloseablePanel(
      Texture pixel, UiSkin skin, String title, float x, float y, float w, float h) {
    super(Objects.requireNonNull(pixel, "pixel must not be null"), x, y, w, h);
    Objects.requireNonNull(skin, "skin must not be null");
    Objects.requireNonNull(title, "title must not be null");
    this.px = x;
    this.py = y;
    this.pw = w;
    this.ph = h;

    titleBarBg = new PixelRect(pixel, TITLE_BAR_BG_COLOR);
    titleLabel = new TextLabel(skin.font(), title, 0f, 0f, TITLE_COLOR);
    divider = new PixelBorder(pixel, 1f, DIVIDER_COLOR);
    // Close button constructed at placeholder (0, 0); repositioned in doBoundedLayout().
    closeButton = new Button(skin, "Close", 0f, 0f);
    closeButton.setLabelColour(Color.BLACK.cpy());

    // Wire close button → onClose signal → hide this panel.
    closeButton.onClick.connect(onClose::emit);
    onClose.connect(this::close);

    // titleBarBg must be first so it renders behind title text and close button.
    addWidget(titleBarBg);
    addWidget(titleLabel);
    addWidget(divider);
    addWidget(closeButton);

    setOpaque(true); // absorb all clicks inside bounds
    setVisible(false); // hidden until open() is called
  }

  // -------------------------------------------------------------------------
  // Public API
  // -------------------------------------------------------------------------

  /** Shows the panel. Equivalent to {@code setVisible(true)}. */
  public void open() {
    setVisible(true);
  }

  /** Hides the panel and fires {@link #onClose}. Equivalent to {@code setVisible(false)}. */
  public void close() {
    setVisible(false);
  }

  /** Updates the text shown in the title bar. */
  public void setTitle(String title) {
    Objects.requireNonNull(title, "title must not be null");
    titleLabel.setText(title);
  }

  /**
   * Returns the inset rectangle available for content widgets — the area below the title bar with
   * {@value #CONTENT_PAD}px padding on all sides.
   *
   * <p>The returned rectangle is freshly allocated on every call; it is safe to store without
   * aliasing concerns.
   */
  @Override
  public Rectangle getContentBounds() {
    return new Rectangle(
        px + CONTENT_PAD,
        py + CONTENT_PAD,
        pw - CONTENT_PAD * 2f,
        ph - TITLE_BAR_H - CONTENT_PAD * 2f);
  }

  // -------------------------------------------------------------------------
  // BoundedWidget
  // -------------------------------------------------------------------------

  @Override
  protected void doBoundedLayout() {
    super.doBoundedLayout();

    float titleBarBottomY = py + ph - TITLE_BAR_H;
    float titleBarCenterY = py + ph - TITLE_BAR_H / 2f;

    // Title bar background
    titleBarBg.setBounds(px, titleBarBottomY, pw, TITLE_BAR_H);

    // Title label — baseline roughly centred in the title bar
    titleLabel.setPosition(px + TITLE_PAD_H, titleBarCenterY + 10f);

    // Divider line at the bottom edge of the title bar
    divider.setBounds(px, titleBarBottomY, pw, 1f);

    // Close button: reset to the placeholder origin so every layout pass
    // measures from (0, 0) — without this the second call (fired by the
    // initial LibGDX resize event) would measure from the already-placed
    // position and push the button to (0, 0) / off-screen.
    closeButton.setPosition(0f, 0f);
    closeButton.layout();
    Rectangle cb = closeButton.getBounds(); // measured at textX=0, textY=0
    // cb.x = textX - PAD_H = -PAD_H  →  textX_new = targetBoundsX - cb.x
    float targetBoundsX = px + pw - cb.width - CLOSE_PAD;
    float targetTextX = targetBoundsX - cb.x;
    // cb.y = textY - glH - PAD_V_BOT  →  textY_new = targetBoundsY - cb.y
    float targetBoundsY = titleBarCenterY - cb.height / 2f;
    float targetTextY = targetBoundsY - cb.y;
    closeButton.setPosition(targetTextX, targetTextY);
    // CompositeWidget.layout() will call closeButton.layout() again after
    // doBoundedLayout() returns, applying the new position.
  }
}
