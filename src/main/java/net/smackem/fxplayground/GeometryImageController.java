package net.smackem.fxplayground;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.util.AffineTransformation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class GeometryImageController {
    private static final double CX_RATIO = 16.0;
    private static final double CY_RATIO = 16.0;
    private final Collection<Geometry> geometries = new ArrayList<>();

    @FXML
    private Canvas canvas;

    @FXML
    private void initialize() {
    }

    private void render() {
        final GraphicsContext gc = this.canvas.getGraphicsContext2D();
        for (final Geometry geometry : this.geometries) {
            renderGeometry(gc, geometry);
        }
    }

    private void renderGeometry(GraphicsContext gc, Geometry geometry) {
        gc.save();
        gc.setFill(Color.LAWNGREEN);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2.0);
        for (int i = 0; i < geometry.getNumGeometries(); i++) {
            renderInnerGeometry(gc, geometry.getGeometryN(i));
        }
        gc.restore();
    }

    private void renderInnerGeometry(GraphicsContext gc, Geometry geometry) {
        gc.beginPath();
        for (final var coordinate : geometry.getCoordinates()) {
            gc.lineTo(coordinate.x, coordinate.y);
        }
        if (geometry.getArea() > 0) {
            gc.fill();
        }
        gc.stroke();
        gc.closePath();
    }

    private Collection<Geometry> importFromImage(Image image) {
        final Collection<Geometry> geometries = new ArrayList<>();
        final int width = (int) image.getWidth();
        final int height = (int) image.getHeight();
        final PixelReader reader = image.getPixelReader();
        final int[] buffer = new int[width * height];
        final GeometryFactory gf = new GeometryFactory();
        final Geometry geometryTemplate = gf.createPolygon(new Coordinate[] {
                new Coordinate(0, 0),
                new Coordinate(CX_RATIO, 0),
                new Coordinate(CX_RATIO, CY_RATIO),
                new Coordinate(0, CY_RATIO),
                new Coordinate(0, 0),
        });

        this.canvas.setWidth(width * CX_RATIO);
        this.canvas.setHeight(height * CY_RATIO);

        reader.getPixels(0, 0, width, height, WritablePixelFormat.getIntArgbInstance(), buffer, 0, width);

        int i = 0;
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                final int pixel = buffer[i++];
                final Color color = Color.rgb(
                        (pixel >> 16) & 0xff,
                        (pixel >> 8) & 0xff,
                        pixel & 0xff);
                if (color.getBrightness() > 0.5) {
                    continue;
                }
                final Geometry geometry = gf.createGeometry(geometryTemplate);
                geometry.apply(AffineTransformation.translationInstance(col * CX_RATIO, row * CY_RATIO));
                geometry.geometryChanged();
                geometries.add(geometry);
            }
        }

        //return geometries;
        return List.of(unionGeometries(geometries));
    }

    private Geometry unionGeometries(Collection<Geometry> geometries) {
        Geometry firstGeometry = null;
        for (final Geometry geometry : geometries) {
            if (firstGeometry == null) {
                firstGeometry = geometry;
            } else {
                firstGeometry = firstGeometry.union(geometry);
            }
        }
        return firstGeometry;
    }

    @FXML
    private void loadImage(ActionEvent actionEvent) throws IOException {
        final FileChooser dialog = new FileChooser();
        final File file = dialog.showOpenDialog(null);
        if (file == null) {
            return;
        }
        try (final InputStream is = new FileInputStream(file.getAbsoluteFile())) {
            final Image image = scaleImage(new Image(is));
            this.geometries.addAll(importFromImage(image));
            render();
        }
    }

    private static Image scaleImage(Image image) {
        final ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(40);
        imageView.setFitHeight(30);
        imageView.setSmooth(true);
        return imageView.snapshot(null, null);
    }
}
