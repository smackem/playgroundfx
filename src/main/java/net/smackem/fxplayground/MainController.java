package net.smackem.fxplayground;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;

public class MainController {

    @FXML
    private BorderPane root;

    @FXML
    private void showExplosion(ActionEvent actionEvent) throws IOException {
        root.setCenter(App.loadFXML("explosion"));
    }
}
