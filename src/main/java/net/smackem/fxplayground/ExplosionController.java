package net.smackem.fxplayground;

import java.util.*;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class ExplosionController {

    private final Timeline ticker;
    private final Collection<Dot> dots;
    private static final double WIDTH = 600;
    private static final double HEIGHT = 600;
    private static final List<Color[]> PRECOMPUTED_COLORS;

    @FXML
    private Canvas canvas;

    static {
        PRECOMPUTED_COLORS = new ArrayList<>(360);
        for (int hue = 0; hue < 360; hue++) {
            final Color hsb = Color.hsb(hue, 1, 1);
            final Color[] colors = new Color[101];
            for (int opacity = 0; opacity <= 100; opacity++) {
                colors[opacity] = Color.color(hsb.getRed(), hsb.getGreen(), hsb.getBlue(), opacity / 100.0);
            }
            PRECOMPUTED_COLORS.add(colors);
        }
    }

    public ExplosionController() {
        this.dots = createDots();
        this.ticker = new Timeline(new KeyFrame(Duration.millis(40), this::tick));
        this.ticker.setCycleCount(Animation.INDEFINITE);
    }

    @FXML
    private void initialize() {
        this.canvas.setWidth(WIDTH);
        this.canvas.setHeight(HEIGHT);
        this.ticker.play();
    }

    private static Collection<Dot> createDots() {
        int dotCount = 3_000;
        final Random random = new Random();
        final double maxRadius = WIDTH * 3 / 4;
        final Point2D center = new Point2D(WIDTH / 2, HEIGHT / 2);
        final Collection<Dot> dots = new ArrayList<>(dotCount);

        for ( ; dotCount > 0; dotCount--) {
            final double angle = random.nextDouble() * Math.PI * 2;
            final double radius = random.nextDouble() * maxRadius;
            final Point2D pos = new Point2D(
                    center.getX() + 12 - random.nextDouble() * 24,
                    center.getY() + 12 - random.nextDouble() * 24);
            final Point2D dest = new Point2D(
                    center.getX() + Math.cos(angle) * radius,
                    center.getY() + Math.sin(angle) * radius);
            final double speed = 0.6 + random.nextDouble() * 8.4;
            dots.add(new Dot(pos, dest, speed, random.nextInt(360)));
        }

        return dots;
    }

    private void tick(ActionEvent ae) {
        final GraphicsContext gc = this.canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, WIDTH, HEIGHT);
        boolean finished = true;

        for (final Dot dot : this.dots) {
            if (dot.finished) {
                continue;
            }
            final Color color = PRECOMPUTED_COLORS.get(dot.hue)[(int)(dot.distance * 100 / dot.initialDistance)];
            gc.setFill(color);
            gc.fillOval(dot.pos.getX(), dot.pos.getY(), 3, 3);
            finished &= dot.move();
        }

        if (finished) {
            this.ticker.stop();
        }
    }

    private static class Dot {
        final Point2D dest;
        final Point2D inc;
        final int hue;
        final double initialDistance;
        final double speed;
        double distance;
        Point2D pos;
        boolean finished;

        Dot(Point2D pos, Point2D dest, double speed, int hue) {
            final Point2D difference = dest.subtract(pos);
            this.dest = dest;
            this.inc = difference.normalize().multiply(speed);
            this.pos = pos;
            this.initialDistance = difference.magnitude();
            this.speed = speed;
            this.hue = hue;
            this.distance = this.initialDistance;
        }

        boolean move() {
            if (this.finished) {
                return true;
            }
            this.pos = this.pos.add(this.inc);
            this.distance = dest.subtract(pos).magnitude();
            this.finished = this.distance < this.speed;
            return this.finished;
        }
    }
}