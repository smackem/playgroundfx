package net.smackem.fxplayground.osc;

import com.illposed.osc.*;
import com.illposed.osc.transport.udp.OSCPortIn;
import com.illposed.osc.transport.udp.OSCPortInBuilder;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.WindowEvent;
import net.smackem.fxplayground.App;

import java.io.IOException;

public class OscController implements OSCPacketListener {
    @FXML
    private Canvas canvas;

    private final OSCPortIn inboundPort;

    public OscController() throws IOException {
        this.inboundPort = new OSCPortInBuilder()
                .setLocalPort(7770)
                .addPacketListener(this)
                .build();
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
        if (oscPacketEvent.getPacket() instanceof OSCBundle) {
            return;
        }
        if (oscPacketEvent.getPacket() instanceof OSCMessage) {
            return;
        }
        throw new IllegalArgumentException("invalid packet type: " + oscPacketEvent.getPacket());
    }

    @Override
    public void handleBadData(OSCBadDataEvent oscBadDataEvent) {

    }
}
