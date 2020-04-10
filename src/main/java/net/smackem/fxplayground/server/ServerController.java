package net.smackem.fxplayground.server;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.stage.WindowEvent;
import net.smackem.fxplayground.App;

import java.io.IOException;
import java.util.concurrent.*;

public class ServerController {
    private final ObservableList<String> messages = FXCollections.observableArrayList();
    private final LocalServer server;
    private final ExecutorService executorService;

    @FXML
    private ListView<String> messagesList;

    public ServerController() {
        this.executorService = Executors.newSingleThreadExecutor();
        this.server = openServer(this.executorService);
        if (this.server != null) {
            this.server.subscribe(new UnboundedSubscriber<>(item ->
                    Platform.runLater(() -> this.messages.add(item))));
        }
    }

    @FXML
    private void initialize() {
        this.messagesList.setItems(this.messages);
        App.instance().scene().getWindow().addEventFilter(
                WindowEvent.WINDOW_CLOSE_REQUEST, this::onCloseWindow);
    }

    private LocalServer openServer(Executor executor) {
        try {
            return new LocalServer(55555, executor);
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
        this.executorService.shutdown();
        try {
            this.executorService.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
