package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;

/**
 * A small, modal-style alert dialog with a title bar, a body message, and a built-in close button.
 *
 * <p>Extends {@link CloseablePanel} — the title bar, divider, and close button are inherited. The
 * body area contains a single, horizontally-centred {@link TextLabel} whose text is set at {@link
 * #show} time.
 *
 * <h3>Usage</h3>
 *
 * <pre>{@code
 * AlertDialog alert = AlertDialog.centered(pixel, skin, font, WORLD_W, WORLD_H);
 * uiLayer.add(alert, 20);  // high z-order so it floats above other panels
 *
 * someButton.onClick.connect(() -> alert.show("Medical Bay", "Coming soon!"));
 * }</pre>
 */
public final class AlertDialog extends CloseablePanel {

  // Default dimensions for the centered factory.
  private static final float DEFAULT_W = 560f;
  private static final float DEFAULT_H = 180f;

  // Approximate half-cap-height of a BODY-size font (~34 px), used to
  // nudge the baseline so the text appears vertically centred in the
  // content area.
  private static final float BODY_HALF_CAP = 17f;

  private final TextLabel messageLabel;

  // -------------------------------------------------------------------------
  // Constructor
  // -------------------------------------------------------------------------

  /**
   * Creates an alert dialog at the given world-space position and size. The dialog starts hidden;
   * call {@link #show(String, String)} to display it.
   *
   * @param pixel 1×1 white texture for solid-rect drawing
   * @param skin skin used by the inherited title bar and close button
   * @param font font used for the body message
   * @param x left edge in world coordinates
   * @param y bottom edge in world coordinates
   * @param w dialog width
   * @param h dialog height
   */
  public AlertDialog(
      Texture pixel, UiSkin skin, BitmapFont font, float x, float y, float w, float h) {
    super(pixel, skin, "", x, y, w, h);
    Rectangle cb = getContentBounds();
    messageLabel = new TextLabel(font, "", cb.x, cb.y + cb.height / 2f + BODY_HALF_CAP);
    messageLabel.setAlign(TextLabel.HAlign.CENTER, cb.width);
    addWidget(messageLabel);
  }

  // -------------------------------------------------------------------------
  // Factory
  // -------------------------------------------------------------------------

  /**
   * Creates an alert dialog centred within the given world dimensions using the default size
   * ({@value #DEFAULT_W} × {@value #DEFAULT_H}).
   */
  public static AlertDialog centered(
      Texture pixel, UiSkin skin, BitmapFont font, float worldW, float worldH) {
    float x = (worldW - DEFAULT_W) / 2f;
    float y = (worldH - DEFAULT_H) / 2f;
    return new AlertDialog(pixel, skin, font, x, y, DEFAULT_W, DEFAULT_H);
  }

  // -------------------------------------------------------------------------
  // Public API
  // -------------------------------------------------------------------------

  /**
   * Sets the title and body message, then opens the dialog.
   *
   * @param title text shown in the title bar
   * @param message body text shown in the content area
   */
  public void show(String title, String message) {
    setTitle(title);
    messageLabel.setText(message);
    open();
  }
}
