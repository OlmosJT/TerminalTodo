package io.olmosjt.terminaltodo;

import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TaskCell extends ListCell<Task> {
  private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
  private static final DateTimeFormatter DATE_HEADER_FMT = DateTimeFormatter.ofPattern("MMM dd");

  private final VBox container = new VBox(2);
  private final Label dateHeader = new Label();
  private final HBox rowLayout = new HBox(10);
  private final Label bracketLabel = new Label();
  private final Label timeLabel = new Label();
  private final Label textLabel = new Label();
  private final TextField editField = new TextField();
  private final Label rmLabel = new Label("[rm]");
  private final VBox subTaskContainer = new VBox(2);
  private final Runnable onUpdate;

  public TaskCell(Runnable onUpdate) {
    this.onUpdate = onUpdate;
    setupUI();
    setupEditing();
  }

  private void setupUI() {
    rowLayout.setAlignment(Pos.CENTER_LEFT);

    // Horizontal centering for the date header
    dateHeader.getStyleClass().add("date-separator");
    dateHeader.setMaxWidth(Double.MAX_VALUE);
    dateHeader.setAlignment(Pos.CENTER);

    bracketLabel.getStyleClass().add("bracket");
    bracketLabel.setCursor(javafx.scene.Cursor.HAND);
    bracketLabel.setOnMouseClicked(e -> toggleStatus(getItem()));

    timeLabel.getStyleClass().add("time-stamp");
    textLabel.getStyleClass().add("task-text");
    textLabel.setWrapText(true);
    HBox.setHgrow(textLabel, Priority.ALWAYS);

    editField.getStyleClass().add("cmd-input");
    HBox.setHgrow(editField, Priority.ALWAYS);

    rmLabel.getStyleClass().add("rm-btn");
    rmLabel.setOnMouseClicked(e -> {
      getListView().getItems().remove(getItem());
      onUpdate.run();
    });

    rowLayout.getChildren().addAll(bracketLabel, timeLabel, textLabel, rmLabel);
    subTaskContainer.setPadding(new javafx.geometry.Insets(0, 0, 5, 65));

    this.listViewProperty().addListener((obs, oldL, newL) -> {
      if (newL != null) {
        textLabel.maxWidthProperty().bind(Bindings.createDoubleBinding(
            () -> newL.getWidth() - 300, newL.widthProperty()
        ));
      }
    });
  }

  private void toggleStatus(Task t) {
    if (t != null && !t.isMigrated()) {
      t.setDone(!t.isDone());
      t.setCompletedAt(t.isDone() ? LocalDateTime.now() : null);
      onUpdate.run();
      updateItem(getItem(), false);
    }
  }

  private void setupEditing() {
    textLabel.setOnMouseClicked(e -> {
      if (e.getClickCount() == 2 && getItem() != null) {
        editField.setText(getItem().getText());
        rowLayout.getChildren().set(3, editField);
        editField.requestFocus();
      }
    });
    editField.setOnAction(e -> {
      getItem().setText(editField.getText());
      rowLayout.getChildren().set(3, textLabel);
      onUpdate.run();
      updateItem(getItem(), false);
    });
    editField.setOnKeyPressed(e -> {
      if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) rowLayout.getChildren().set(3, textLabel);
    });
  }

  @Override
  protected void updateItem(Task item, boolean empty) {
    super.updateItem(item, empty);
    if (empty || item == null) {
      setGraphic(null);
    } else {
      container.getChildren().clear();
      handleDateSeparator(item);
      container.getChildren().add(rowLayout);

      subTaskContainer.getChildren().clear();
      for (Task sub : item.getSubTasks()) {
        HBox subRow = new HBox(10);
        Label sb = new Label(sub.isDone() ? "[x]" : "[ ]");
        sb.getStyleClass().add("bracket");
        sb.setOnMouseClicked(e -> { toggleStatus(sub); updateItem(item, false); });
        Label st = new Label(sub.getText());
        st.getStyleClass().add("task-text");
        if (!sub.isDone()) st.getStyleClass().add("priority-normal");
        if (sub.isDone()) st.getStyleClass().add("completed");
        subRow.getChildren().addAll(sb, st);
        subTaskContainer.getChildren().add(subRow);
      }
      if (!item.getSubTasks().isEmpty()) container.getChildren().add(subTaskContainer);

      textLabel.setText(item.getText());
      textLabel.getStyleClass().removeAll("completed", "priority-low", "priority-normal", "priority-high", "priority-critical");

      if (!item.isDone()) textLabel.getStyleClass().add("priority-" + item.getPriority().name().toLowerCase());
      bracketLabel.setText(item.isDone() ? "[x]" : "[ ]");
      if (item.isDone()) textLabel.getStyleClass().add("completed");

      // Timestamp Format: [ID][startTime -- completionTime]
      String idPart = item.getId().substring(0, 4);
      String startPart = item.getCreatedAt().format(TIME_FMT);
      String endPart = (item.isDone() && item.getCompletedAt() != null)
          ? " -- " + item.getCompletedAt().format(TIME_FMT)
          : "";

      timeLabel.setText(String.format("[%s][%s%s]", idPart, startPart, endPart));

      setGraphic(container);
    }
  }

  private void handleDateSeparator(Task item) {
    boolean show = getIndex() == 0;
    if (!show && getIndex() > 0) {
      Task prev = getListView().getItems().get(getIndex() - 1);
      if (!prev.getCreatedAt().toLocalDate().isEqual(item.getCreatedAt().toLocalDate())) show = true;
    }
    if (show) {
      dateHeader.setText(item.getCreatedAt().toLocalDate().isEqual(LocalDate.now())
          ? "--- Today ---"
          : "--- " + item.getCreatedAt().format(DATE_HEADER_FMT) + " ---");
      container.getChildren().add(dateHeader);
    }
  }
}