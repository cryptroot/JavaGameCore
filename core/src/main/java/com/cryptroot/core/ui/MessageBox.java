package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.utils.Align;
import com.cryptroot.core.dialogue.Speaker;

/**
 * A rectangular display widget that shows the current speaker name and body text for a dialogue
 * line. Body text wraps automatically at the box edge.
 *
 * <p>{@code MessageBox} is display-only: {@link #hit} always returns {@code false} and {@link
 * #update} always returns {@code false}. Interaction (advancing lines, resetting) belongs to the
 * owning {@link ConversationWidget}.
 *
 * <h3>Layout</h3>
 *
 * <pre>
 * ┌──────────────────────────────────────────────┐  top of box
 * │  SpeakerName                                 │  NAME_SECTION_H tall
 * ├──────────────────────────────────────────────┤  divider line
 * │                                              │
 * │  Body text (left-aligned, wrapped)           │
 * │                                              │
 * │                              ▼ click to continue │  (click indicator)
 * └──────────────────────────────────────────────┘
 * </pre>
 *
 * <h3>Visibility</h3>
 *
 * The box is invisible until {@link #setContent(Speaker, String)} is called. Call {@link
 * #clearContent()} (or {@link #reset()}) to hide it again.
 *
 * <h3>Factory method — full-width VN style</h3>
 *
 * Use {@link #fullWidth(Texture, BitmapFont, float, float)} to create a message box that spans the
 * full world width at a fixed Y position, as seen in visual-novel-style presentations.
 */
public final class MessageBox implements UiWidget {

  // -------------------------------------------------------------------------
  // Layout constants
  // -------------------------------------------------------------------------

  private static final float PAD_H = 20f; // horizontal padding
  private static final float PAD_TOP = 14f; // inside top edge
  private static final float PAD_BOT = 14f; // inside bottom edge
  private static final float NAME_SECTION_H = 52f; // height reserved for the name row
  private static final float DIVIDER_H = 1f; // divider line height

  // -------------------------------------------------------------------------
  // Visual constants
  // -------------------------------------------------------------------------

  private static final Color BG_COLOR = new Color(0.05f, 0.05f, 0.10f, 0.92f);
  private static final Color BORDER_COLOR = new Color(0.55f, 0.55f, 0.75f, 1f);
  private static final Color NAME_COLOR = new Color(1.00f, 0.85f, 0.50f, 1f);
  private static final Color BODY_COLOR = Color.WHITE;
  private static final Color HINT_COLOR = new Color(0.60f, 0.60f, 0.65f, 1f);
  private static final Color DIVIDER_COLOR = new Color(0.35f, 0.35f, 0.50f, 1f);

  private static final String CLICK_HINT = "\u25bc  click to continue";

  // -------------------------------------------------------------------------
  // Fields
  // -------------------------------------------------------------------------

  private final Texture pixel;
  private final BitmapFont font; // used for both name and body

  private float bx, by, bw, bh;

  // Cached layout positions — recomputed in layout()
  private float nameY;
  private float dividerY;
  private float bodyY;
  private float bodyWrapWidth;
  private float hintX;
  private float hintY;

  // Reusable GlyphLayout for hint width measurement
  private final GlyphLayout hintLayout = new GlyphLayout();

  private boolean visible = false;
  private String speakerName = "";
  private String bodyText = "";

  // -------------------------------------------------------------------------
  // Constructors / factory
  // -------------------------------------------------------------------------

  public MessageBox(Texture pixel, BitmapFont font, float x, float y, float w, float h) {
    this.pixel = pixel;
    this.font = font;
    setBounds(x, y, w, h);
  }

