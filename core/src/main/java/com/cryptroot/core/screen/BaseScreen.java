package com.cryptroot.core.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.ScreenUtils;
import com.cryptroot.core.GameContext;
import com.cryptroot.core.ui.UiLayer;

/**
 * Abstract base for all screens in any game built on {@code myjourney-core}.
 *
 * <p>Parameterised on {@code C} so each game supplies its own concrete {@link GameContext} subclass
 * without casting.
 *
 * <p>Provides:
 *
 * <ul>
 *   <li>Shared {@link #context} and per-screen {@link #uiLayer}.
 *   <li>A final {@link #render(float)} template: runs the {@code uiLayer.update} early-return
 *       guard, clears the screen, applies the viewport, then calls the abstract {@link
 *       #onRender(float)} hook.
 *   <li>Final {@link #resize(int, int)} — updates the viewport, resizes the selection-outline FBO,
 *       and triggers a UI layout pass.
 *   <li>Final {@link #hide()} — clears the input processor and resets the UI layer. Subclasses may
 *       add teardown via the {@link #onHide()} hook.
 * </ul>
 *
 * <p>Navigation (the "navigator" reference) is intentionally absent from this class. Each game's
 * own intermediate base class adds it with the correct navigator type.
 *
 * @param <C> the concrete {@link GameContext} subclass for this game
 */
public abstract class BaseScreen<C extends GameContext> extends ScreenAdapter {

  protected final C context;
  protected final UiLayer uiLayer;

  protected BaseScreen(C context) {
    this.context = context;
    this.uiLayer = new UiLayer(context.viewport(), context.camera());
  }

  // -------------------------------------------------------------------------
  // Hooks
  // -------------------------------------------------------------------------

  /** The clear colour used at the start of every frame. Default is {@link Color#BLACK}. */
  protected Color clearColor() {
    return Color.BLACK;
  }

  /** Subclasses implement their per-frame rendering logic here. */
  protected abstract void onRender(float delta);

  /** Called after the base {@link #hide()} cleanup. Override for additional teardown. */
  protected void onHide() {}

  // -------------------------------------------------------------------------
  // ScreenAdapter
  // -------------------------------------------------------------------------

  @Override
  public final void render(float delta) {
    if (uiLayer.update(delta)) return;
    ScreenUtils.clear(clearColor());
    context.viewport().apply();
    onRender(delta);
  }

  @Override
  public final void resize(int width, int height) {
    context.viewport().update(width, height, true);
    context.outlineRenderer().resize(context.viewport());
    uiLayer.layout();
  }

  @Override
  public final void hide() {
    Gdx.input.setInputProcessor(null);
    uiLayer.reset();
    onHide();
  }
}
