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
        this.shapes.addAll(createRandomShapes());
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
        if (shape.geometry.getArea() > 0) {
            gc.closePath();
            gc.fill();
        }
        gc.stroke();
        gc.restore();
    }

    private static Collection<Shape> createShapes() {
        final GeometryFactory gf = new GeometryFactory();
        final Geometry circle1 = createCircle(gf, 100, 80, 18);
        final Geometry circle2 = createCircle(gf, 300, 260, 150);
        final Geometry circle3 = createCircle(gf, 500, 10, 5);
        final Geometry circle4 = createCircle(gf, 350, 240, 30);
        final Geometry circle5 = createCircle(gf, 20, 400, 100);
        final Geometry circle6 = createCircle(gf, 400, 300, 80);
        final Geometry line1 = createLine(gf, 0, 0, 600, 400);
        final Geometry line2 = createLine(gf, 500, 0, 0, 400);
        final Geometry intersection1 = circle1.intersection(line1);
        final Geometry intersection2 = line2.intersection(line1);
        final Geometry intersection3 = circle2.intersection(line2);
        final Geometry intersection4 = circle2.intersection(circle6);
        intersection2.getCoordinate().x = 600;
        intersection2.getCoordinate().y = 440;
        intersection2.geometryChanged();
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
                new Shape(intersection2.buffer(5), true, paintComputed),
                new Shape(intersection4, true, paintComputed)
        );
    }

    private static Collection<Shape> createRandomShapes() {
        final GeometryFactory gf = new GeometryFactory();
        final List<Shape> shapes = new ArrayList<>();
        final var random = ThreadLocalRandom.current();
        for (int i = 0; i < 20; i++) {
            final int radius = random.nextInt(5, 100);
            final int x = random.nextInt(640);
            final int y = random.nextInt(480);
            final Paint paint = Color.rgb(
                    random.nextInt(256),
                    random.nextInt(256),
                    random.nextInt(256),
                    0.6);
            final var circle = new Shape(createCircle(gf, x, y, radius), false, paint);
            shapes.add(circle);
        }
        for (int i = 0; i < 10; i++) {
            final int x1 = random.nextInt(640);
            final int y1 = random.nextInt(480);
            final int x2 = random.nextInt(640);
            final int y2 = random.nextInt(480);
            final var line = new Shape(createLine(gf, x1, y1, x2, y2), false, Color.BLACK);
            shapes.add(line);
            intersectShape(line, shapes, gf);
        }
        return shapes;
    }

    private static void intersectShape(Shape shape, List<Shape> shapes, GeometryFactory gf) {
        final int count = shapes.size();
        for (int i = 0; i < count - 1; i++) {
            final var shape2 = shapes.get(i);
            final var geometry = shape.geometry.intersection(shape2.geometry);
            if (geometry instanceof LineString) {
                final var line = (LineString)geometry;
                for (final var coordinate : line.getCoordinates()) {
                    shapes.add(new Shape(
                            gf.createPoint(coordinate).buffer(5), true, Color.BLACK));
                }
            } else if (geometry instanceof Point) {
                final var point = (Point)geometry;
                shapes.add(new Shape(point.buffer(5), true, Color.BLACK));
            } else {
                shapes.add(new Shape(geometry.buffer(5), true, Color.RED));
                System.out.println(geometry.toString());
            }
        }
    }

    private static LineString createLine(GeometryFactory gf, double x1, double y1, double x2, double y2) {
        return gf.createLineString(new Coordinate[]{
                new Coordinate(x1, y1),
                new Coordinate(x2, y2),
        });
    }

    private static Geometry createCircle(GeometryFactory gf, double x, double y, double radius) {
        // maximum 16 points per quadrant, climbing fast: 5 for radius 10
        final int numPoints = (int)(Math.atan(radius / 20.0) * 16.0 / Math.atan(1000));
        return gf.createPoint(new Coordinate(x, y)).buffer(radius, numPoints);
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
