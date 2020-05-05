package net.smackem.fxplayground.mapgen;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.CheckBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

import java.util.concurrent.ThreadLocalRandom;

public class MapGenController {

    private final ElevationMap map;
    private final BooleanProperty hintRenderingEnabled = new SimpleBooleanProperty(true);
    private static final int MAX_VALUE = 255;
    private static final int TILE_WIDTH = 2;
    private static final int TILE_HEIGHT = 2;

    private static final ColorMapping[] COLOR_MAPPINGS = new ColorMapping[] {
            new ColorMapping(0, 120, Color.hsb(240, 1, 0.5), Color.hsb(180, 0.7, 0.7)),
            new ColorMapping(121, 140, Color.hsb(60, 0.4, 0.7), Color.hsb(80, 1, 0.5)),
            new ColorMapping(141, 210, Color.hsb(80, 1, 0.5), Color.hsb(120, 1, 0.3)),
            new ColorMapping(211, 240, Color.hsb(120, 1, 0.3), Color.hsb(60, 0.3, 0.5)),
            new ColorMapping(241, 255, Color.hsb(60, 0.1, 0.6), Color.hsb(0, 0, 1)),
    };

    @FXML
    private Canvas canvas;

    @FXML
    private CheckBox renderHintCheckBox;

    public MapGenController() {
        this.map = new ElevationMap(320, 240);
    }

    @FXML
    private void initialize() {
        render();
        this.renderHintCheckBox.selectedProperty().bindBidirectional(this.hintRenderingEnabled);
        this.hintRenderingEnabled.addListener(ignored -> render());
    }

    @FXML
    private void onSmoothen(ActionEvent actionEvent) {
        this.map.smoothen();
        render();
    }

    @FXML
    public void onSharpen(ActionEvent actionEvent) {
        this.map.sharpen();
        render();
    }

    @FXML
    private void onExpand(ActionEvent actionEvent) {
        this.map.expand();
        render();
    }

    @FXML
    private void onMap(ActionEvent actionEvent) {
        this.map.generate();
        render();
    }

    @FXML
    private void onSeedIsland(ActionEvent actionEvent) {
        this.map.seedIsland();
        render();
    }

    @FXML
    private void onSeedMountains(ActionEvent actionEvent) {
        this.map.seedMountains();
        render();
    }

    @FXML
    private void onSeedHighPlains(ActionEvent actionEvent) {
        this.map.seedHighPlains();
        render();
    }

    @FXML
    public void onSeedLakes(ActionEvent actionEvent) {
        this.map.seedLakes();
        render();
    }

    @FXML
    private void onSeedRiver(ActionEvent actionEvent) {
        this.map.seedRiver();
        render();
    }

    private static record ColorMapping(int min, int max, Color minColor, Color maxColor) {};

    private void render() {
        final Bitmap bitmap = this.map.bitmap();
        final int height = bitmap.height();
        final int width = bitmap.width();
        final GraphicsContext gc = this.canvas.getGraphicsContext2D();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int value = bitmap.get(x, y);
                gc.setFill(colorizeElevation(value));
                gc.fillRect(x * TILE_WIDTH, y * TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT);
            }
        }
        if (this.map.hintGeometry() != null && this.hintRenderingEnabled.get()) {
            renderGeometry(gc, this.map.hintGeometry());
        }
//        final GaussianBlur blur = new GaussianBlur();
//        blur.setRadius(3);
//        gc.applyEffect(blur);
    }

    private void renderGeometry(GraphicsContext gc, Geometry geometry) {
        gc.save();
        gc.setFill(Color.rgb(255, 255, 255, 0.3));
        gc.setStroke(Color.RED);
        gc.setLineWidth(2.0);
        for (int i = 0; i < geometry.getNumGeometries(); i++) {
            renderInnerGeometry(gc, geometry.getGeometryN(i));
        }
        gc.restore();
    }

    private void renderInnerGeometry(GraphicsContext gc, Geometry geometry) {
        gc.beginPath();
        for (final var coordinate : geometry.getCoordinates()) {
            gc.lineTo(coordinate.x * TILE_WIDTH, coordinate.y * TILE_HEIGHT);
        }
        if (geometry.getArea() > 0) {
            gc.fill();
        }
        gc.stroke();
        gc.closePath();
    }

    private static Paint colorizeHue(int value) {
        final double hue = value * 360.0 / MAX_VALUE;
        return Color.hsb(hue, 1.0, 1.0);
    }

    private static Paint colorizeGrey(int value) {
        return Color.rgb(value, value, value);
    }

    private static Paint colorizeElevation(int value) {
        for (final ColorMapping mapping : COLOR_MAPPINGS) {
            if (value >= mapping.min && value <= mapping.max) {
                final double range = mapping.max - mapping.min;
                final double ratio = (value - mapping.min) / range;
                return interpolate(mapping.minColor, mapping.maxColor, ratio);
            }
        }
        throw new IllegalArgumentException();
    }

    private static Color interpolate(Color origin, Color dest, double ratio) {
        return Color.rgb(
                (int) (255 * (origin.getRed() + (dest.getRed() - origin.getRed()) * ratio)),
                (int) (255 * (origin.getGreen() + (dest.getGreen() - origin.getGreen()) * ratio)),
                (int) (255 * (origin.getBlue() + (dest.getBlue() - origin.getBlue()) * ratio)));
    }
}
