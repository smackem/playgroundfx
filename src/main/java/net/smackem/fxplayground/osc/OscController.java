package net.smackem.fxplayground.osc;

import com.illposed.osc.*;
import com.illposed.osc.transport.udp.OSCPortIn;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.WindowEvent;
import net.smackem.fxplayground.App;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class OscController implements OSCPacketListener {
    private static final Logger log = LoggerFactory.getLogger(OscController.class);

    @FXML
    private Canvas canvas;

    private final OSCPortIn inboundPort;

    public OscController() throws IOException {
        this.inboundPort = new OSCPortIn(7770);
        this.inboundPort.addPacketListener(this);
        this.inboundPort.startListening();
    }

    @FXML
    private void initialize() {
        App.instance().scene().getWindow().setOnCloseRequest(this::onWindowClosed);
        render();
    }

    private void onWindowClosed(WindowEvent windowEvent) {
        try {
            this.inboundPort.close();
        } catch (Exception e) {
            log.error("error closing osc port", e);
            e.printStackTrace();
        }
    }

    private void render() {
        final GraphicsContext gc = this.canvas.getGraphicsContext2D();
        gc.setFill(Color.AQUAMARINE);
        gc.fillRect(0, 0, 100, 100);
    }

    @Override
    public void handlePacket(OSCPacketEvent oscPacketEvent) {
        internalHandlePacket(oscPacketEvent.getPacket());
    }

    private void internalHandlePacket(OSCPacket packet) {
        if (packet instanceof OSCBundle bundle) {
            log.info("bundle received @ {}: {}", bundle.getTimestamp(), bundle.getPackets());
            for (final OSCPacket innerPacket : bundle.getPackets()) {
                internalHandlePacket(innerPacket);
            }
            return;
        }
        if (packet instanceof OSCMessage message) {
            log.info("message @ {}: {}", message.getAddress(), message.getInfo());
            return;
        }
        throw new IllegalArgumentException("invalid packet type: " + packet.getClass());
    }

    @Override
    public void handleBadData(OSCBadDataEvent oscBadDataEvent) {
        log.info("bad osc data: {}", oscBadDataEvent);
    }
}
