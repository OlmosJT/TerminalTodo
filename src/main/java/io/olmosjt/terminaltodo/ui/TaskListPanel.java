package io.olmosjt.terminaltodo.ui;

import io.olmosjt.terminaltodo.backend.DataService;
import io.olmosjt.terminaltodo.backend.Task;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
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

    listContainer = new VBox(5);
    listContainer.setPadding(new Insets(10));
    listContainer.getStyleClass().add("task-list-container");

    this.setContent(listContainer);
    this.setFitToWidth(true);
    this.setHbarPolicy(ScrollBarPolicy.NEVER);
    this.getStyleClass().add("task-scroll-pane");

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

    rebuildAll();

    tasks.addListener((ListChangeListener<Task>) c -> {
      while (c.next()) {
        if (c.wasPermutated()) {
          rebuildAll();
        } else if (c.wasUpdated()) {
        } else {
          for (Task rem : c.getRemoved()) {
            removeTaskNode(rem);
          }
          for (Task add : c.getAddedSubList()) {
            addTaskNode(add);
          }
        }
      }
      checkPlaceholder();
    });
  }

  private void checkPlaceholder() {
    if (tasks.isEmpty()) {
      if (!listContainer.getChildren().contains(placeholder)) {
        listContainer.getChildren().clear();
        listContainer.getChildren().add(placeholder);
      }
    } else {
      listContainer.getChildren().remove(placeholder);
    }
  }

  public void render() {
    rebuildAll();
  }

  private void rebuildAll() {
    listContainer.getChildren().clear();

    if (tasks.isEmpty()) {
      listContainer.getChildren().add(placeholder);
      return;
    }

    LocalDate lastDate = null;
    for (Task task : tasks) {
      LocalDate taskDate = task.getCreatedAt().toLocalDate();
      if (lastDate == null || !taskDate.isEqual(lastDate)) {
        addDateHeader(taskDate);
        lastDate = taskDate;
      }
      addTaskRow(task);
    }
  }

  private void addDateHeader(LocalDate date) {
    Label dateSep = new Label();
    dateSep.getStyleClass().add("date-separator");
    dateSep.setMaxWidth(Double.MAX_VALUE);
    dateSep.setAlignment(Pos.CENTER);

    if (date.isEqual(LocalDate.now())) {
      dateSep.setText("--- Today ---");
    } else {
      dateSep.setText("--- " + date.format(DATE_HEADER_FMT) + " ---");
    }
    dateSep.setUserData("DATE_HEADER_" + date.toString());
    listContainer.getChildren().add(dateSep);
  }

  private void addTaskRow(Task task) {
    TaskContainer row = new TaskContainer(
        task,
        () -> new Thread(() -> DataService.updateTask(task)).start(),
        () -> {
          tasks.remove(task);
          new Thread(() -> DataService.deleteTask(task.getId())).start();
        }
    );
    row.setUserData(task);
    listContainer.getChildren().add(row);
  }


  private void addTaskNode(Task task) {
    boolean todayHeaderExists = listContainer.getChildren().stream()
        .anyMatch(n -> n instanceof Label && "--- Today ---".equals(((Label)n).getText()));

    if (!todayHeaderExists) {
      addDateHeader(LocalDate.now());
    }
    addTaskRow(task);
  }

  private void removeTaskNode(Task task) {
    Node nodeToRemove = null;
    for (Node n : listContainer.getChildren()) {
      if (n instanceof TaskContainer && task.equals(n.getUserData())) {
        nodeToRemove = n;
        break;
      }
    }

    if (nodeToRemove != null) {
      listContainer.getChildren().remove(nodeToRemove);
    }
  }
}