package net.smackem.fxplayground.osc;

import javafx.geometry.Point2D;

import java.util.ArrayList;
import java.util.List;

public class Figure {
    private final List<Point2D> points = new ArrayList<>();

    public List<Point2D> points() {
        return this.points;
    }
}
