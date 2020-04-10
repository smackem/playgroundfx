package net.smackem.fxplayground.server;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

import java.io.IOException;
import java.util.concurrent.ForkJoinPool;

public class ServerController {
    private LocalServer server;
    private final ObservableList<String> messages = FXCollections.observableArrayList();

    @FXML
    private ListView<String> messagesList;

    public ServerController() {
        try {
            this.server = new LocalServer(55555, ForkJoinPool.commonPool());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        this.server.subscribe(new OneByOneSubscriber<>(messages::add));
    }

    @FXML
    private void initialize() {
        this.messagesList.setItems(this.messages);
    }
}
