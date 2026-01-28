package io.olmosjt.terminaltodo;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.util.List;

public class ReportWindow {
  private final List<Task> allTasks;
  private final String currentTheme;

  public ReportWindow(List<Task> tasks, String theme) {
    this.allTasks = tasks;
    this.currentTheme = theme;
  }

  public void show() {
    Stage stage = new Stage();
    stage.setTitle("SYSTEM_REPORT");

    VBox root = new VBox(15);
    root.getStyleClass().addAll("terminal-window", currentTheme);
    root.setPadding(new Insets(20));

    long total = allTasks.size();
    long completed = allTasks.stream().filter(Task::isDone).count();
    double efficiency = total == 0 ? 0 : ((double) completed / total) * 100;

    Label efficiencyLabel = new Label(String.format(">> SYSTEM EFFICIENCY: %.1f%%", efficiency));
    efficiencyLabel.getStyleClass().add("header-text");

    ProgressBar progress = new ProgressBar(efficiency / 100.0);
    progress.setMaxWidth(Double.MAX_VALUE);
    progress.getStyleClass().add("monitor-progress");

    VBox logConsole = new VBox(5);
    logConsole.getStyleClass().add("log-console");
    VBox.setVgrow(logConsole, Priority.ALWAYS);

    Label logHeader = new Label("--- RECENT PROCESSES ---");
    logHeader.getStyleClass().add("stat-key");
    logConsole.getChildren().add(logHeader);

    allTasks.stream()
        .sorted((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()))
        .limit(15)
        .forEach(t -> {
          String status = t.isDone() ? "[OK]" : "[..]";
          Label l = new Label(String.format("%s %s %s", t.getId().substring(0, 4), status, t.getText()));
          l.getStyleClass().add("log-text");
          logConsole.getChildren().add(l);
        });

    Button closeBtn = new Button("exit report");
    closeBtn.getStyleClass().add("cmd-input");
    closeBtn.setOnAction(e -> stage.close());

    root.getChildren().addAll(efficiencyLabel, progress, logConsole, closeBtn);

    Scene scene = new Scene(root, 500, 400);
    scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
    stage.setScene(scene);
    stage.show();
  }
}