package io.olmosjt.terminaltodo;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigService {
  private static final String CONFIG_FILE = System.getProperty("user.home") + "/.daily_plan.conf";
  private static final Properties props = new Properties();

  // Load config automatically when class is accessed
  static {
    try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
      props.load(in);
    } catch (IOException e) {
      // File doesn't exist yet, ignore
    }
  }

  public static String getTheme() {
    return props.getProperty("theme", "theme-gruvbox"); // Default to Gruvbox
  }

  public static void setTheme(String theme) {
    props.setProperty("theme", theme);
    save();
  }

  private static void save() {
    try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
      props.store(out, "TerminalTodo Configuration");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
