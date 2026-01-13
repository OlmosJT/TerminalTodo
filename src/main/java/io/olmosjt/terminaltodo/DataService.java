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
          String[] parts = line.split("\\|", 4);
          if (parts.length == 4) {
            // Parse Status: 0=Todo, 1=Done, 2=Migrated
            int statusInt = 0;
            try {
              statusInt = Integer.parseInt(parts[0]);
            } catch (NumberFormatException e) {
              /* legacy */
            }

            boolean isDone = (statusInt == 1);
            boolean isMigrated = (statusInt == 2);

            LocalDateTime created =
                parts[1].equals("null")
                    ? LocalDateTime.now()
                    : LocalDateTime.parse(parts[1], FILE_FMT);

            LocalDateTime completed =
                parts[2].equals("null")
                    ? null
                    : LocalDateTime.parse(parts[2], FILE_FMT);

            Task t = new Task(parts[3]);
            t.setDone(isDone);
            t.setMigrated(isMigrated);
            t.setCreatedAt(created);
            t.setCompletedAt(completed);
            tasks.add(t);
          }
        });
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public static void saveTasks(ObservableList<Task> tasks) {
    try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(FILE_PATH))) {
      for (Task t : tasks) {
        // Save 2 for migrated, 1 for done, 0 for todo
        String status = t.isMigrated() ? "2" : (t.isDone() ? "1" : "0");
        String created = t.getCreatedAt().format(FILE_FMT);
        String completed = t.getCompletedAt() == null ? "null" : t.getCompletedAt().format(FILE_FMT);
        writer.write(status + "|" + created + "|" + completed + "|" + t.getText());
        writer.newLine();
      }
    } catch (IOException e) { e.printStackTrace(); }
  }
}