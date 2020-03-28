package net.smackem.fxplayground;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.layout.BorderPane;

public class MainController {

    @FXML
    private BorderPane root;

    @FXML
    private void showStage(ActionEvent actionEvent) throws IOException {
        final var source = actionEvent.getSource();
        if (source instanceof Node == false) {
            return;
        }
        final var widget = (Node)source;
        final String stage = (String)widget.getUserData();
        root.setCenter(App.loadFXML(stage));
    }
}
