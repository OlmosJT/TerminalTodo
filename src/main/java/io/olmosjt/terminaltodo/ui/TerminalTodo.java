package io.olmosjt.terminaltodo.ui;

import io.olmosjt.terminaltodo.backend.DataService;
import io.olmosjt.terminaltodo.backend.Task;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class TerminalTodo extends Application {
  private ObservableList<Task> tasks;
  private BorderPane root;
  private TaskListPanel taskListPanel; // Now our custom ScrollPane

  public static void main(String[] args) { launch(args); }

  @Override
  public void start(Stage primaryStage) {
    // Data Load
    tasks = FXCollections.observableArrayList();
    DataService.loadTasks(tasks);

    // Layout
    root = new BorderPane();
    String currentTheme = DataService.getTheme();
    root.getStyleClass().addAll("terminal-window", currentTheme);

    // Components
    taskListPanel = new TaskListPanel(tasks);
    InputPanel inputPanel = new InputPanel(this::handleCommand);

    root.setCenter(taskListPanel);
    root.setBottom(inputPanel);

    Scene scene = new Scene(root, 750, 500);
    scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

    primaryStage.setTitle("user@fedora:~/todo");
    primaryStage.setScene(scene);
    primaryStage.show();

    inputPanel.requestFocusOnInput();
  }

  private void handleCommand(String input) {
    if (input.startsWith("!theme ")) {
      changeTheme(input.substring(7).toLowerCase());
    }
    else if (input.equalsIgnoreCase("!report")) {
      new ReportWindow(tasks, DataService.getTheme()).show();
    }
    else if (input.equalsIgnoreCase("!clear")) {
      tasks.clear();
      DataService.saveTasks(tasks);
    }
    else if (input.startsWith("!s ")) {
      addSubTask(input);
    }
    else if (input.equalsIgnoreCase("exit")) {
      Platform.exit();
    }
    else {
      addTask(input);
    }
  }

  private void changeTheme(String themeName) {
    if (themeName.equals("dart")) themeName = "dark";
    String themeClass = "theme-" + themeName;
    root.getStyleClass().removeAll("theme-gruvbox", "theme-light", "theme-dark", "theme-orange");
    root.getStyleClass().add(themeClass);
    DataService.setTheme(themeClass);
  }

  private void addSubTask(String input) {
    String[] parts = input.split(" ", 3);
    if (parts.length == 3) {
      String idPrefix = parts[1];
      String subText = parts[2];
      tasks.stream()
          .filter(t -> t.getId().startsWith(idPrefix))
          .findFirst()
          .ifPresent(parent -> {
            parent.getSubTasks().add(new Task(subText, Task.Priority.NORMAL));
            DataService.saveTasks(tasks);
            taskListPanel.render(); // Explicit re-render needed for subtasks
          });
    }
  }

  private void addTask(String input) {
    Task.Priority p = Task.Priority.NORMAL;
    String text = input;

    if (input.startsWith("!c ")) { p = Task.Priority.CRITICAL; text = input.substring(3); }
    else if (input.startsWith("!h ")) { p = Task.Priority.HIGH; text = input.substring(3); }
    else if (input.startsWith("!l ")) { p = Task.Priority.LOW; text = input.substring(3); }

    tasks.add(new Task(text.trim(), p));
    DataService.saveTasks(tasks);
    // Note: tasks.add triggers the Listener in TaskListPanel, so render() happens automatically
  }
}