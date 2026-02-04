package io.olmosjt.terminaltodo.ui;

import io.olmosjt.terminaltodo.backend.DataService;
import io.olmosjt.terminaltodo.backend.Task;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TerminalTodo extends Application {
  private ObservableList<Task> tasks;
  private BorderPane root;
  private TaskListPanel taskListPanel;
  private Stage primaryStage;

  private static final Pattern CMD_PATTERN = Pattern.compile("^(.*?)\\s+\\[([a-zA-Z0-9:]+)\\]$");

  public static void main(String[] args) { launch(args); }

  @Override
  public void start(Stage primaryStage) {
    this.primaryStage = primaryStage;

    tasks = FXCollections.observableArrayList();
    DataService.loadTasks(tasks);

    root = new BorderPane();
    String currentTheme = "theme-dark";
    root.getStyleClass().addAll("terminal-window", currentTheme);

    taskListPanel = new TaskListPanel(tasks);
    InputPanel inputPanel = new InputPanel(this::handleCommand);

    root.setCenter(taskListPanel);
    root.setBottom(inputPanel);

    Scene scene = new Scene(root, 750, 500);
    scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

    primaryStage.setTitle("user@linux:~/todo");
    primaryStage.setScene(scene);
    primaryStage.show();

    inputPanel.requestFocusOnInput();
  }

  private void handleCommand(String input) {
    String cleanInput = input.trim();
    if (cleanInput.isEmpty()) return;

    if (cleanInput.equalsIgnoreCase("exit") || cleanInput.equalsIgnoreCase("!exit")) {
      Platform.exit();
      return;
    }
    if (cleanInput.equalsIgnoreCase("clear") || cleanInput.equalsIgnoreCase("!clear")) {
      tasks.clear();
      DataService.saveTasks(tasks);
      return;
    }
    if (cleanInput.equalsIgnoreCase("help") || cleanInput.equalsIgnoreCase("!help")) {
      showHelp();
      return;
    }

    Matcher matcher = CMD_PATTERN.matcher(cleanInput);
    String taskText = cleanInput;
    String tag = null;

    if (matcher.find()) {
      taskText = matcher.group(1).trim();
      tag = matcher.group(2).toLowerCase();
    }

    processTask(taskText, tag);
  }

  private void processTask(String text, String tag) {
    Task.Priority priority = Task.Priority.NORMAL;
    String parentId = null;

    if (tag != null) {
      if (tag.startsWith("sub:")) {
        parentId = tag.substring(4);
      } else {
        switch (tag) {
          case "high" -> priority = Task.Priority.HIGH;
          case "critical" -> priority = Task.Priority.CRITICAL;
          case "low" -> priority = Task.Priority.LOW;
        }
      }
    }

    if (parentId != null) {
      createSubTask(parentId, text);
    } else {
      createTask(text, priority);
    }
  }

  private void createTask(String text, Task.Priority priority) {
    tasks.add(new Task(text, priority));
    DataService.saveTasks(tasks);
  }

  private void createSubTask(String parentIdPrefix, String text) {
    tasks.stream()
        .filter(t -> t.getId().startsWith(parentIdPrefix))
        .findFirst()
        .ifPresentOrElse(parent -> {
          parent.getSubTasks().add(new Task(text, Task.Priority.NORMAL));
          DataService.saveTasks(tasks);
          taskListPanel.render();
        }, () -> {
          createTask(text + " (Orphaned subtask)", Task.Priority.NORMAL);
        });
  }

  private void showHelp() {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.initOwner(primaryStage);
    alert.setResizable(true);
    alert.setTitle("TerminalTodo Help");
    alert.setHeaderText("COMMAND REFERENCE");

    String helpText = """
        1. Create Task:
           $ buy milk
        
        2. Set Priority [low|high|critical]:
           $ buy milk [high]
           $ fix bug [critical]
        
        3. Create Subtask [sub:<parent_id>]:
           $ check logs [sub:1a2b]
        
        4. Edit Task:
           Double click on existing task description. Edit (press ENTER to save)
        
        5. System:
           $ clear   (Delete all tasks)
           $ help    (Show this menu)
           $ exit    (Close app)
        """;

    TextArea area = new TextArea(helpText);
    area.setEditable(false);
    area.setWrapText(true);
    area.getStyleClass().add("help-console");
    area.setMaxWidth(Double.MAX_VALUE);
    area.setMaxHeight(Double.MAX_VALUE);

    var dialogPane = alert.getDialogPane();
    dialogPane.setContent(area);
    dialogPane.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
    dialogPane.getStyleClass().addAll("terminal-window", "theme-dark");

    alert.showAndWait();
  }
}