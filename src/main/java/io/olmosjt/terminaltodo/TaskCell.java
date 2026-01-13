package io.olmosjt.terminaltodo;

import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TaskCell extends ListCell<Task> {
  private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
  private static final DateTimeFormatter DATE_HEADER_FMT = DateTimeFormatter.ofPattern("MMM dd");

  private final VBox container;
  private final Label dateHeader;
  private final HBox rowLayout;
  private final Label bracketLabel;
  private final Label timeLabel;
  private final Label textLabel;
  private final Label rmLabel;
  private final Runnable onUpdate;

  public TaskCell(Runnable onUpdate) {
    this.onUpdate = onUpdate;

    // --- Container Setup ---
    container = new VBox(5);
    container.setAlignment(Pos.CENTER);

    dateHeader = new Label();
    dateHeader.getStyleClass().add("date-separator");
    dateHeader.setMaxWidth(Double.MAX_VALUE);
    dateHeader.setAlignment(Pos.CENTER);

    rowLayout = new HBox(10);
    rowLayout.setAlignment(Pos.CENTER_LEFT);

    bracketLabel = new Label();
    bracketLabel.getStyleClass().add("bracket");
    bracketLabel.setMinWidth(Region.USE_PREF_SIZE);

    timeLabel = new Label();
    timeLabel.getStyleClass().add("time-stamp");
    timeLabel.setMinWidth(Region.USE_PREF_SIZE);

    textLabel = new Label();
    textLabel.getStyleClass().add("task-text");
    textLabel.setWrapText(true);
    HBox.setHgrow(textLabel, Priority.ALWAYS);

    rmLabel = new Label("[rm]");
    rmLabel.getStyleClass().add("rm-btn");
    rmLabel.setMinWidth(Region.USE_PREF_SIZE);

    // --- Event Listeners ---
    rmLabel.setOnMouseClicked(e -> {
      getListView().getItems().remove(getItem());
      onUpdate.run();
    });

    rowLayout.setOnMouseClicked(e -> {
      if (getItem() != null && !getItem().isMigrated()) {
        Task t = getItem();
        t.setDone(!t.isDone());
        t.setCompletedAt(t.isDone() ? LocalDateTime.now() : null);
        updateItem(t, false);
        onUpdate.run();
      }
    });

    rowLayout.getChildren().addAll(bracketLabel, timeLabel, textLabel, rmLabel);

    // --- FIX: Safe Binding Logic ---
    // Wait until the cell is actually added to a ListView before binding
    this.listViewProperty().addListener((obs, oldList, newList) -> {
      if (newList != null) {
        textLabel.maxWidthProperty().bind(
            Bindings.createDoubleBinding(() ->
                    newList.getWidth() - 160,
                newList.widthProperty()
            )
        );
      }
    });
  }

  @Override
  protected void updateItem(Task item, boolean empty) {
    super.updateItem(item, empty);
    if (empty || item == null) {
      setGraphic(null);
    } else {
      container.getChildren().clear();

      // Date Separator Logic
      boolean showHeader = false;
      int index = getIndex();
      if (index == 0) {
        showHeader = true;
      } else if (index - 1 >= 0 && index - 1 < getListView().getItems().size()) {
        Task prev = getListView().getItems().get(index - 1);
        LocalDate prevDate = prev.getCreatedAt().toLocalDate();
        LocalDate currDate = item.getCreatedAt().toLocalDate();
        if (!prevDate.isEqual(currDate)) {
          showHeader = true;
        }
      }

      if (showHeader) {
        String dateStr = "--- " + item.getCreatedAt().format(DATE_HEADER_FMT) + " ---";
        if (item.getCreatedAt().toLocalDate().isEqual(LocalDate.now())) dateStr = "--- Today ---";
        else if (item.getCreatedAt().toLocalDate().isEqual(LocalDate.now().minusDays(1))) dateStr = "--- Yesterday ---";

        dateHeader.setText(dateStr);
        container.getChildren().add(dateHeader);
      }

      container.getChildren().add(rowLayout);

      textLabel.setText(item.getText());
      textLabel.getStyleClass().removeAll("completed", "migrated-text");
      bracketLabel.getStyleClass().removeAll("completed-bracket", "migrated-bracket");
      timeLabel.getStyleClass().removeAll("completed-time");

      if (item.isMigrated()) {
        bracketLabel.setText("[>]");
        timeLabel.setText("[" + item.getCreatedAt().format(TIME_FMT) + "]");
        textLabel.getStyleClass().add("migrated-text");
        bracketLabel.getStyleClass().add("migrated-bracket");
      } else if (item.isDone()) {
        bracketLabel.setText("[x]");
        timeLabel.setText("[" + item.getCreatedAt().format(TIME_FMT) + "]");
        textLabel.getStyleClass().add("completed");
        bracketLabel.getStyleClass().add("completed-bracket");
        timeLabel.getStyleClass().add("completed-time");
      } else {
        bracketLabel.setText("[ ]");
        timeLabel.setText("[" + item.getCreatedAt().format(TIME_FMT) + "]");
      }

      setGraphic(container);
    }
  }
}