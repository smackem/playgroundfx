package net.smackem.fxplayground;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.paint.Color;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.util.AffineTransformation;

import java.util.ArrayList;
import java.util.Collection;

public class GeometryImageController {
    private static final double CX_RATIO = 16.0;
    private static final double CY_RATIO = 16.0;
    private Geometry geometry;

    @FXML
    private Canvas canvas;

    @FXML
    private void initialize() {
    }

    private Geometry importFromImage(Image image) {
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
                final int pixel = buffer[i];
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
                i++;
            }
        }

        return unionGeometries(geometries);
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
}
