module io.olmosjt.terminaltodo {
  requires javafx.controls;
  requires javafx.fxml;


  opens io.olmosjt.terminaltodo to javafx.fxml;
  exports io.olmosjt.terminaltodo;
}