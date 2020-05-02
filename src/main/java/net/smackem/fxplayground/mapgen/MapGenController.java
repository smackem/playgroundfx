package net.smackem.fxplayground.mapgen;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.Effect;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.util.concurrent.ThreadLocalRandom;

public class MapGenController {

    private final Bitmap bitmap;
    private static final int MAX_VALUE = 255;

    @FXML
    private Canvas canvas;

    public MapGenController() {
        this.bitmap = Bitmap.random(160, 120, MAX_VALUE + 1);
    }

    @FXML
    private void initialize() {
        render();
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
    private void onSeed(ActionEvent actionEvent) {
        seed();
        render();
    }

    private void seed() {
        final double centerX = this.bitmap.width() / 2.0;
        final double centerY = this.bitmap.height() / 2.0;
        final double maxDistance = Math.sqrt(centerX * centerX + centerY * centerY);
        final int halfMax = MAX_VALUE / 2;
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int y = 0; y < this.bitmap.height(); y++) {
            for (int x = 0; x < this.bitmap.width(); x++) {
                final double dx = (double)x - centerX;
                final double dy = (double)y - centerY;
                final double distance = Math.sqrt(dx * dx + dy * dy);
                final double ratio = 1.0 - (distance / maxDistance);
                final int maxRandom = halfMax + (int)(ratio * halfMax);
                this.bitmap.set(x, y, random.nextInt(maxRandom));
            }
        }
    }

    private void render() {
        final GraphicsContext gc = this.canvas.getGraphicsContext2D();
        final int tileWidth = 4;
        final int tileHeight = 4;
        for (int y = 0; y < this.bitmap.height(); y++) {
            for (int x = 0; x < this.bitmap.width(); x++) {
                final int value = this.bitmap.get(x, y);
                gc.setFill(colorizeElevation(value));
                gc.fillRect(x * tileWidth, y * tileHeight, tileWidth, tileHeight);
            }
        }
//        final GaussianBlur blur = new GaussianBlur();
//        blur.setRadius(3);
//        gc.applyEffect(blur);
    }

    private static Paint colorizeHue(int value) {
        final double hue = value * 360.0 / MAX_VALUE;
        return Color.hsb(hue, 1.0, 1.0);
    }

    private static Paint colorizeGrey(int value) {
        return Color.rgb(value, value, value);
    }

    private static record ColorMapping(int min, int max, Color minColor, Color maxColor) {};

    private static final ColorMapping[] COLOR_MAPPINGS = new ColorMapping[] {
        new ColorMapping(0, 120, Color.hsb(240, 1, 0.5), Color.hsb(180, 0.7, 0.7)),
        new ColorMapping(121, 140, Color.hsb(60, 0.4, 0.7), Color.hsb(80, 1, 0.5)),
        new ColorMapping(141, 210, Color.hsb(80, 1, 0.5), Color.hsb(120, 1, 0.3)),
        new ColorMapping(211, 255, Color.hsb(120, 1, 0.3), Color.hsb(0, 0, 1)),
    };

    private static Paint colorizeElevation(int value) {
        for (final ColorMapping mapping : COLOR_MAPPINGS) {
            if (value >= mapping.min && value <= mapping.max) {
                final double range = mapping.max - mapping.min;
                final double ratio = (value - mapping.min) / range;
                return Color.rgb(
                    (int) (255 * (mapping.minColor.getRed() + (mapping.maxColor.getRed() - mapping.minColor.getRed()) * ratio)),
                    (int) (255 * (mapping.minColor.getGreen() + (mapping.maxColor.getGreen() - mapping.minColor.getGreen()) * ratio)),
                    (int) (255 * (mapping.minColor.getBlue() + (mapping.maxColor.getBlue() - mapping.minColor.getBlue()) * ratio)));
            }
        }
        throw new IllegalArgumentException();
    }

    private static int clamp(int value) {
        if (value > MAX_VALUE) {
            return MAX_VALUE;
        }
        return Math.max(value, 0);
    }

    private void smoothen() {
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
    }

    void expand() {
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
    }
}
