package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.cryptroot.core.event.Signal0;
import com.cryptroot.core.ui.layout.Insets;
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

  /** Scratch for measuring the close button, so layout allocates nothing. */
  private final Vector2 scratch = new Vector2();

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

    titleBarBg = new PixelRect(pixel, TITLE_BAR_BG_COLOR);
    titleLabel = new TextLabel(skin.font(), title, TITLE_COLOR.cpy());
    divider = new PixelBorder(pixel, 1f, DIVIDER_COLOR);
    closeButton = new Button(skin, "Close");
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
   * The title bar plus {@value #CONTENT_PAD} units of padding on every side.
   *
   * <p>Declaring the chrome here is all that is needed for {@link #getContentBounds()} and {@link
   * #preferredSize} to both account for the title bar.
   */
  public static final Insets CHROME =
      new Insets(CONTENT_PAD, CONTENT_PAD, CONTENT_PAD, CONTENT_PAD + TITLE_BAR_H);

  @Override
  protected Insets chromeInsets() {
    return CHROME;
  }

  // -------------------------------------------------------------------------
  // BoundedWidget
  // -------------------------------------------------------------------------

  @Override
  protected void doBoundedLayout() {
    super.doBoundedLayout();

    float titleBarBottomY = frame.y + frame.height - TITLE_BAR_H;

    titleBarBg.setBounds(frame.x, titleBarBottomY, frame.width, TITLE_BAR_H);

    // Title text: vertically centred in the bar by the shared metric helper rather than a
    // hand-tuned baseline nudge.
    titleLabel.setBounds(
        frame.x + TITLE_PAD_H, titleBarBottomY, frame.width - TITLE_PAD_H, TITLE_BAR_H);
    titleLabel.setBoxAlign(Align.left | Align.center);

    divider.setBounds(frame.x, titleBarBottomY, frame.width, 1f);

    // Close button: ask it how big it wants to be, then place that box directly. No need to
    // reverse-engineer the button's internal padding to work back from a text baseline.
    closeButton.preferredSize(scratch);
    closeButton.setBounds(
        frame.x + frame.width - scratch.x - CLOSE_PAD,
        titleBarBottomY + (TITLE_BAR_H - scratch.y) / 2f,
        scratch.x,
        scratch.y);
  }
}
