package io.olmosjt.terminaltodo;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportWindow {

  private final List<Task> allTasks;
  private final String currentTheme;
  private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("HH:mm");

  public ReportWindow(List<Task> tasks, String theme) {
    this.allTasks = tasks;
    this.currentTheme = theme;
  }

  public void show() {
    Stage reportStage = new Stage();
    reportStage.initStyle(StageStyle.UNDECORATED);
    // FIX 1: Change Modality to NONE to allow interacting with the main window
    reportStage.initModality(Modality.NONE);
    reportStage.setMinWidth(600);
    reportStage.setMinHeight(500);

    BorderPane reportRoot = new BorderPane();
    reportRoot.getStyleClass().addAll("terminal-window", currentTheme);
    reportRoot.setPadding(new Insets(2));

    // Header
    HBox header = createHeader(reportStage);

    // --- Controls (Responsive) ---
    HBox controls = new HBox(10);
    controls.setPadding(new Insets(10, 20, 0, 20));
    controls.setAlignment(Pos.CENTER_LEFT);
    controls.getStyleClass().add("monitor-bg");

    DatePicker startDate = new DatePicker(LocalDate.now().minusDays(7));
    DatePicker endDate = new DatePicker(LocalDate.now());
    styleDatePicker(startDate);
    styleDatePicker(endDate);

    HBox.setHgrow(startDate, Priority.SOMETIMES);
    HBox.setHgrow(endDate, Priority.SOMETIMES);

    Label arrow = new Label("->");
    arrow.setMinWidth(Region.USE_PREF_SIZE);

    Label runBtn = new Label("[ RUN DIAGNOSTICS ]");
    runBtn.getStyleClass().add("control-btn");
    runBtn.setStyle("-fx-border-color: -accent; -fx-border-width: 1px; -fx-padding: 2 5;");
    runBtn.setMinWidth(140);
    runBtn.setAlignment(Pos.CENTER);

    controls.getChildren().addAll(startDate, arrow, endDate, runBtn);

    // Content
    VBox contentArea = new VBox(20);
    contentArea.setPadding(new Insets(20));
    contentArea.getStyleClass().add("monitor-bg");
    VBox.setVgrow(contentArea, Priority.ALWAYS);

    runBtn.setOnMouseClicked(e -> updateReportContent(contentArea, startDate.getValue(), endDate.getValue()));
    updateReportContent(contentArea, startDate.getValue(), endDate.getValue()); // Initial run

    // Footer
    HBox footer = createResizeHandle(reportStage);

    VBox mainLayout = new VBox(controls, contentArea);
    VBox.setVgrow(mainLayout, Priority.ALWAYS);

    reportRoot.setTop(header);
    reportRoot.setCenter(mainLayout);
    reportRoot.setBottom(footer);

    Scene scene = new Scene(reportRoot, 600, 500);
    scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
    reportStage.setScene(scene);
    reportStage.show();
  }

  private HBox createHeader(Stage stage) {
    HBox header = new HBox(10);
    header.getStyleClass().add("header");
    header.setAlignment(Pos.CENTER_LEFT);
    Label title = new Label("SYSTEM DIAGNOSTICS [PID " + ProcessHandle.current().pid() + "]");
    title.getStyleClass().add("header-text");
    Region rSpacer = new Region();
    HBox.setHgrow(rSpacer, Priority.ALWAYS);
    Label close = new Label("[x]");
    close.getStyleClass().addAll("control-btn", "close-btn");
    close.setOnMouseClicked(e -> stage.close());
    header.getChildren().addAll(title, rSpacer, close);

    final double[] dragDelta = {0, 0};
    header.setOnMousePressed(e -> {
      dragDelta[0] = stage.getX() - e.getScreenX();
      dragDelta[1] = stage.getY() - e.getScreenY();
    });
    header.setOnMouseDragged(e -> {
      stage.setX(e.getScreenX() + dragDelta[0]);
      stage.setY(e.getScreenY() + dragDelta[1]);
    });
    return header;
  }

  private void updateReportContent(VBox container, LocalDate start, LocalDate end) {
    container.getChildren().clear();
    if (start == null || end == null || start.isAfter(end)) return;

    List<Task> rangeTasks = allTasks.stream()
        .filter(t -> {
          LocalDate d = t.getCreatedAt().toLocalDate();
          return (d.isEqual(start) || d.isAfter(start)) && (d.isEqual(end) || d.isBefore(end));
        }).collect(Collectors.toList());

    // Stats
    long total = rangeTasks.size();
    long completed = rangeTasks.stream().filter(Task::isDone).count();
    double efficiency = total == 0 ? 0 : ((double) completed / total) * 100;

    VBox statsBox = new VBox(5);
    statsBox.getChildren().addAll(
        createStatLine("RANGE", start + " to " + end),
        createStatLine("PROCESSES", total + " total, " + completed + " terminated"),
        createStatLine("EFFICIENCY", String.format("%.1f%%", efficiency))
    );

    ProgressBar cpuBar = new ProgressBar(efficiency / 100.0);
    cpuBar.setMaxWidth(Double.MAX_VALUE);
    cpuBar.getStyleClass().add("monitor-progress");

    // Chart
    ScrollPane scrollPane = new ScrollPane(createAsciiHistogram(rangeTasks, start, end));
    scrollPane.getStyleClass().add("chart-scroll");
    scrollPane.setFitToHeight(true);
    scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

    // Logs
    VBox logConsole = new VBox(2);
    logConsole.getStyleClass().add("log-console");
    rangeTasks.stream().sorted((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt())).limit(10)
        .forEach(t -> {
          Label l = new Label(t.getCreatedAt().format(DISPLAY_FMT) + " " + (t.isDone() ? "[OK]" : "[..]") + " " + t.getText());
          l.getStyleClass().add("log-text");
          logConsole.getChildren().add(l);
        });

    container.getChildren().addAll(statsBox, cpuBar, new Separator(), new Label("ACTIVITY:"), scrollPane, new Separator(), logConsole);
  }

  private HBox createAsciiHistogram(List<Task> tasks, LocalDate start, LocalDate end) {
    HBox barChart = new HBox(15);
    barChart.setAlignment(Pos.BOTTOM_LEFT);
    barChart.setPrefHeight(60);
    barChart.setPadding(new Insets(0,0,15,0));

    Map<LocalDate, Long> counts = tasks.stream()
        .collect(Collectors.groupingBy(t -> t.getCreatedAt().toLocalDate(), Collectors.counting()));

    for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
      Long count = counts.getOrDefault(date, 0L);
      String block = count > 6 ? "█" : (count > 4 ? "▆" : (count > 2 ? "▄" : (count > 0 ? "▂" : " ")));

      VBox barCol = new VBox(2);
      barCol.setAlignment(Pos.BOTTOM_CENTER);
      Label bar = new Label(block);
      bar.getStyleClass().add("chart-bar");
      bar.setStyle("-fx-font-size: " + (12 + (count * 2)) + "px;");

      Label dateLbl = new Label(date.format(DateTimeFormatter.ofPattern("dd/MM")));
      dateLbl.getStyleClass().add("chart-label");

      barCol.getChildren().addAll(bar, dateLbl);
      barChart.getChildren().add(barCol);
    }
    return barChart;
  }

  private HBox createResizeHandle(Stage stage) {
    HBox footer = new HBox();
    footer.setAlignment(Pos.BOTTOM_RIGHT);
    Label resize = new Label(" ///");
    resize.getStyleClass().add("resize-handle");
    resize.setCursor(Cursor.SE_RESIZE);

    final double[] startPos = {0, 0};
    final double[] startSize = {0, 0};

    resize.setOnMousePressed(e -> {
      startPos[0] = e.getScreenX();
      startPos[1] = e.getScreenY();
      startSize[0] = stage.getWidth();
      startSize[1] = stage.getHeight();
    });

    resize.setOnMouseDragged(e -> {
      double deltaX = e.getScreenX() - startPos[0];
      double deltaY = e.getScreenY() - startPos[1];
      double newW = startSize[0] + deltaX;
      double newH = startSize[1] + deltaY;
      if(newW > 500) stage.setWidth(newW);
      if(newH > 400) stage.setHeight(newH);
    });

    footer.getChildren().add(resize);
    return footer;
  }

  private HBox createStatLine(String key, String value) {
    HBox line = new HBox(10);
    Label k = new Label(key + ":"); k.getStyleClass().add("stat-key"); k.setMinWidth(100);
    Label v = new Label(value); v.getStyleClass().add("stat-value");
    line.getChildren().addAll(k, v);
    return line;
  }

  // FIX 3: Removed fixed width, added MaxWidth
  private void styleDatePicker(DatePicker dp) {
    dp.setMaxWidth(Double.MAX_VALUE); // Allow growing

    dp.setConverter(new StringConverter<LocalDate>() {
      DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
      public String toString(LocalDate d) { return d != null ? fmt.format(d) : ""; }
      public LocalDate fromString(String s) { return s != null && !s.isEmpty() ? LocalDate.parse(s, fmt) : null; }
    });
    dp.getEditor().getStyleClass().add("cmd-input");
  }
}