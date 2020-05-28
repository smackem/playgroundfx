package net.smackem.fxplayground.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SampleMqttClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(SampleMqttClient.class);
    private final MqttClient mqtt;
    private final String name;
    private final ScheduledExecutorService executor;
    private final String topic = "/smackem/playgroundfx/mqtt/sample";
    private int count;

    public SampleMqttClient(String name) throws IOException {
        this.name = name;
        final String broker = "tcp://localhost:55555";
        final MemoryPersistence persistence = new MemoryPersistence();
        try {
            this.mqtt = new MqttClient(broker, name, persistence);
            final MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            this.mqtt.connect(connOpts);
            this.mqtt.subscribe(this.topic, this::onMessage);
        } catch (MqttException e) {
            throw new IOException(e);
        }
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.executor.scheduleAtFixedRate(this::tick, 1, 1, TimeUnit.SECONDS);
    }

    private void onMessage(String topic, MqttMessage message) {
        log.info("received @ {}: {}", topic, new String(message.getPayload(), StandardCharsets.UTF_8));
    }

    private void tick() {
        final String content = "Message from %s @ %d".formatted(this.name, ++this.count);
        final int qos = 2;
        try {
            final MqttMessage message = new MqttMessage(content.getBytes(StandardCharsets.UTF_8));
            message.setQos(qos);
            this.mqtt.publish(this.topic, message);
        } catch (MqttException me) {
            log.info("reason " + me.getReasonCode());
            log.info("msg " + me.getMessage());
            log.info("loc " + me.getLocalizedMessage());
            log.info("cause " + me.getCause());
            log.error("error publishing message", me);
        }
    }

    @Override
    public void close() {
        if (this.mqtt != null) {
//            try {
//                this.mqtt.disconnectForcibly();
//            } catch (Exception ignored) {
//                // ignore
//            }
            try {
                this.mqtt.close();
            } catch (Exception ignored) {
                // ignore
            }
        }
        this.executor.shutdown();
        try {
            this.executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            // ignore
        }
    }
}
