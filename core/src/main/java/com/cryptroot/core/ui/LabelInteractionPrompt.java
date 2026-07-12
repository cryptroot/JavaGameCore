package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.cryptroot.core.dialogue.InteractionPrompt;
import java.util.Objects;

/**
 * A non-interactive, toggleable text hint backing an {@link InteractionPrompt}.
 *
 * <p>Reuses {@link TextLabel} for rendering (no duplicated glyph layout) and adds simple show/hide
 * visibility so an {@code AmbientConversationDirector} can flash a line such as <em>"Click on Laura
 * to chat"</em> without grabbing input — {@link #hit} always returns {@code false}, so clicks pass
 * straight through to the game beneath.
 *
 * <p>Add it to a {@link UiLayer}; it draws only while shown.
 */
public final class LabelInteractionPrompt implements UiWidget, InteractionPrompt {

  private final TextLabel label;
  private boolean visible = false;

  /**
   * @param font font for the hint text
   * @param centerX X around / within which the text is centred
   * @param baselineY text baseline Y (world units)
   * @param regionWidth width to centre within; pass {@code 0} to centre around {@code centerX}
   */
  public LabelInteractionPrompt(
      BitmapFont font, float centerX, float baselineY, float regionWidth) {
    Objects.requireNonNull(font, "font must not be null");
    this.label =
        new TextLabel(font, "", centerX, baselineY).setAlign(TextLabel.HAlign.CENTER, regionWidth);
  }

  /** Sets the hint colour. Returns {@code this} for chaining. */
  public LabelInteractionPrompt setColor(Color color) {
    Objects.requireNonNull(color, "color must not be null");
    label.setColor(color);
    return this;
  }

  // ---- InteractionPrompt ----

  @Override
  public void show(String text) {
    Objects.requireNonNull(text, "text must not be null");
    label.setText(text);
    label.layout();
    visible = true;
  }

  @Override
  public void hide() {
    visible = false;
  }

  // ---- UiWidget (display-only, never consumes input) ----

  @Override
  public boolean isVisible() {
    return visible;
  }

  @Override
  public void layout() {
    label.layout();
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
    visible = false;
  }

  @Override
  public void draw(PolygonSpriteBatch batch) {
    if (visible) label.draw(batch);
  }
}
