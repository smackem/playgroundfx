package net.smackem.fxplayground.server;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.WindowEvent;
import net.smackem.fxplayground.App;

import java.io.IOException;
import java.util.concurrent.*;

public class ServerController {
    private final ObservableList<String> messages = FXCollections.observableArrayList();
    private final LocalServer server;
    private static final int PORT = 55555;

    @FXML
    public Label listeningLabel;

    @FXML
    private ListView<String> messagesList;

    public ServerController() {
        this.server = openServer();
        if (this.server != null) {
            this.server.subscribe(new UnboundedSubscriber<>(item ->
                    this.messages.add(item.toString())));
        }
    }

    @FXML
    private void initialize() {
        this.messagesList.setItems(this.messages);
        this.listeningLabel.setText(String.format("Listening on TCP port %d for utf8 lines (telnet)", PORT));
        App.instance().scene().getWindow().addEventFilter(
                WindowEvent.WINDOW_CLOSE_REQUEST, this::onCloseWindow);
    }

    private LocalServer openServer() {
        try {
            return new LocalServer(PORT, PlatformExecutor.INSTANCE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private <T extends Event> void onCloseWindow(T t) {
        if (this.server != null) {
            try {
                this.server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
