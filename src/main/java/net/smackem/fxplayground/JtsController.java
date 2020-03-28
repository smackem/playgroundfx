package net.smackem.fxplayground;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.util.AffineTransformation;
import org.locationtech.jts.util.GeometricShapeFactory;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class JtsController {

    private final Collection<Shape> shapes = new ArrayList<>();

    @FXML
    private Canvas canvas;

    @FXML
    private void initialize() {
        this.shapes.addAll(createShapes2());
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
        gc.setFill(shape.paint);
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

    private static Collection<Shape> createShapes() {
        final GeometryFactory gf = new GeometryFactory();
        final Geometry circle1 = createCircle(100, 80, 18);
        final Geometry circle2 = createCircle(300, 260, 150);
        final Geometry circle3 = createCircle(500, 10, 5);
        final Geometry circle4 = createCircle(450, 40, 30);
        final Geometry circle5 = createCircle(20, 400, 100);
        final Geometry circle6 = createCircle(400, 300, 80);
        final Geometry line1 = createLine(gf, 0, 0, 600, 400);
        final Geometry line2 = createLine(gf, 500, 0, 0, 400);
        final Geometry intersection1 = circle1.intersection(line1);
        final Geometry intersection2 = line2.intersection(line1);
        final Geometry intersection3 = circle2.intersection(line2);
        intersection1.apply(AffineTransformation.translationInstance(10, 20));
        final Paint paint = Color.rgb(0xff, 0xc0, 0x00, 0.7);
        final Paint paintComputed = Color.rgb(0x40, 0x80, 0xff, 0.7);
        return Arrays.asList(
                new Shape(circle1, false, paint),
                new Shape(circle2, false, paint),
                new Shape(circle3, false, paint),
                new Shape(circle4, false, paint),
                new Shape(circle5, false, paint),
                new Shape(circle6, false, paint),
                new Shape(line1, false, paint),
                new Shape(line2, false, paint),
                new Shape(intersection1.buffer(5), true, paintComputed),
                new Shape(intersection3.buffer(5), true, paintComputed),
                new Shape(intersection2.buffer(5), true, paintComputed)
        );
    }

    private static Collection<Shape> createShapes2() {
        final List<Shape> shapes = new ArrayList<>();
        final var random = ThreadLocalRandom.current();
        for (int i = 0; i < 20; i++) {
            final int radius = random.nextInt(5, 100);
            final int x = random.nextInt(640);
            final int y = random.nextInt(480);
            final Paint paint = Color.rgb(random.nextInt(256),
                    random.nextInt(256),
                    random.nextInt(256),
                    0.6);
            shapes.add(new Shape(createCircle(x, y, radius), false, paint));
        }
        final GeometryFactory gf = new GeometryFactory();
        for (int i = 0; i < 10; i++) {
            final int x1 = random.nextInt(640);
            final int y1 = random.nextInt(480);
            final int x2 = random.nextInt(640);
            final int y2 = random.nextInt(480);
            final var line = new Shape(createLine(gf, x1, y1, x2, y2), false, Color.WHITE);
            final int count = shapes.size();
            for (int j = 0; j < count; j++) {
                final var shape = shapes.get(j);
                final var geometry = line.geometry.intersection(shape.geometry);
                if (geometry instanceof LineString) {
                    shapes.add(new Shape(geometry.buffer(5), true, Color.WHITE));
                }
            }
            shapes.add(line);
        }
        return shapes;
    }

    private static LineString createLine(GeometryFactory gf, double x1, double y1, double x2, double y2) {
        return gf.createLineString(new Coordinate[]{
                new Coordinate(x1, y1),
                new Coordinate(x2, y2),
        });
    }

    private static Geometry createCircle(double x, double y, double radius) {
        final GeometricShapeFactory shapeFactory = new GeometricShapeFactory();
        // maximum 64 points, climbing fast: 30 for radius 18
        final int numPoints = (int)(Math.atan(radius / 20.0) * 64.0 / Math.atan(100));
        shapeFactory.setNumPoints(Math.max(8, numPoints));
        shapeFactory.setCentre(new Coordinate(x, y));
        shapeFactory.setSize(radius * 2);
        return shapeFactory.createCircle();
    }

    private static class Shape {
        final Geometry geometry;
        final boolean computed;
        final Paint paint;

        private Shape(Geometry geometry, boolean computed, Paint paint) {
            this.geometry = geometry;
            this.computed = computed;
            this.paint = paint;
        }
    }
}
