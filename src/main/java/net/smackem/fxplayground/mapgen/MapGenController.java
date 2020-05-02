package net.smackem.fxplayground.mapgen;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

public class MapGenController {

    private final Bitmap bitmap;
    private static final int MAX_VALUE = 255;

    @FXML
    private Canvas canvas;

    public MapGenController() {
        this.bitmap = Bitmap.random(80, 60, MAX_VALUE + 1);
    }

    @FXML
    private void initialize() {
        render();
    }

    private void render() {
        final GraphicsContext gc = this.canvas.getGraphicsContext2D();
        final int tileWidth = 8;
        final int tileHeight = 8;
        for (int y = 0; y < this.bitmap.height(); y++) {
            for (int x = 0; x < this.bitmap.width(); x++) {
                final int value = this.bitmap.get(x, y);
                gc.setFill(colorize(value));
                gc.fillRect(x * tileWidth, y * tileHeight, tileWidth, tileHeight);
            }
        }
    }

    private static Paint colorize(int value) {
//        final double hue = value * 360.0 / MAX_VALUE;
//        return Color.hsb(hue, 1.0, 1.0);
        return Color.rgb(value, value, value);
    }

    private static int clamp(int value) {
        if (value > MAX_VALUE) {
            return MAX_VALUE;
        }
        return Math.max(value, 0);
    }

    @FXML
    private void onSmoothen(ActionEvent actionEvent) {
        final Bitmap source = Bitmap.copyOf(this.bitmap);
        final Bitmap kernel = Bitmap.kernel(
                0, 1, 2, 1, 0,
                1, 2, 4, 2, 1,
                2, 4, 8, 4, 2,
                1, 2, 4, 2, 1,
                0, 1, 2, 1, 0);
        for (int y = 0; y < source.height(); y++) {
            for (int x = 0; x < source.width(); x++) {
                final int v = source.convolute(x, y, kernel);
                this.bitmap.set(x, y, clamp(v));
            }
        }
        render();
    }

    @FXML
    private void onExpand(ActionEvent actionEvent) {
        final Bitmap source = Bitmap.copyOf(this.bitmap);
        final Bitmap.MinMax minMax = source.minMax();
        final double sourceRange = minMax.max() - minMax.min();
        for (int y = 0; y < source.height(); y++) {
            for (int x = 0; x < source.width(); x++) {
                final double value = source.get(x, y);
                final double ratio = (value - minMax.min()) / sourceRange;
                this.bitmap.set(x, y, (int) (ratio * MAX_VALUE));
            }
        }
        render();
    }
}
