package io.olmosjt.terminaltodo.ui;

import io.olmosjt.terminaltodo.backend.DataService;
import io.olmosjt.terminaltodo.backend.Task;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class TaskListPanel extends ScrollPane {
  private static final DateTimeFormatter DATE_HEADER_FMT = DateTimeFormatter.ofPattern("MMM dd");

  private final VBox listContainer;
  private final ObservableList<Task> tasks;
  private final VBox placeholder;

  public TaskListPanel(ObservableList<Task> tasks) {
    this.tasks = tasks;

    // 1. Main Container (VBox) to hold rows
    listContainer = new VBox(5);
    listContainer.setPadding(new Insets(10));
    listContainer.getStyleClass().add("task-list-container"); // New CSS class if needed

    // 2. ScrollPane Settings
    this.setContent(listContainer);
    this.setFitToWidth(true);
    this.setHbarPolicy(ScrollBarPolicy.NEVER);
    this.getStyleClass().add("task-scroll-pane");

    // 3. Placeholder (Empty State)
    placeholder = new VBox();
    placeholder.setAlignment(Pos.CENTER);
    Label art = new Label("""
            > SYSTEM STATUS: IDLE
            All processes terminated.
            Are you being productive or just lucky?
            ───▄▄▄
            ─▄▀░▄░▀▄
            ─█░█▄▀░█
            ─█░▀▄▄▀█▄█▄▀
            ▄▄█▄▄▄▄███▀
            """);
    art.getStyleClass().add("placeholder-text");
    placeholder.getChildren().add(art);
    // Bind placeholder visibility
    placeholder.visibleProperty().bind(javafx.beans.binding.Bindings.isEmpty(tasks));

    // 4. Initial Render & Listener
    render();
    tasks.addListener((ListChangeListener<Task>) c -> render());
  }

  /**
   * "Vue-like" render function.
   * Clears the DOM and rebuilds it based on state.
   */
  public void render() {
    listContainer.getChildren().clear();

    if (tasks.isEmpty()) {
      listContainer.getChildren().add(placeholder);
      return;
    }

    LocalDate lastDate = null;

    for (Task task : tasks) {
      // --- Date Header Logic ---
      LocalDate taskDate = task.getCreatedAt().toLocalDate();
      if (lastDate == null || !taskDate.isEqual(lastDate)) {
        Label dateSep = new Label();
        dateSep.getStyleClass().add("date-separator");
        dateSep.setMaxWidth(Double.MAX_VALUE);
        dateSep.setAlignment(Pos.CENTER);

        if (taskDate.isEqual(LocalDate.now())) {
          dateSep.setText("--- Today ---");
        } else {
          dateSep.setText("--- " + taskDate.format(DATE_HEADER_FMT) + " ---");
        }
        listContainer.getChildren().add(dateSep);
        lastDate = taskDate;
      }

      // --- Component Injection ---
      TaskContainer row = new TaskContainer(
          task,
          () -> DataService.saveTasks(tasks),  // onSave prop
          () -> {                              // onDelete prop
            tasks.remove(task);
            DataService.saveTasks(tasks);
          }
      );
      listContainer.getChildren().add(row);
    }
  }
}