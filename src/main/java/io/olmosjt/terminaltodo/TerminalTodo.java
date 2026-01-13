package io.olmosjt.terminaltodo;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TerminalTodo extends Application {

  private ObservableList<Task> tasks;
  private Label statusLabel;
  private BorderPane root;
  private TextField inputField;
  private Label promptChar;
  private boolean awaitingClearConfirmation = false;
  private double xOffset = 0, yOffset = 0;

  public static void main(String[] args) { launch(args); }

  @Override
  public void start(Stage primaryStage) {
    primaryStage.initStyle(StageStyle.UNDECORATED);
    primaryStage.setMinWidth(400);
    primaryStage.setMinHeight(300);

    tasks = FXCollections.observableArrayList();
    DataService.loadTasks(tasks);

    root = new BorderPane();

    // --- CHANGE: Load saved theme dynamically ---
    String savedTheme = ConfigService.getTheme();
    root.getStyleClass().addAll("terminal-window", savedTheme);
    // --------------------------------------------

    // ... rest of the method (UI Setup) ...
    HBox header = createHeader(primaryStage);
    ListView<Task> listView = new ListView<>(tasks);
    listView.getStyleClass().add("task-list");
    listView.setCellFactory(param -> new TaskCell(() -> {
      DataService.saveTasks(tasks);
      updateStatus();
    }));

    HBox inputBox = createInputBox();
    HBox statusBar = createStatusBar(primaryStage);

    root.setTop(header);
    root.setCenter(listView);
    root.setBottom(new VBox(inputBox, statusBar));

    inputField.setOnAction(e -> {
      if (!inputField.getText().isEmpty()) {
        handleInput(inputField.getText().trim());
      }
    });



    // ... key listeners and scene setup ...
    Scene scene = new Scene(root, 700, 500);
    scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
    primaryStage.setTitle("~/daily_plan");
    primaryStage.setScene(scene);
    primaryStage.show();

    inputField.requestFocus();
  }

  private void handleInput(String input) {
    inputField.setStyle("");

    if (awaitingClearConfirmation) {
      if (input.equalsIgnoreCase("y") || input.equalsIgnoreCase("yes")) {
        tasks.clear();
        DataService.saveTasks(tasks);
        resetInputState();
        updateStatus();
      } else {
        resetInputState();
      }
      return;
    }

    if (input.equalsIgnoreCase("exit")) Platform.exit();
    else if (input.equalsIgnoreCase("clear")) initiateClearSequence();
    else if (input.equalsIgnoreCase("migrate")) {
      inputField.clear();
      performMigration();
    }
    else {
      tasks.add(new Task(input));
      DataService.saveTasks(tasks);
      inputField.clear();
      updateStatus();
    }
  }

  private void performMigration() {
    LocalDate today = LocalDate.now();
    LocalDate yesterday = today.minusDays(1);
    int count = 0;

    List<Task> tasksToMigrate = new ArrayList<>();

    for (Task t : tasks) {
      boolean isFromYesterday = t.getCreatedAt().toLocalDate().isEqual(yesterday);

      if (!t.isDone() && !t.isMigrated() && isFromYesterday) {
        tasksToMigrate.add(t);
      }
    }

    if (tasksToMigrate.isEmpty()) {
      inputField.setPromptText("No pending tasks from yesterday found.");
      inputField.setStyle("-fx-prompt-text-fill: -dim;");
      return;
    }

    for (Task oldTask : tasksToMigrate) {
      oldTask.setMigrated(true);

      Task newTask = new Task(oldTask.getText());
      tasks.add(newTask);
      count++;
    }

    DataService.saveTasks(tasks);
    updateStatus();

    inputField.setPromptText("Migrated " + count + " tasks from yesterday to today.");
    inputField.setStyle("-fx-prompt-text-fill: -accent;"); // Greenish text for success
  }

  private HBox createHeader(Stage stage) {
    HBox header = new HBox(10);
    header.getStyleClass().add("header");
    header.setAlignment(Pos.CENTER_LEFT);

    HBox themeSwitcher = new HBox(8);
    themeSwitcher.getChildren().addAll(
        createThemeCircle("theme-gruvbox-btn", "theme-gruvbox"),
        createThemeCircle("theme-light-btn", "theme-light"),
        createThemeCircle("theme-dark-btn", "theme-dark")
    );

    Label user = new Label(" user@linux:~/todo");
    user.getStyleClass().add("header-text");
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    Label reportBtn = new Label("[report]");
    reportBtn.getStyleClass().add("control-btn");
    // Open the ReportWindow Class
    reportBtn.setOnMouseClicked(e -> {
      String currentTheme = root.getStyleClass().stream()
          .filter(s -> s.startsWith("theme-"))
          .findFirst()
          .orElse("theme-gruvbox");

      new ReportWindow(tasks, currentTheme).show();
    });

    Label clearBtn = new Label("[clear]");
    clearBtn.getStyleClass().add("control-btn");
    clearBtn.setOnMouseClicked(e -> initiateClearSequence());

    Label pinBtn = new Label("[^]");
    pinBtn.getStyleClass().add("control-btn");
    pinBtn.setOnMouseClicked(e -> {
      boolean top = !stage.isAlwaysOnTop();
      stage.setAlwaysOnTop(top);
      pinBtn.setText(top ? "[*]" : "[^]");
      if(top) pinBtn.getStyleClass().add("active"); else pinBtn.getStyleClass().remove("active");
    });

    Label closeBtn = new Label("[x]");
    closeBtn.getStyleClass().addAll("control-btn", "close-btn");
    closeBtn.setOnMouseClicked(e -> Platform.exit());

    header.getChildren().addAll(themeSwitcher, user, spacer, reportBtn, clearBtn, pinBtn, closeBtn);

    header.setOnMousePressed(e -> { xOffset = e.getSceneX(); yOffset = e.getSceneY(); });
    header.setOnMouseDragged(e -> { stage.setX(e.getScreenX() - xOffset); stage.setY(e.getScreenY() - yOffset); });

    return header;
  }

  private HBox createInputBox() {
    HBox box = new HBox(10);
    box.getStyleClass().add("input-line");
    box.setAlignment(Pos.CENTER_LEFT);
    box.setPadding(new Insets(10));
    promptChar = new Label("$");
    promptChar.getStyleClass().add("prompt");
    inputField = new TextField();
    inputField.setPromptText("echo 'new task' >> daily_plan");
    inputField.getStyleClass().add("cmd-input");
    HBox.setHgrow(inputField, Priority.ALWAYS);
    box.getChildren().addAll(promptChar, inputField);
    return box;
  }

  private HBox createStatusBar(Stage stage) {
    HBox bar = new HBox();
    bar.getStyleClass().add("status-bar");
    statusLabel = new Label();
    updateStatus();
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    Label resize = new Label(" ///");
    resize.getStyleClass().add("resize-handle");
    resize.setCursor(Cursor.SE_RESIZE);

    // --- SMOOTH RESIZE LOGIC ---
    // We use arrays to hold mutable state inside the lambda
    final double[] startPos = {0, 0};
    final double[] startSize = {0, 0};

    resize.setOnMousePressed(e -> {
      // 1. Record where the mouse is and how big the window is RIGHT NOW
      startPos[0] = e.getScreenX();
      startPos[1] = e.getScreenY();
      startSize[0] = stage.getWidth();
      startSize[1] = stage.getHeight();
    });

    resize.setOnMouseDragged(e -> {
      // 2. Calculate how far the mouse moved
      double deltaX = e.getScreenX() - startPos[0];
      double deltaY = e.getScreenY() - startPos[1];

      // 3. Add that movement to the original size
      double newW = startSize[0] + deltaX;
      double newH = startSize[1] + deltaY;

      // 4. Apply safety limits
      if(newW > 400) stage.setWidth(newW);
      if(newH > 300) stage.setHeight(newH);
    });
    // ---------------------------

    bar.getChildren().addAll(statusLabel, spacer, new Label("-- INSERT --"), resize);
    return bar;
  }

  private void initiateClearSequence() {
    awaitingClearConfirmation = true;
    promptChar.setText("Confirm delete all? (y/n):");
    promptChar.getStyleClass().add("prompt-warning");
    inputField.clear();
    inputField.setPromptText("");
  }

  private void resetInputState() {
    awaitingClearConfirmation = false;
    promptChar.setText("$");
    promptChar.getStyleClass().remove("prompt-warning");
    inputField.clear();
    inputField.setPromptText("echo 'new task' >> daily_plan");
  }

  private Circle createThemeCircle(String style, String rootClass) {
    Circle c = new Circle(6);
    c.getStyleClass().addAll("theme-circle", style);
    c.setOnMouseClicked(e -> {
      root.getStyleClass().removeAll("theme-gruvbox", "theme-light", "theme-dark");
      root.getStyleClass().add(rootClass);

      // --- CHANGE: Save to config immediately ---
      ConfigService.setTheme(rootClass);
      // ------------------------------------------
    });
    return c;
  }

  private void updateStatus() {
    long pending = tasks.stream().filter(t -> !t.isDone()).count();
    statusLabel.setText(pending + " process(es) active");
  }
}