package net.smackem.fxplayground.mapgen;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class MapGenController {

    private final Bitmap bitmap;

    @FXML
    private Canvas canvas;

    public MapGenController() {
        this.bitmap = Bitmap.random(80, 60);
    }

    @FXML
    private void initialize() {
        render();
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
                this.bitmap.set(x, y, source.convolute(x, y, kernel));
            }
        }
    }

    private void render() {
        final GraphicsContext gc = this.canvas.getGraphicsContext2D();
        final int tileWidth = 8;
        final int tileHeight = 8;
        final double maxElevation = 255.0;
        for (int y = 0; y < this.bitmap.height(); y++) {
            for (int x = 0; x < this.bitmap.width(); x++) {
                final double hue = this.bitmap.get(x, y) * 360.0 / maxElevation;
                gc.setFill(Color.hsb(hue, 1.0, 1.0));
                gc.fillRect(x * tileWidth, y * tileHeight, tileWidth, tileHeight);
            }
        }
    }

    @FXML
    private void process(ActionEvent actionEvent) {
        smoothen();
        render();
    }
}
