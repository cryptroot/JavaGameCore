package com.cryptroot.core.world;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Supplies the text shown when the pointer rests on this entity.
 *
 * <p>The component only answers "what would you say?"; nothing about <em>where</em> or <em>how</em>
 * a tooltip is drawn lives here. Presentation belongs to whatever is displaying the world — {@link
 * com.cryptroot.core.ui.WorldViewport} renders it as a UI-space box near the cursor, using the
 * enclosing skin's font, so tooltips stay legible at any zoom instead of scaling with the scene.
 *
 * <p>Returning {@code null} suppresses the tooltip for that frame, which is how a conditional
 * tooltip ("locked until you own the lease") turns itself off.
 */
public interface TooltipComponent extends EntityComponent {

  /** The tooltip text, or {@code null} for none right now. */
  String tooltip();

  /** A fixed tooltip. */
  static TooltipComponent of(String text) {
    return () -> text;
  }

  /** A tooltip recomputed on every query, for text that depends on live game state. */
  static TooltipComponent of(Supplier<String> supplier) {
    Objects.requireNonNull(supplier, "supplier must not be null");
    return supplier::get;
  }
}
