package io.olmosjt.terminaltodo.backend;

import javafx.collections.ObservableList;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataService {
  private static final String DB_URL = "jdbc:h2:" + System.getProperty("user.home") + "/.daily_plan_db;DB_CLOSE_ON_EXIT=FALSE";

  private static final String TABLE_SQL = """
    CREATE TABLE IF NOT EXISTS tasks (
        id VARCHAR(36) PRIMARY KEY,
        text VARCHAR(255),
        is_done BOOLEAN,
        is_migrated BOOLEAN,
        created_at TIMESTAMP,
        completed_at TIMESTAMP,
        priority VARCHAR(20),
        parent_id VARCHAR(36)
    );
    CREATE TABLE IF NOT EXISTS settings (
        conf_key VARCHAR(50) PRIMARY KEY,
        conf_value VARCHAR(255)
    );
    """;

  static {
    try (Connection conn = DriverManager.getConnection(DB_URL);
         Statement stmt = conn.createStatement()) {
      stmt.execute(TABLE_SQL);
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public static String getTheme() {
    String sql = "SELECT conf_value FROM settings WHERE conf_key = 'theme'";
    try (Connection conn = DriverManager.getConnection(DB_URL);
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
      if (rs.next()) {
        return rs.getString("conf_value");
      }
    } catch (SQLException e) { e.printStackTrace(); }
    return "theme-dark";
  }

  public static void setTheme(String theme) {
    String sql = "MERGE INTO settings (conf_key, conf_value) KEY(conf_key) VALUES ('theme', ?)";
    try (Connection conn = DriverManager.getConnection(DB_URL);
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, theme);
      ps.executeUpdate();
    } catch (SQLException e) { e.printStackTrace(); }
  }

  public static void loadTasks(ObservableList<Task> tasks) {
    tasks.clear();
    Map<String, Task> taskMap = new HashMap<>();
    List<Task> topLevel = new ArrayList<>();
    Map<String, String> parentLinks = new HashMap<>();

    try (Connection conn = DriverManager.getConnection(DB_URL);
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery("SELECT * FROM tasks ORDER BY created_at")) {

      while (rs.next()) {
        Task t = new Task();
        t.setId(rs.getString("id"));
        t.setText(rs.getString("text"));
        t.setDone(rs.getBoolean("is_done"));
        t.setMigrated(rs.getBoolean("is_migrated"));

        Timestamp created = rs.getTimestamp("created_at");
        t.setCreatedAt(created != null ? created.toLocalDateTime() : LocalDateTime.now());

        Timestamp completed = rs.getTimestamp("completed_at");
        if (completed != null) t.setCompletedAt(completed.toLocalDateTime());

        try {
          t.setPriority(Task.Priority.valueOf(rs.getString("priority")));
        } catch (Exception e) { t.setPriority(Task.Priority.NORMAL); }

        String parentId = rs.getString("parent_id");

        taskMap.put(t.getId(), t);
        if (parentId != null) {
          parentLinks.put(t.getId(), parentId);
        } else {
          topLevel.add(t);
        }
      }

      for (Map.Entry<String, String> entry : parentLinks.entrySet()) {
        Task child = taskMap.get(entry.getKey());
        Task parent = taskMap.get(entry.getValue());
        if (parent != null && child != null) {
          parent.getSubTasks().add(child);
        } else if (child != null) {
          topLevel.add(child);
        }
      }

      tasks.addAll(topLevel);

    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public static void saveTasks(ObservableList<Task> tasks) {
    String insertSql = "INSERT INTO tasks (id, text, is_done, is_migrated, created_at, completed_at, priority, parent_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    try (Connection conn = DriverManager.getConnection(DB_URL)) {
      conn.setAutoCommit(false);
      try (Statement stmt = conn.createStatement();
           PreparedStatement ps = conn.prepareStatement(insertSql)) {

        stmt.execute("DELETE FROM tasks");

        for (Task t : tasks) {
          insertTask(ps, t, null);
          for (Task sub : t.getSubTasks()) {
            insertTask(ps, sub, t.getId());
          }
        }

        conn.commit();
      } catch (SQLException e) {
        conn.rollback();
        e.printStackTrace();
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public static void updateTask(Task t) {
    String sql = "UPDATE tasks SET text=?, is_done=?, completed_at=?, priority=? WHERE id=?";
    try (Connection conn = DriverManager.getConnection(DB_URL);
         PreparedStatement ps = conn.prepareStatement(sql)) {

      ps.setString(1, t.getText());
      ps.setBoolean(2, t.isDone());
      ps.setTimestamp(3, t.getCompletedAt() != null ? Timestamp.valueOf(t.getCompletedAt()) : null);
      ps.setString(4, t.getPriority().name());
      ps.setString(5, t.getId());

      ps.executeUpdate();

      for (Task sub : t.getSubTasks()) {
        updateTask(sub);
      }
    } catch (SQLException e) { e.printStackTrace(); }
  }

  // CHANGED: Added single task delete to prevent heavy full-rewrite
  public static void deleteTask(String id) {
    // Simple delete. In a real app, you might want to cascade delete subtasks manually
    // if the DB FK doesn't handle it, but for now we assume this is sufficient or handled by app logic.
    String sql = "DELETE FROM tasks WHERE id=?";
    try (Connection conn = DriverManager.getConnection(DB_URL);
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, id);
      ps.executeUpdate();
      // Ideally delete orphaned subtasks here too
      deleteOrphanedSubtasks(id);
    } catch (SQLException e) { e.printStackTrace(); }
  }

  private static void deleteOrphanedSubtasks(String parentId) {
    String sql = "DELETE FROM tasks WHERE parent_id=?";
    try (Connection conn = DriverManager.getConnection(DB_URL);
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, parentId);
      ps.executeUpdate();
    } catch (SQLException e) { e.printStackTrace(); }
  }

  private static void insertTask(PreparedStatement ps, Task t, String parentId) throws SQLException {
    ps.setString(1, t.getId());
    ps.setString(2, t.getText());
    ps.setBoolean(3, t.isDone());
    ps.setBoolean(4, t.isMigrated());
    ps.setTimestamp(5, Timestamp.valueOf(t.getCreatedAt()));
    ps.setTimestamp(6, t.getCompletedAt() != null ? Timestamp.valueOf(t.getCompletedAt()) : null);
    ps.setString(7, t.getPriority().name());
    ps.setString(8, parentId);
    ps.addBatch();
    ps.executeBatch();
  }
}