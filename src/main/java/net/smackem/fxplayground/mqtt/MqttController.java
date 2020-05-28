package net.smackem.fxplayground.mqtt;

import io.moquette.BrokerConstants;
import io.moquette.broker.Server;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.stage.WindowEvent;
import net.smackem.fxplayground.App;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

public class MqttController {
    private final Server mqttServer;
    private final Collection<SampleMqttClient> clients = new ArrayList<>();

    public MqttController() throws IOException {
        final Properties properties = new Properties();
        properties.setProperty(BrokerConstants.PORT_PROPERTY_NAME, String.valueOf(55555));
        this.mqttServer = new Server();
        this.mqttServer.startServer(properties);
        //Runtime.getRuntime().addShutdownHook(new Thread(this.mqttServer::stopServer));
    }

    @FXML
    private void initialize() {
        App.instance().scene().getWindow().addEventFilter(
                WindowEvent.WINDOW_CLOSE_REQUEST, this::onCloseWindow);
    }

    private <T extends Event> void onCloseWindow(T t) {
        for (final SampleMqttClient client : this.clients) {
            client.close();
        }
        if (this.mqttServer != null) {
            this.mqttServer.stopServer();
        }
    }

    @FXML
    private void newClient(ActionEvent actionEvent) throws Exception {
        final var client = new SampleMqttClient("client " + this.clients.size());
        this.clients.add(client);
    }
}
