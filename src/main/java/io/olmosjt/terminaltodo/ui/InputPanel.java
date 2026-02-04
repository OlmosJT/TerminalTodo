package io.olmosjt.terminaltodo.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import java.util.function.Consumer;

public class InputPanel extends HBox {
  private final TextField inputField;
  private final Consumer<String> onCommand;

  public InputPanel(Consumer<String> onCommand) {
    this.onCommand = onCommand;

    this.setSpacing(10);
    this.getStyleClass().add("input-line");
    this.setAlignment(Pos.CENTER_LEFT);

    Label promptChar = new Label("$");
    promptChar.getStyleClass().add("prompt");

    inputField = new TextField();
    inputField.getStyleClass().add("cmd-input");
    inputField.setPromptText("!theme, !report, !clear, !s [ID] [text]");

    inputField.setOnAction(e -> handleInput());

    HBox.setHgrow(inputField, Priority.ALWAYS);
    this.getChildren().addAll(promptChar, inputField);
  }

  private void handleInput() {
    String text = inputField.getText().trim();
    if (!text.isEmpty()) {
      onCommand.accept(text);
      inputField.clear();
    }
  }

  public void requestFocusOnInput() {
    inputField.requestFocus();
  }
}