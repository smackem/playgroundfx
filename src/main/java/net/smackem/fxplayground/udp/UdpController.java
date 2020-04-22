package net.smackem.fxplayground.udp;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import net.smackem.fxplayground.App;
import net.smackem.fxplayground.PlatformExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;

public class UdpController {

    private static final Logger log = LoggerFactory.getLogger(UdpController.class);
    private final Timeline ticker;
    private final UdpMultiListener listener;
    private final LocalUdpClient client;
    private final ObservableList<RemoteHostViewModel> remoteHosts = FXCollections.observableArrayList();

    @FXML
    private ListView<RemoteHostViewModel> hostList;

    private static class RemoteHostViewModel {
        RemoteHostViewModel(String remoteAddress) {
            this.remoteAddressProperty.set(remoteAddress);
        }

        final StringProperty remoteAddressProperty = new SimpleStringProperty();
        final StringProperty latestMessageProperty = new SimpleStringProperty();
    }

    public UdpController() throws IOException {
        final int[] ports = new int[] {
                40550, 40551, 40552, 40553, 40554, 40555, 40556, 40557, 40558, 40559,
        };
        this.listener = new UdpMultiListener(ports);
        this.client = new LocalUdpClient(ports);
        this.client.messageReceivedEvent().subscribe(this::onRemoteHostMessage);
        this.ticker = new Timeline(new KeyFrame(Duration.seconds(3), this::tick));
        this.ticker.setCycleCount(Animation.INDEFINITE);
    }

    private void tick(ActionEvent actionEvent) {
        final String message = "Hello" + LocalDateTime.now().getSecond();
        log.debug("sending {}", message);
        this.client.sendToAll(message);
    }

    @FXML
    private void initialize() {
        this.hostList.itemsProperty().set(this.remoteHosts);
        App.instance().scene().getWindow().setOnCloseRequest(this::onWindowClosed);
        this.ticker.play();
        this.hostList.setCellFactory(list -> new RemoteHostCell());
    }

    private void onRemoteHostMessage(LocalUdpClient.Message message) {
        PlatformExecutor.INSTANCE.execute(() -> this.remoteHosts.stream()
                .filter(remoteHost -> Objects.equals(remoteHost.remoteAddressProperty.get(), message.remoteAddress()))
                .findFirst()
                .ifPresentOrElse(
                        remoteHost -> {
                            log.debug("FOUND {}, saying {}", message.remoteAddress(), message.text());
                            remoteHost.latestMessageProperty.set(message.text());
                        },
                        () -> {
                            log.debug("NEW {}, saying {}", message.remoteAddress(), message.text());
                            final var remoteHost = new RemoteHostViewModel(message.remoteAddress());
                            remoteHost.latestMessageProperty.set(message.text());
                            this.remoteHosts.add(remoteHost);
                        }));
    }

    private void onWindowClosed(WindowEvent windowEvent) {
        this.ticker.stop();
        try {
            this.client.close();
        } catch (IOException | InterruptedException ignored) { }
        try {
            this.listener.close();
        } catch (IOException | InterruptedException ignored) { }
    }

    private static class RemoteHostCell extends ListCell<RemoteHostViewModel> {
        @Override
        protected void updateItem(RemoteHostViewModel remoteHost, boolean empty) {
            super.updateItem(remoteHost, empty);
            if (empty ) {
                textProperty().unbind();
                setText(null);
                return;
            }
            textProperty().bind(Bindings.format("%s\n%s",
                    remoteHost.remoteAddressProperty, remoteHost.latestMessageProperty));
        }
    }
}
