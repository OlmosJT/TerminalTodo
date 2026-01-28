package io.olmosjt.terminaltodo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor
public class Task {
  public enum Priority { LOW, NORMAL, HIGH, CRITICAL }

  private String id;
  private String text;
  private boolean isDone;
  private boolean isMigrated;
  private LocalDateTime createdAt;
  private LocalDateTime completedAt;
  private Priority priority = Priority.NORMAL;
  private List<Task> subTasks = new ArrayList<>();

  public Task(String text, Priority priority) {
    this.id = UUID.randomUUID().toString().substring(0, 8);
    this.text = text;
    this.priority = priority;
    this.isDone = false;
    this.isMigrated = false;
    this.createdAt = LocalDateTime.now();
  }
}