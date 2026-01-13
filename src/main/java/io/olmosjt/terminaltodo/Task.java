package io.olmosjt.terminaltodo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class Task {
  private String text;
  private boolean isDone;
  private boolean isMigrated;
  private LocalDateTime createdAt;
  private LocalDateTime completedAt;

  public Task(String text) {
    this.text = text;
    this.isDone = false;
    this.isMigrated = false;
    this.createdAt = LocalDateTime.now();
  }

}
