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

    private final Bitmap bitmap;
    private final BooleanProperty hintRenderingEnabled = new SimpleBooleanProperty(true);
    private static final int MAX_VALUE = 255;
    private static final int TILE_WIDTH = 2;
    private static final int TILE_HEIGHT = 2;

    private static record ColorMapping(int min, int max, Color minColor, Color maxColor) {};
    private static final ColorMapping[] COLOR_MAPPINGS = new ColorMapping[] {
            new ColorMapping(0, 120, Color.hsb(240, 1, 0.5), Color.hsb(180, 0.7, 0.7)),
            new ColorMapping(121, 140, Color.hsb(60, 0.4, 0.7), Color.hsb(80, 1, 0.5)),
            new ColorMapping(141, 210, Color.hsb(80, 1, 0.5), Color.hsb(120, 1, 0.3)),
            new ColorMapping(211, 240, Color.hsb(120, 1, 0.3), Color.hsb(60, 0.3, 0.5)),
            new ColorMapping(241, 255, Color.hsb(60, 0.1, 0.6), Color.hsb(0, 0, 1)),
    };

    private Geometry hintGeometry;
    private Bitmap.MinMax expansionRange;

    @FXML
    private Canvas canvas;

    @FXML
    private CheckBox renderHintCheckBox;

    public MapGenController() {
        this.bitmap = Bitmap.random(320, 240, MAX_VALUE + 1);
    }

    @FXML
    private void initialize() {
        render();
        this.renderHintCheckBox.selectedProperty().bindBidirectional(this.hintRenderingEnabled);
        this.hintRenderingEnabled.addListener(ignored -> render());
    }

    @FXML
    private void onSmoothen(ActionEvent actionEvent) {
        smoothen();
        render();
    }

    @FXML
    private void onExpand(ActionEvent actionEvent) {
        expand();
        render();
    }

    @FXML
    private void onMap(ActionEvent actionEvent) {
        for (int j = 0; j < 3; j++) {
            for (int i = 0; i < 10; i++) {
                smoothen();
            }
            expand();
        }
        render();
    }

    @FXML
    private void onSeedIsland(ActionEvent actionEvent) {
        seedIsland();
        render();
    }

    @FXML
    private void onSeedMountains(ActionEvent actionEvent) {
        seedMountains();
        render();
    }

    @FXML
    private void onSeedHighPlains(ActionEvent actionEvent) {
        seedHighPlains();
        render();
    }

    private void seedIsland() {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final int height = this.bitmap.height();
        final int width = this.bitmap.width();
        final double centerX = width / 2.0;
        final double centerY = height / 2.0;
        //noinspection UnnecessaryLocalVariable
        final double maxDistance = centerY;//Math.sqrt(centerX * centerX + centerY * centerY);
        final int halfMax = MAX_VALUE / 2;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final double dx = (double)x - centerX;
                final double dy = (double)y - centerY;
                final double distance = Math.sqrt(dx * dx + dy * dy);
                final double ratio = 1.0 - (distance / maxDistance);
                final int maxRandom = halfMax + (int)(ratio * halfMax);
                this.bitmap.set(x, y, random.nextInt(maxRandom));
            }
        }
        this.hintGeometry = null;
        this.expansionRange = null;
    }

    private void seedMountains() {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final int height = this.bitmap.height();
        final int width = this.bitmap.width();
        final GeometryFactory gf = new GeometryFactory();
        final Coordinate[] coordinates = new Coordinate[5];
        for (int i = 0; i < coordinates.length; i++) {
            coordinates[i] = new Coordinate(
                    random.nextInt(width),
                    random.nextInt(height));
        }
        final Geometry mountainPeaks = gf.createLineString(coordinates);
        final double maxDistance = Math.sqrt(
                width * width + height * height);
        final int halfMax = MAX_VALUE / 2;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final Geometry point = gf.createPoint(new Coordinate(x, y));
                final double distance = point.distance(mountainPeaks);
                final double ratio = 1.0 - (distance / maxDistance);
                final int value = (int)(ratio * halfMax);
                this.bitmap.set(x, y, value + random.nextInt(halfMax));
            }
        }
        this.hintGeometry = mountainPeaks;
        this.expansionRange = null;
    }

    private void seedHighPlains() {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final int height = this.bitmap.height();
        final int width = this.bitmap.width();
        final GeometryFactory gf = new GeometryFactory();
        final Geometry[] circles = new Geometry[8];
        for (int i = 0; i < circles.length; i++) {
            circles[i] = gf.createPoint(new Coordinate(
                    random.nextInt(width),
                    random.nextInt(height)))
                .buffer(20 + random.nextInt(this.bitmap.height() / 4));
        }
        final Geometry plains = gf.createGeometryCollection(circles);
        final int halfMax = MAX_VALUE / 2;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final Geometry point = gf.createPoint(new Coordinate(x, y));
                int value = random.nextInt(halfMax);
                if (plains.intersects(point)) {
                    value += halfMax;
                }
                this.bitmap.set(x, y, value);
            }
        }
        this.hintGeometry = plains;
        this.expansionRange = new Bitmap.MinMax(140, 220);
    }

    private void render() {
        final int height = this.bitmap.height();
        final int width = this.bitmap.width();
        final GraphicsContext gc = this.canvas.getGraphicsContext2D();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int value = this.bitmap.get(x, y);
                gc.setFill(colorizeElevation(value));
                gc.fillRect(x * TILE_WIDTH, y * TILE_HEIGHT, TILE_WIDTH, TILE_HEIGHT);
            }
        }
        if (this.hintGeometry != null && this.hintRenderingEnabled.get()) {
            renderGeometry(gc, this.hintGeometry);
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

    private static int clamp(int value) {
        if (value > MAX_VALUE) {
            return MAX_VALUE;
        }
        return Math.max(value, 0);
    }

    private void smoothen() {
        final int height = this.bitmap.height();
        final int width = this.bitmap.width();
        final Bitmap source = Bitmap.copyOf(this.bitmap);
        final Bitmap kernel = Bitmap.kernel(
                0, 1, 2, 1, 0,
                1, 2, 4, 2, 1,
                2, 4, 8, 4, 2,
                1, 2, 4, 2, 1,
                0, 1, 2, 1, 0);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int v = source.convolute(x, y, kernel);
                this.bitmap.set(x, y, clamp(v));
            }
        }
    }

    void expand() {
        final int height = this.bitmap.height();
        final int width = this.bitmap.width();
        final Bitmap source = Bitmap.copyOf(this.bitmap);
        final Bitmap.MinMax minMax = source.minMax();
        final double sourceRange = minMax.max() - minMax.min();
        final int min = this.expansionRange != null ? this.expansionRange.min() : 0;
        final int max = this.expansionRange != null ? this.expansionRange.max() : MAX_VALUE;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final double value = source.get(x, y);
                final double ratio = (value - minMax.min()) / sourceRange;
                this.bitmap.set(x, y, min + (int) (ratio * (max - min)));
            }
        }
    }
}
