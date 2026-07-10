package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;

/**
 * A specialised {@link Button} used as a tab header inside a {@link TabbedPanel}.
 *
 * <h3>Differences from {@code Button}</h3>
 *
 * <ul>
 *   <li><b>No click delay.</b> Tab switches are instant — {@link #hit} fires {@link #onClick}
 *       immediately and returns {@code false} so it does not consume the render frame.
 *   <li><b>Active state.</b> One tab is always "active"; call {@link #setActive(boolean)} to
 *       control the visual. The active tab is drawn with the {@code selectedSlice} nine-patch even
 *       when not hovered.
 *   <li><b>Position is set externally.</b> {@link #setBounds} drives geometry; the label is centred
 *       inside those bounds. No text-baseline constructor parameter is needed — the {@link
 *       TabbedPanel} manages positions during {@code doLayout()}.
 * </ul>
 *
 * <p>Instances are created and owned by {@link TabbedPanel}; they are not intended to be added to a
 * {@link UiLayer} directly.
 */
public final class TabButton extends Button {

  private static final float HOVER_DARKEN = 0.80f; // lighter dimming than regular buttons

  private boolean active = false;
  private float bx, by, bw, bh; // current tab header geometry

  /**
   * @param skin skin shared with the owning {@link TabbedPanel}; provides the nine-patch slices and
   *     font
   * @param label the tab display name
   */
  public TabButton(UiSkin skin, String label) {
    super(skin, label, 0f, 0f); // positions are overwritten by setBoundsFromRect()
  }

  // -------------------------------------------------------------------------
  // External geometry control (called from TabbedPanel.doBoundedLayout)
  // -------------------------------------------------------------------------

  /**
   * Sets the header rect for this tab so {@link TabbedPanel} can drive the layout without requiring
   * a label-baseline metric from the caller.
   */
  public void setTabBounds(float x, float y, float w, float h) {
    bx = x;
    by = y;
    bw = w;
    bh = h;
    bounds.set(x, y, w, h);
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public boolean isActive() {
    return active;
  }

  // -------------------------------------------------------------------------
  // Button overrides
  // -------------------------------------------------------------------------

  /**
   * Positions the contained {@link TextLabel} centred in the current tab header rect. The label
   * baseline is placed so the cap-height is vertically centred.
   */
  @Override
  protected void doBoundedLayout() {
    bounds.set(bx, by, bw, bh);

    // Access labelWidget from the parent via the existing child list.
    // The label was added as the sole child in the super constructor.
    UiWidget child = children().get(0);
    if (child instanceof TextLabel lbl) {
      float centreX = bx + bw / 2f;
      float centreY = by + (bh + skin.font().getCapHeight()) / 2f;
      lbl.setAlign(TextLabel.HAlign.CENTER, 0f);
      lbl.setPosition(centreX, centreY);
      lbl.layout();
    }
  }

  @Override
  protected void doDraw(PolygonSpriteBatch batch) {
    if (active) {
      skin.selectedSlice().draw(batch, bounds.x, bounds.y, bounds.width, bounds.height);
    } else if (hovered) {
      batch.setColor(HOVER_DARKEN, HOVER_DARKEN, HOVER_DARKEN, 1f);
      skin.normalSlice().draw(batch, bounds.x, bounds.y, bounds.width, bounds.height);
      batch.setColor(Color.WHITE);
    } else {
      skin.normalSlice().draw(batch, bounds.x, bounds.y, bounds.width, bounds.height);
    }
    // TextLabel child drawn by the composite delegation in draw()
  }

  /**
   * Instant activation — no click-feedback delay, no frame consumption. The {@link #onClick} signal
   * is emitted here and the method returns {@code false} so the screen's {@code render()} continues
   * normally.
   */
  @Override
  public boolean hit(float worldX, float worldY) {
    if (bounds.contains(worldX, worldY)) {
      onClick.emit();
      return true; // still consume the pointer event
    }
    return false;
  }

  /**
   * No ticking needed — there is no click-feedback timer on tab buttons. Delegate to child widgets
   * only.
   */
  @Override
  public boolean update(float delta) {
    return super.update(delta); // ticks child TextLabel (no-op)
  }
}
