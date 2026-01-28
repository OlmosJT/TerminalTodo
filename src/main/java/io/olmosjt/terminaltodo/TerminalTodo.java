package io.olmosjt.terminaltodo;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class TerminalTodo extends Application {
  private ObservableList<Task> tasks;
  private ListView<Task> listView;
  private BorderPane root;
  private TextField inputField;
  private Label promptChar;

  public static void main(String[] args) { launch(args); }

  @Override
  public void start(Stage primaryStage) {
    tasks = FXCollections.observableArrayList();
    DataService.loadTasks(tasks);

    root = new BorderPane();
    root.getStyleClass().addAll("terminal-window", ConfigService.getTheme());

    listView = new ListView<>(tasks);
    listView.getStyleClass().add("task-list");
    listView.setCellFactory(param -> new TaskCell(() -> DataService.saveTasks(tasks)));

    // --- FUN PLACEHOLDER ---
    Label placeholder = new Label("""
        > SYSTEM STATUS: IDLE
        All processes terminated.
        Are you being productive or just lucky?
        ───▄▄▄
        ─▄▀░▄░▀▄
        ─█░█▄▀░█
        ─█░▀▄▄▀█▄█▄▀
        ▄▄█▄▄▄▄███▀
        """);
    placeholder.getStyleClass().add("placeholder-text");
    placeholder.setAlignment(Pos.CENTER);
    listView.setPlaceholder(placeholder);
    // -----------------------

    HBox inputBox = createInputArea();

    root.setCenter(listView);
    root.setBottom(inputBox);

    Scene scene = new Scene(root, 750, 500);
    scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

    primaryStage.setTitle("user@fedora:~/todo");
    primaryStage.setScene(scene);
    primaryStage.show();
    inputField.requestFocus();
  }

  private HBox createInputArea() {
    HBox inputBox = new HBox(10);
    inputBox.getStyleClass().add("input-line");
    // FIX: Explicitly align center-left so the $ and Text overlap correctly
    inputBox.setAlignment(Pos.CENTER_LEFT);

    promptChar = new Label("$");
    promptChar.getStyleClass().add("prompt");

    inputField = new TextField();
    inputField.getStyleClass().add("cmd-input");
    inputField.setPromptText("!theme, !report, !clear, !s [ID] [text]");
    inputField.setOnAction(e -> handleInput(inputField.getText().trim()));
    HBox.setHgrow(inputField, Priority.ALWAYS);

    inputBox.getChildren().addAll(promptChar, inputField);
    return inputBox;
  }

  private void handleInput(String input) {
    if (input.isEmpty()) return;

    if (input.startsWith("!theme ")) {
      String themeName = input.substring(7).toLowerCase();
      if (themeName.equals("dart")) themeName = "dark";
      String themeClass = "theme-" + themeName;
      root.getStyleClass().removeAll("theme-gruvbox", "theme-light", "theme-dark", "theme-orange");
      root.getStyleClass().add(themeClass);
      ConfigService.setTheme(themeClass);
    }
    else if (input.equalsIgnoreCase("!report")) {
      new ReportWindow(tasks, ConfigService.getTheme()).show();
    }
    else if (input.equalsIgnoreCase("!clear")) {
      tasks.clear();
      DataService.saveTasks(tasks);
    }
    else if (input.startsWith("!s ")) {
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
              listView.refresh();
            });
      }
    }
    else if (input.equalsIgnoreCase("exit")) {
      Platform.exit();
    }
    else {
      Task.Priority p = Task.Priority.NORMAL;
      String text = input;
      if (input.startsWith("!c ")) { p = Task.Priority.CRITICAL; text = input.substring(3); }
      else if (input.startsWith("!h ")) { p = Task.Priority.HIGH; text = input.substring(3); }
      else if (input.startsWith("!l ")) { p = Task.Priority.LOW; text = input.substring(3); }

      tasks.add(new Task(text.trim(), p));
      DataService.saveTasks(tasks);
    }
    inputField.clear();
  }
}