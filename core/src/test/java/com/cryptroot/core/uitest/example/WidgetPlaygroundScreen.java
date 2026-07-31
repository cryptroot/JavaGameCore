package com.cryptroot.core.uitest.example;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Align;
import com.cryptroot.core.FontSize;
import com.cryptroot.core.screen.BaseScreen;
import com.cryptroot.core.ui.Button;
import com.cryptroot.core.ui.InputField;
import com.cryptroot.core.ui.Panel;
import com.cryptroot.core.ui.ScrollList;
import com.cryptroot.core.ui.Slider;
import com.cryptroot.core.ui.TextLabel;
import com.cryptroot.core.ui.UiSkin;
import com.cryptroot.core.ui.layout.Insets;
import com.cryptroot.core.ui.layout.VStack;
import java.util.ArrayList;
import java.util.List;

/**
 * A one-of-everything screen for the interaction harness's own example test: a click counter, a
 * slider, a scrolling list and a text field.
 *
 * <p>Purpose-built rather than borrowed from a game, because {@code core} is the innermost module
 * and cannot depend on one. Each widget is here to cover a distinct input path — click, drag,
 * scroll, keyboard — so a regression in the harness shows up as a specific failing step.
 */
final class WidgetPlaygroundScreen extends BaseScreen<PlaygroundContext> {

  static final String INCREMENT_LABEL = "Increment";

  /** Enough rows that the list must scroll, so scrolling has an observable effect. */
  private static final int ROW_COUNT = 40;

  private static final float GUTTER = 16f;

  private final UiSkin skin;
  private final Texture pixel;

  private int clicks;

  private TextLabel counterLabel;
  private Button incrementButton;
  private Slider slider;
  private ScrollList list;
  private InputField field;

  WidgetPlaygroundScreen(PlaygroundContext context) {
    super(context);
    this.skin = context.assets().skin(FontSize.BODY);
    this.pixel = context.assets().pixel();
  }

  @Override
  protected void onShow() {
    counterLabel = new TextLabel(skin.font(), counterText()).setBoxAlign(Align.left | Align.center);

    incrementButton = new Button(skin, INCREMENT_LABEL);
    incrementButton.onClick.connect(
        () -> {
          clicks++;
          counterLabel.setText(counterText());
        });

    slider = new Slider(pixel, skin.font(), 0f, 100f, 0f);

    List<String> rows = new ArrayList<>(ROW_COUNT);
    for (int i = 0; i < ROW_COUNT; i++) {
      rows.add("Row " + i);
    }
    list = new ScrollList(skin, pixel, rows);

    field = new InputField(skin, pixel, "type here");

    VStack body =
        new VStack()
            .padding(Insets.all(GUTTER))
            .spacing(GUTTER)
            .stretchCross(true)
            .add(counterLabel)
            .add(incrementButton)
            .add(slider)
            .add(field)
            .add(list, 1f);

    Panel panel = new Panel(pixel);
    panel.setContent(body);
    uiLayer.setRoot(panel);
  }

  private String counterText() {
    return "Clicks: " + clicks;
  }

  // -------------------------------------------------------------------------
  // Observables for the test
  // -------------------------------------------------------------------------

  int clicks() {
    return clicks;
  }

  String counterLabelText() {
    return counterLabel.getText();
  }

  Button incrementButton() {
    return incrementButton;
  }

  Slider slider() {
    return slider;
  }

  ScrollList list() {
    return list;
  }

  InputField field() {
    return field;
  }
}
