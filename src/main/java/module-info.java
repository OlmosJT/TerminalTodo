module io.olmosjt.terminaltodo {
  requires javafx.controls;
  requires javafx.fxml;
  requires java.sql;
  requires static lombok;

  exports io.olmosjt.terminaltodo.ui;
  opens io.olmosjt.terminaltodo.ui to javafx.graphics;
  exports io.olmosjt.terminaltodo;
}