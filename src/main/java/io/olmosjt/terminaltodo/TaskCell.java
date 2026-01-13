package io.olmosjt.terminaltodo;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TaskCell extends ListCell<Task> {
  private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("HH:mm");

  private final HBox layout;
  private final Label bracketLabel;
  private final Label timeLabel;
  private final Label textLabel;
  private final Label rmLabel;


  private final Runnable onUpdate;

  public TaskCell(Runnable onUpdate) {
    this.onUpdate = onUpdate;

    layout = new HBox(10);
    layout.setAlignment(Pos.CENTER_LEFT);

    bracketLabel = new Label();
    bracketLabel.getStyleClass().add("bracket");

    timeLabel = new Label();
    timeLabel.getStyleClass().add("time-stamp");
    timeLabel.setMinWidth(60);

    textLabel = new Label();
    textLabel.getStyleClass().add("task-text");
    HBox.setHgrow(textLabel, Priority.ALWAYS);

    rmLabel = new Label("[rm]");
    rmLabel.getStyleClass().add("rm-btn");

    rmLabel.setOnMouseClicked(e -> {
      getListView().getItems().remove(getItem());
      onUpdate.run();
    });

    layout.setOnMouseClicked(e -> {
      if (getItem() != null) {
        Task t = getItem();
        t.setDone(!t.isDone());
        t.setCompletedAt(t.isDone() ? LocalDateTime.now() : null);
        updateItem(t, false);
        onUpdate.run();
      }
    });

    layout.getChildren().addAll(bracketLabel, timeLabel, textLabel, rmLabel);
  }

  @Override
  protected void updateItem(Task item, boolean empty) {
    super.updateItem(item, empty);
    if (empty || item == null) {
      setGraphic(null);
    } else {
      textLabel.setText(item.getText());
      bracketLabel.setText(item.isDone() ? "[x]" : "[ ]");

      String timeStr = "[" + item.getCreatedAt().format(DISPLAY_FMT) + "]";
      if (item.isDone() && item.getCompletedAt() != null) {
        timeStr = "[" + item.getCreatedAt().format(DISPLAY_FMT) + " -> " + item.getCompletedAt().format(DISPLAY_FMT) + "]";
      }
      timeLabel.setText(timeStr);

      // Reset styles
      textLabel.getStyleClass().removeAll("completed");
      bracketLabel.getStyleClass().removeAll("completed-bracket");
      timeLabel.getStyleClass().removeAll("completed-time");

      if (item.isDone()) {
        textLabel.getStyleClass().add("completed");
        bracketLabel.getStyleClass().add("completed-bracket");
        timeLabel.getStyleClass().add("completed-time");
      }
      setGraphic(layout);
    }
  }
}
