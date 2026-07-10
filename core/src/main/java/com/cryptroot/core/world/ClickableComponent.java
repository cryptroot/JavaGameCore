package com.cryptroot.core.world;

import com.cryptroot.core.event.Signal0;

/**
 * Component that makes an entity respond to pointer interaction.
 *
 * <p>Requires a {@link BoundsComponent} on the same entity for hit-testing. {@link
 * WorldEntityLayer} automatically fires the hover signals on enter/exit and {@link #onClicked()}
 * when the entity is clicked.
 */
public interface ClickableComponent extends EntityComponent {
  Signal0 onClicked();

  Signal0 onHoverEnter();

  Signal0 onHoverExit();
}