  /**
   * Factory: creates a message box spanning the full world width ({@code 1600} units) at a fixed
   * {@code y} position with the given height.
   *
   * @param pixel 1×1 white pixel texture
   * @param font font for name and body text
   * @param y bottom edge in world coordinates
   * @param h height of the message box
   * @return a new {@code MessageBox} ready to be added to a {@link UiLayer} or {@link
   *     ConversationWidget}
   */
  public static MessageBox fullWidth(Texture pixel, BitmapFont font, float y, float h) {
    return new MessageBox(pixel, font, 0f, y, 1600f, h);
  }

  // -------------------------------------------------------------------------
  // Public API
  // -------------------------------------------------------------------------

  /**
   * Repositions and resizes the box. Call {@link #layout()} (or let the owning {@link
   * ConversationWidget} trigger it) after changing bounds.
   */
  public void setBounds(float x, float y, float w, float h) {
    this.bx = x;
    this.by = y;
    this.bw = w;
    this.bh = h;
  }

  /**
   * Shows the message box with the given speaker and line of text.
   *
   * @param speaker the active speaker (provides the display name)
   * @param text body text to display (will be word-wrapped)
   */
  public void setContent(Speaker speaker, String text) {
    this.speakerName = speaker.name();
    this.bodyText = text;
    this.visible = true;
  }

  /** Hides the message box and clears all content. Equivalent to {@link #reset()}. */
  public void clearContent() {
    speakerName = "";
    bodyText = "";
    visible = false;
  }

  public boolean isVisible() {
    return visible;
  }

  // -------------------------------------------------------------------------
  // UiWidget — non-interactive
  // -------------------------------------------------------------------------

  @Override
  public void layout() {
    // Name label sits at the top of the box, baseline just below the top edge.
    nameY = by + bh - PAD_TOP - 8f; // baseline; ascent goes upward from here

    // Divider sits below the name section.
    dividerY = by + bh - NAME_SECTION_H;

    // Body text baseline starts just below the divider.
    bodyY = dividerY - PAD_TOP;
    bodyWrapWidth = bw - PAD_H * 2f;

    // Click hint: right-aligned inside the box, near the bottom.
    hintLayout.setText(font, CLICK_HINT);
    hintX = bx + bw - PAD_H - hintLayout.width;
    hintY = by + PAD_BOT + hintLayout.height;
  }

  @Override
  public void updateHover(float worldX, float worldY) {}

  @Override
  public boolean hit(float worldX, float worldY) {
    return false;
  }

  @Override
  public boolean update(float delta) {
    return false;
  }

  @Override
  public void reset() {
    clearContent();
  }

  @Override
  public void draw(PolygonSpriteBatch batch) {
    if (!visible) return;

    // Background
    batch.setColor(BG_COLOR);
    batch.draw(pixel, bx, by, bw, bh);
    batch.setColor(Color.WHITE);

    // Border
    drawBorder(batch);

    // Divider line
    batch.setColor(DIVIDER_COLOR);
    batch.draw(pixel, bx, dividerY, bw, DIVIDER_H);
    batch.setColor(Color.WHITE);

    // Speaker name
    font.setColor(NAME_COLOR);
    font.draw(batch, speakerName, bx + PAD_H, nameY);

    // Body text (wrapped)
    font.setColor(BODY_COLOR);
    font.draw(batch, bodyText, bx + PAD_H, bodyY, bodyWrapWidth, Align.left, true);

    // Click hint
    font.setColor(HINT_COLOR);
    font.draw(batch, CLICK_HINT, hintX, hintY);

    font.setColor(Color.WHITE);
  }

  // -------------------------------------------------------------------------
  // Private helpers
  // -------------------------------------------------------------------------

  private void drawBorder(PolygonSpriteBatch batch) {
    float t = 1f; // border thickness
    batch.setColor(BORDER_COLOR);
    // bottom
    batch.draw(pixel, bx, by, bw, t);
    // top
    batch.draw(pixel, bx, by + bh - t, bw, t);
    // left
    batch.draw(pixel, bx, by, t, bh);
    // right
    batch.draw(pixel, bx + bw - t, by, t, bh);
    batch.setColor(Color.WHITE);
  }
}
