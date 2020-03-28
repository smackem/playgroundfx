package net.smackem.fxplayground;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.util.GeometricShapeFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class JtsController {

    private final Collection<Shape> shapes = new ArrayList<>();

    @FXML
    private Canvas canvas;

    @FXML
    private void initialize() {
        createShapes();
        draw();
    }

    private void draw() {
        final GraphicsContext gc = this.canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, this.canvas.getWidth(), this.canvas.getHeight());

        for (final var shape : this.shapes) {
            drawShape(gc, shape);
        }
    }

    private void drawShape(GraphicsContext gc, Shape shape) {
        gc.save();
        gc.setFill(shape.computed
                ? Color.rgb(0x40, 0x40, 0xff, 0.7)
                : Color.rgb(0xff, 0xc0, 0x00, 0.7));
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2.0);
        gc.beginPath();
        for (final var coordinate : shape.geometry.getCoordinates()) {
            gc.lineTo(coordinate.x, coordinate.y);
        }
        gc.closePath();
        gc.fill();
        gc.stroke();
        gc.restore();
    }

    private void createShapes() {
        final GeometryFactory gf = new GeometryFactory();
        final Geometry circle1 = createCircle(100, 80, 18);
        final Geometry circle2 = createCircle(300, 260, 150);
        final Geometry line1 = createLine(gf, 0, 0, 600, 400);
        final Geometry line2 = createLine(gf, 500, 0, 0, 400);
        final Geometry intersection1 = circle1.intersection(line1);
        final Geometry intersection2 = line2.intersection(line1);
        final Geometry intersection3 = circle2.intersection(line2);
        this.shapes.addAll(Arrays.asList(
                new Shape(circle1, false),
                new Shape(circle2, false),
                new Shape(line1, false),
                new Shape(line2, false),
                new Shape(intersection1.buffer(5), true),
                new Shape(intersection3.buffer(5), true),
                new Shape(intersection2.buffer(5), true)
        ));
    }

    private static LineString createLine(GeometryFactory gf, double x1, double y1, double x2, double y2) {
        return gf.createLineString(new Coordinate[]{
                new Coordinate(x1, y1),
                new Coordinate(x2, y2),
        });
    }

    private static Geometry createCircle(double x, double y, double radius) {
        final GeometricShapeFactory shapeFactory = new GeometricShapeFactory();
        shapeFactory.setNumPoints(16);
        shapeFactory.setCentre(new Coordinate(x, y));
        shapeFactory.setSize(radius * 2);
        return shapeFactory.createCircle();
//        final GeometryFactory gf = new GeometryFactory();
//        return gf.createPoint(new Coordinate(x, y)).buffer(radius);
    }

    private static class Shape {
        final Geometry geometry;
        final boolean computed;

        private Shape(Geometry geometry, boolean computed) {
            this.geometry = geometry;
            this.computed = computed;
        }
    }
}
