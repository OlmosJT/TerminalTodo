package io.olmosjt.terminaltodo;

import javafx.collections.ObservableList;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

public class DataService {
  private static final String FILE_PATH = System.getProperty("user.home") + "/.daily_plan";
  private static final DateTimeFormatter FILE_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  public static void loadTasks(ObservableList<Task> tasks) {
    Path path = Paths.get(FILE_PATH);
    if (Files.exists(path)) {
      try (Stream<String> lines = Files.lines(path)) {
        lines.forEach(line -> {
          String[] parts = line.split("\\|", 5); // Updated to 5 parts
          if (parts.length >= 4) {
            int statusInt = Integer.parseInt(parts[0]);
            LocalDateTime created = parts[1].equals("null") ? LocalDateTime.now() : LocalDateTime.parse(parts[1], FILE_FMT);
            LocalDateTime completed = parts[2].equals("null") ? null : LocalDateTime.parse(parts[2], FILE_FMT);

            // Handle priority and text
            Task.Priority priority = Task.Priority.NORMAL;
            String taskText;
            if (parts.length == 5) {
              priority = Task.Priority.valueOf(parts[3]);
              taskText = parts[4];
            } else {
              taskText = parts[3];
            }

            Task t = new Task(taskText, Task.Priority.NORMAL);
            t.setDone(statusInt == 1);
            t.setMigrated(statusInt == 2);
            t.setCreatedAt(created);
            t.setCompletedAt(completed);
            t.setPriority(priority);
            tasks.add(t);
          }
        });
      } catch (Exception e) { e.printStackTrace(); }
    }
  }

  public static void saveTasks(ObservableList<Task> tasks) {
    try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(FILE_PATH))) {
      for (Task t : tasks) {
        writeTask(writer, t, false);
        for (Task sub : t.getSubTasks()) {
          writeTask(writer, sub, true);
        }
      }
    } catch (IOException e) { e.printStackTrace(); }
  }

  private static void writeTask(BufferedWriter writer, Task t, boolean isSub) throws IOException {
    String prefix = isSub ? "SUB|" : "";
    String status = t.isMigrated() ? "2" : (t.isDone() ? "1" : "0");
    writer.write(String.format("%s%s|%s|%s|%s|%s",
        prefix, status, t.getCreatedAt(), t.getCompletedAt(), t.getPriority(), t.getText()));
    writer.newLine();
  }
}