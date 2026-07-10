package com.cryptroot.core.world.component;

import com.cryptroot.core.event.Signal0;
import com.cryptroot.core.world.ClickableComponent;

/**
 * Default implementation of {@link ClickableComponent} using three {@link Signal0} instances.
 *
 * <p>Connect behaviour at scene-construction time:
 *
 * <pre>{@code
 * WorldEntity florence = new WorldEntity()
 *     .with(SpineRenderComponent.class, spineComp)
 *     .with(BoundsComponent.class,      boundsComp)
 *     .with(ClickableComponent.class,   new DefaultClickableComponent());
 *
 * florence.get(ClickableComponent.class)
 *         .ifPresent(c -> c.onClicked().connect(conversation::start));
 * }</pre>
 */
public final class DefaultClickableComponent implements ClickableComponent {

  private final Signal0 onClicked = new Signal0();
  private final Signal0 onHoverEnter = new Signal0();
  private final Signal0 onHoverExit = new Signal0();

  @Override
  public Signal0 onClicked() {
    return onClicked;
  }

  @Override
  public Signal0 onHoverEnter() {
    return onHoverEnter;
  }

  @Override
  public Signal0 onHoverExit() {
    return onHoverExit;
  }
}
