package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.cryptroot.core.event.Signal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A {@link Panel} with a row of named tabs at the top. Clicking a tab makes that tab's content
 * widgets visible and interactive; all other tabs' widgets are hidden and non-interactive.
 *
 * <h3>Tab strip position</h3>
 *
 * Currently only {@link TabPosition#TOP} and {@link TabPosition#BOTTOM} are supported. Vertical tab
 * strips ({@code LEFT}/{@code RIGHT}) would require rotated text rendering and are deferred; the
 * {@link TabPosition} enum reserves the values for a future implementation.
 *
 * <h3>Layout contract</h3>
 *
 * <ol>
 *   <li>Construct the {@code TabbedPanel} and (optionally) call {@link #setBounds(float, float,
 *       float, float)}.
 *   <li>Call {@link #addTab(String)} for every tab you want.
 *   <li>Call {@link #addWidget(int, UiWidget)} to register each widget to a tab.
 *   <li>Add the panel to a {@link UiLayer} via {@code uiLayer.add(panel, z)} which triggers the
 *       first {@link #layout()} call.
 *   <li>Query {@link #getContentBounds()} <em>after</em> {@code layout()} has been called to obtain
 *       the stable content rectangle for placing widgets.
 * </ol>
 *
 * <h3>Content bounds</h3>
 *
 * {@link #getContentBounds()} returns the inset area below (or above) the tab strip, inside the
 * panel border. Use it to compute positions for the widgets you register via {@link #addWidget(int,
 * UiWidget)}.
 *
 * <h3>Event isolation</h3>
 *
 * Tab content widgets are <em>not</em> registered as {@link CompositeWidget} children. Their
 * lifecycle ({@code layout}, {@code draw}, {@code updateHover}, {@code hit}, {@code dragged},
 * {@code scrolled}, {@code update}, {@code reset}) is driven manually so it can be gated to the
 * active tab only.
 *
 * <pre>{@code
 * TabbedPanel tabs = new TabbedPanel(pixel, skin, 60f, 120f, 1480f, 700f);
 * int tabA = tabs.addTab("Inputs");
 * int tabB = tabs.addTab("Outputs");
 *
 * uiLayer.add(tabs, 0);   // triggers layout()
 *
 * Rectangle content = tabs.getContentBounds();
 * tabs.addWidget(tabA, new Button(skin, "Click me", content.x + 20f, content.y + content.height - 40f));
 * uiLayer.layout();       // re-layout after adding content widgets
 * }</pre>
 */
public final class TabbedPanel extends Panel {

  /**
   * Controls which edge of the panel the tab strip is drawn on.
   *
   * <p><b>Note:</b> {@code LEFT} and {@code RIGHT} are reserved for a future vertical-strip
   * implementation that requires rotated text rendering. Only {@code TOP} and {@code BOTTOM} are
   * currently functional.
   */
  public enum TabPosition {
    TOP,
    BOTTOM
  }

  private static final float TAB_HEIGHT = 36f; // height of the tab strip
  private static final float TAB_PAD_H = 18f; // minimum horizontal padding per tab
  private static final float TAB_GAP = 2f; // horizontal gap between tab buttons
  private static final float CONTENT_PAD = 12f; // inset inside the content area

  /** Fires with the index of the newly active tab whenever the selection changes. */
  public final Signal<Integer> onTabChanged = new Signal<>();

  private final UiSkin skin;
  private final TabPosition tabPosition;

  // Parallel lists — index = tab index.
  private final List<TabButton> tabButtons = new ArrayList<>();
  private final List<List<UiWidget>> tabContents = new ArrayList<>();

  private int activeTab = 0;
  private int hoveredTab = -1; // kept for future fine-grained hover styling

  /** The content widget that consumed the last {@link #hit} call; receives drag events. */
  private UiWidget contentDragTarget = null;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  /**
   * Creates a tabbed panel with the tab strip at the top.
   *
   * @param pixel 1×1 white texture for solid-rect drawing
   * @param skin skin used for tab-button nine-patch and font
   * @param x left edge in world coordinates
   * @param y bottom edge in world coordinates
   * @param w panel width
   * @param h panel height
   */
  public TabbedPanel(Texture pixel, UiSkin skin, float x, float y, float w, float h) {
    this(pixel, skin, x, y, w, h, TabPosition.TOP);
  }

  /**
   * Creates a tabbed panel with the tab strip at the specified position.
   *
   * @param pixel 1×1 white texture for solid-rect drawing
   * @param skin skin used for tab-button nine-patch and font
   * @param x left edge in world coordinates
   * @param y bottom edge in world coordinates
   * @param w panel width
   * @param h panel height
   * @param tabPosition which edge the tab strip appears on ({@code TOP} or {@code BOTTOM})
   */
  public TabbedPanel(
      Texture pixel, UiSkin skin, float x, float y, float w, float h, TabPosition tabPosition) {
    super(pixel, x, y, w, h);
    Objects.requireNonNull(skin, "skin must not be null");
    Objects.requireNonNull(tabPosition, "tabPosition must not be null");
    this.skin = skin;
    this.tabPosition = tabPosition;
  }

  // -------------------------------------------------------------------------
  // Tab management
  // -------------------------------------------------------------------------

  /**
   * Appends a new tab with the given display name and returns its index. The first tab added (index
   * 0) is active by default.
   *
   * <p>Tabs must be added before calling {@link #layout()} — or alternatively call {@link
   * #layout()} again after all tabs are added.
   *
   * @param name display label shown in the tab header
   * @return the index that identifies this tab in subsequent {@link #addWidget(int, UiWidget)} and
   *     {@link #setActiveTab(int)} calls
   */
  public int addTab(String name) {
    int index = tabButtons.size();
    Objects.requireNonNull(name, "name must not be null");
    TabButton btn = new TabButton(skin, name);
    btn.onClick.connect(() -> setActiveTab(index));
    btn.setActive(index == activeTab);
    tabButtons.add(btn);
    tabContents.add(new ArrayList<>());
    addChild(btn); // registered child → participates in standard layout/draw/reset
    return index;
  }

  /**
   * Registers a widget to a specific tab. Widgets are <em>not</em> registered as composite
   * children; their lifecycle is driven manually so it can be gated to the active tab.
   *
   * <p>Call {@link #layout()} (or {@link UiLayer#layout()}) after adding widgets to propagate the
   * layout cascade to the newly registered content.
   *
   * @param tabIndex index returned by {@link #addTab(String)}
   * @param widget the widget to show when that tab is active
   * @throws IndexOutOfBoundsException if {@code tabIndex} is out of range
   */
  public void addWidget(int tabIndex, UiWidget widget) {
    Objects.requireNonNull(widget, "widget must not be null");
    tabContents.get(tabIndex).add(widget);
  }

  /**
   * Switches to the tab at {@code index} and emits {@link #onTabChanged}. A no-op if {@code index}
   * is already the active tab.
   */
  public void setActiveTab(int index) {
    int clamped = MathUtils.clamp(index, 0, tabButtons.size() - 1);
    if (clamped == activeTab) return;
    activeTab = clamped;
    contentDragTarget = null;
    for (int i = 0; i < tabButtons.size(); i++) {
      tabButtons.get(i).setActive(i == activeTab);
    }
    onTabChanged.emit(activeTab);
  }

  /** Returns the index of the currently active tab. */
  public int getActiveTab() {
    return activeTab;
  }

  // -------------------------------------------------------------------------
  // Panel overrides
  // -------------------------------------------------------------------------

  /**
   * Returns the inset rectangle available for content widgets — excludes the tab strip height and
   * the content padding on all sides.
   *
   * <p>This rectangle is stable after {@link #layout()} has been called and is freshly allocated on
   * each invocation (safe to store).
   */
  @Override
  public Rectangle getContentBounds() {
    float px = getPanelX();
    float py = getPanelY();
    float pw = getPanelW();
    float ph = getPanelH();

    float cx = px + CONTENT_PAD;
    float cw = pw - CONTENT_PAD * 2f;

    float cy, ch;
    if (tabPosition == TabPosition.TOP) {
      cy = py + CONTENT_PAD;
      ch = ph - TAB_HEIGHT - CONTENT_PAD * 2f;
    } else { // BOTTOM
      cy = py + TAB_HEIGHT + CONTENT_PAD;
      ch = ph - TAB_HEIGHT - CONTENT_PAD * 2f;
    }
    return new Rectangle(cx, cy, cw, ch);
  }

  // -------------------------------------------------------------------------
  // BoundedWidget / CompositeWidget lifecycle
  // -------------------------------------------------------------------------

  @Override
  protected void doBoundedLayout() {
    super.doBoundedLayout();

    float px = getPanelX();
    float py = getPanelY();
    float pw = getPanelW();
    float ph = getPanelH();

    // Position tab-button rects along the strip edge.
    if (!tabButtons.isEmpty()) {
      float tabW = (pw - TAB_GAP * (tabButtons.size() - 1)) / tabButtons.size();
      float stripY = (tabPosition == TabPosition.TOP) ? py + ph - TAB_HEIGHT : py;
      for (int i = 0; i < tabButtons.size(); i++) {
        float tx = px + i * (tabW + TAB_GAP);
        tabButtons.get(i).setTabBounds(tx, stripY, tabW, TAB_HEIGHT);
      }
    }

    // Layout all content widgets across all tabs (so they are all measured
    // even when not visible — safe because layout is side-effect-free).
    for (List<UiWidget> tab : tabContents) {
      for (UiWidget w : tab) {
        w.layout();
      }
    }
  }

  // -------------------------------------------------------------------------
  // Gated interaction — active tab only
  // -------------------------------------------------------------------------

  @Override
  public void updateHover(float worldX, float worldY) {
    // Update tab-button hover (registered children — handled by super).
    super.updateHover(worldX, worldY);

    // Update content widgets for the active tab only.
    if (!tabContents.isEmpty()) {
      for (UiWidget w : tabContents.get(activeTab)) {
        w.updateHover(worldX, worldY);
      }
    }
  }

  @Override
  public boolean hit(float worldX, float worldY) {
    // Test tab buttons directly — must NOT call super.hit() here because
    // Panel.hit() falls through to the opaque-sink check, which would swallow
    // every click inside the panel before content widgets are tested.
    for (int i = tabButtons.size() - 1; i >= 0; i--) {
      if (tabButtons.get(i).hit(worldX, worldY)) return true;
    }

    // Content widgets for the active tab only.
    if (!tabContents.isEmpty()) {
      List<UiWidget> active = tabContents.get(activeTab);
      for (int i = active.size() - 1; i >= 0; i--) {
        UiWidget w = active.get(i);
        if (w.hit(worldX, worldY)) {
          contentDragTarget = w;
          return true;
        }
      }
    }
    contentDragTarget = null;

    // Opaque-sink fallback — absorb the click without acting on it.
    return isOpaque() && bounds.contains(worldX, worldY);
  }

  /**
   * Delegates to {@code contentDragTarget.hitFocusable()} so that a {@link Focusable} widget nested
   * inside this panel (e.g., an {@link InputField}) receives keyboard focus after being clicked.
   */
  @Override
  public Focusable hitFocusable() {
    return contentDragTarget != null ? contentDragTarget.hitFocusable() : null;
  }

  @Override
  public void dragged(float worldX, float worldY) {
    if (contentDragTarget != null) {
      contentDragTarget.dragged(worldX, worldY);
    }
  }

  @Override
  public boolean scrolled(float worldX, float worldY, float amountX, float amountY) {
    if (!tabContents.isEmpty()) {
      List<UiWidget> active = tabContents.get(activeTab);
      for (int i = active.size() - 1; i >= 0; i--) {
        if (active.get(i).scrolled(worldX, worldY, amountX, amountY)) return true;
      }
    }
    return false;
  }

  @Override
  public boolean update(float delta) {
    // Update registered children (tab buttons, bg, border).
    if (super.update(delta)) return true;
    // Tick active tab content.
    if (!tabContents.isEmpty()) {
      for (UiWidget w : tabContents.get(activeTab)) {
        if (w.update(delta)) return true;
      }
    }
    return false;
  }

  /** Draws active-tab content widgets after all registered children. */
  @Override
  protected void doAfterDraw(PolygonSpriteBatch batch) {
    if (!tabContents.isEmpty()) {
      for (UiWidget w : tabContents.get(activeTab)) {
        w.draw(batch);
      }
    }
  }

  /**
   * Resets all tabs (not just the active one) so that hidden widgets — e.g., an {@link InputField}
   * with keyboard focus in an inactive tab — are also cleared on screen hide.
   */
  @Override
  protected void doBoundedReset() {
    contentDragTarget = null;
    for (List<UiWidget> tab : tabContents) {
      for (UiWidget w : tab) {
        w.reset();
      }
    }
  }
}
