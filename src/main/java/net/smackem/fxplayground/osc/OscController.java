package net.smackem.fxplayground.osc;

import com.google.common.base.Joiner;
import com.illposed.osc.*;
import com.illposed.osc.transport.udp.OSCPortIn;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.WindowEvent;
import net.smackem.fxplayground.App;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class OscController implements OSCPacketListener {
    private static final Logger log = LoggerFactory.getLogger(OscController.class);
    private final OSCPortIn inboundPort;
    private final List<Figure> figures = new ArrayList<>();

    @FXML
    private Canvas canvas;

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
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, 1000, 1000);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(3);
        for (final Figure figure : this.figures) {
            Point2D prevPt = null;
            for (final Point2D point : figure.points()) {
                if (prevPt != null) {
                    gc.strokeLine(prevPt.getX(), prevPt.getY(), point.getX(), point.getY());
                }
                prevPt = point;
            }
        }
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
            log.info("message @ {}: {}", message.getAddress(), Joiner.on(", ").join(message.getArguments()));
            switch (message.getAddress()) {
                case "/figure/begin" -> handleFigureBegin(message);
                case "/figure/point" -> handleFigurePoint(message);
                case "/figure/end" -> handleFigureEnd(message);
            }
            return;
        }
        throw new IllegalArgumentException("invalid packet type: " + packet.getClass());
    }

    @Override
    public void handleBadData(OSCBadDataEvent oscBadDataEvent) {
        log.info("bad osc data: {}", oscBadDataEvent);
    }

    private void handleFigureBegin(OSCMessage message) {
        final Figure figure = currentFigure();
        if (figure != null && figure.points().size() <= 1) {
            this.figures.remove(this.figures.size() - 1);
        }
        final Figure newFigure = new Figure();
        final List<Object> args = message.getArguments();
        newFigure.points().add(new Point2D((float) args.get(0), (float) args.get(1)));
        this.figures.add(newFigure);
    }

    private void handleFigurePoint(OSCMessage message) {
        final Figure figure = currentFigure();
        if (figure == null) {
            return;
        }
        final List<Object> args = message.getArguments();
        figure.points().add(new Point2D((float) args.get(0), (float) args.get(1)));
        render();
    }

    private void handleFigureEnd(OSCMessage message) {
    }

    private Figure currentFigure() {
        return this.figures.size() > 0 ? this.figures.get(this.figures.size() - 1) : null;
    }
}
