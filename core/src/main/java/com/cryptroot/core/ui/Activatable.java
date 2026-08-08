package com.cryptroot.core.ui;

/**
 * A widget that can be keyboard-selected and activated by a {@link NavigationGroup}, independent of
 * pointer hover.
 *
 * <p>{@link #setSelected(boolean)} controls a purely visual "this is the current keyboard choice"
 * state — distinct from a {@link BoundedWidget#hovered} pointer-hover state, since a {@link
 * NavigationGroup} does not synchronise with the mouse (see its class Javadoc). {@link #activate()}
 * performs the same action a pointer click would, e.g. {@link Button#triggerClick()}.
 */
public interface Activatable {

  /** Sets whether this widget renders as the current keyboard selection. */
  void setSelected(boolean selected);

  /** Performs this widget's action, as if it had been clicked. */
  void activate();
}
