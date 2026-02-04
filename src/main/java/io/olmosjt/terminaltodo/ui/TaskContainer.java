package io.olmosjt.terminaltodo.ui;

import io.olmosjt.terminaltodo.backend.DataService; // Import added
import io.olmosjt.terminaltodo.backend.Task;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TaskContainer extends VBox {
  private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

  private final Task task;
  private final Runnable onSave;
  private final Runnable onDelete;

  private final HBox rowLayout = new HBox(10);
  private final Label bracketLabel = new Label();
  private final Label timeLabel = new Label();
  private final Label textLabel = new Label();
  private final TextField editField = new TextField();
  private final Label rmLabel = new Label("[rm]");
  private final VBox subTaskContainer = new VBox(2);

  public TaskContainer(Task task, Runnable onSave, Runnable onDelete) {
    this.task = task;
    this.onSave = onSave;
    this.onDelete = onDelete;

    this.getStyleClass().add("task-row");

    setupUI();
    refreshData();
  }

  private void setupUI() {
    this.setSpacing(2);
    rowLayout.setAlignment(Pos.CENTER_LEFT);

    rowLayout.getStyleClass().add("task-line");

    bracketLabel.getStyleClass().add("bracket");
    bracketLabel.setCursor(javafx.scene.Cursor.HAND);
    bracketLabel.setOnMouseClicked(e -> toggleStatus());

    timeLabel.getStyleClass().add("time-stamp");

    textLabel.getStyleClass().add("task-text");
    textLabel.setWrapText(true);
    HBox.setHgrow(textLabel, Priority.ALWAYS);

    textLabel.setOnMouseClicked(e -> {
      if (e.getClickCount() == 2) enableEditMode();
    });

    editField.getStyleClass().add("cmd-input");
    editField.setVisible(false);
    editField.setManaged(false); // Don't take space when hidden
    HBox.setHgrow(editField, Priority.ALWAYS);

    editField.setOnAction(e -> commitEdit());
    editField.setOnKeyPressed(e -> {
      if (e.getCode() == KeyCode.ESCAPE) cancelEdit();
    });

    rmLabel.getStyleClass().add("rm-btn");
    rmLabel.setOnMouseClicked(e -> onDelete.run());

    rowLayout.getChildren().addAll(bracketLabel, timeLabel, textLabel, editField, rmLabel);

    subTaskContainer.setPadding(new javafx.geometry.Insets(0, 0, 5, 65));

    this.getChildren().addAll(rowLayout, subTaskContainer);

    textLabel.maxWidthProperty().bind(Bindings.createDoubleBinding(
        () -> this.getWidth() - 150, this.widthProperty()
    ));
  }

  private void refreshData() {
    textLabel.setText(task.getText());
    textLabel.getStyleClass().removeAll("completed", "priority-low", "priority-normal", "priority-high", "priority-critical");

    if (!task.isDone()) {
      textLabel.getStyleClass().add("priority-" + task.getPriority().name().toLowerCase());
    } else {
      textLabel.getStyleClass().add("completed");
    }

    bracketLabel.setText(task.isDone() ? "[x]" : "[ ]");

    String idPart = task.getId().substring(0, 4);
    String startPart = task.getCreatedAt().format(TIME_FMT);
    String endPart = (task.isDone() && task.getCompletedAt() != null)
        ? " -- " + task.getCompletedAt().format(TIME_FMT)
        : "";
    timeLabel.setText(String.format("[%s][%s%s]", idPart, startPart, endPart));

    subTaskContainer.getChildren().clear();
    for (Task sub : task.getSubTasks()) {
      HBox subRow = new HBox(10);
      subRow.setAlignment(Pos.CENTER_LEFT);
      // CHANGED: Add specific class for hover logic
      subRow.getStyleClass().add("task-line");

      Label sb = new Label(sub.isDone() ? "[x]" : "[ ]");
      sb.getStyleClass().add("bracket");
      sb.setOnMouseClicked(e -> {
        sub.setDone(!sub.isDone());
        onSave.run();
        refreshData();
      });

      Label st = new Label(sub.getText());
      st.getStyleClass().add("task-text");
      if (sub.isDone()) st.getStyleClass().add("completed");

      HBox.setHgrow(st, Priority.ALWAYS);

      Label subRm = new Label("[rm]");
      subRm.getStyleClass().add("rm-btn");
      subRm.setOnMouseClicked(e -> {
        task.getSubTasks().remove(sub);
        new Thread(() -> DataService.deleteTask(sub.getId())).start();
        onSave.run();
        refreshData();
      });

      subRow.getChildren().addAll(sb, st, subRm);
      subTaskContainer.getChildren().add(subRow);
    }
  }

  private void toggleStatus() {
    task.setDone(!task.isDone());
    task.setCompletedAt(task.isDone() ? LocalDateTime.now() : null);
    onSave.run();
    refreshData();
  }

  private void enableEditMode() {
    editField.setText(task.getText());
    textLabel.setVisible(false);
    textLabel.setManaged(false);
    editField.setVisible(true);
    editField.setManaged(true);
    editField.requestFocus();
  }

  private void commitEdit() {
    task.setText(editField.getText());
    cancelEdit(); // Revert UI
    onSave.run();
    refreshData();
  }

  private void cancelEdit() {
    editField.setVisible(false);
    editField.setManaged(false);
    textLabel.setVisible(true);
    textLabel.setManaged(true);
  }
}