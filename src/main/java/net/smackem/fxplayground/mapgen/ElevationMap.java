package net.smackem.fxplayground.mapgen;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

import java.util.concurrent.ThreadLocalRandom;

public final class ElevationMap {
    public static final int MAX_VALUE = 255;
    private final Bitmap bitmap;
    private Geometry hintGeometry;
    private Bitmap.MinMax expansionRange;

    public ElevationMap(int width, int height) {
        this.bitmap = Bitmap.random(width, height, MAX_VALUE + 1);
    }

    public Bitmap bitmap() {
        return this.bitmap;
    }

    public Geometry hintGeometry() {
        return this.hintGeometry;
    }

    public void seedIsland() {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final int height = this.bitmap.height();
        final int width = this.bitmap.width();
        final double centerX = width / 2.0;
        final double centerY = height / 2.0;
        //noinspection UnnecessaryLocalVariable
        final double maxDistance = centerY;//Math.sqrt(centerX * centerX + centerY * centerY);
        final int halfMax = MAX_VALUE / 2;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final double dx = (double)x - centerX;
                final double dy = (double)y - centerY;
                final double distance = Math.sqrt(dx * dx + dy * dy);
                final double ratio = 1.0 - (distance / maxDistance);
                final int maxRandom = halfMax + (int)(ratio * halfMax);
                this.bitmap.set(x, y, random.nextInt(maxRandom));
            }
        }
        this.hintGeometry = null;
        this.expansionRange = null;
    }

    public void seedMountains() {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final int height = this.bitmap.height();
        final int width = this.bitmap.width();
        final GeometryFactory gf = new GeometryFactory();
        final Coordinate[] coordinates = new Coordinate[5];
        for (int i = 0; i < coordinates.length; i++) {
            coordinates[i] = new Coordinate(
                    random.nextInt(width),
                    random.nextInt(height));
        }
        final Geometry mountainPeaks = gf.createLineString(coordinates);
        final double maxDistance = Math.sqrt(width * width + height * height);
        final int halfMax = MAX_VALUE / 2;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final Geometry point = gf.createPoint(new Coordinate(x, y));
                final double distance = point.distance(mountainPeaks);
                final double ratio = 1.0 - (distance / maxDistance);
                final int value = (int)(ratio * halfMax);
                this.bitmap.set(x, y, value + random.nextInt(halfMax));
            }
        }
        this.hintGeometry = mountainPeaks;
        this.expansionRange = null;
    }

    public void seedHighPlains() {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final int height = this.bitmap.height();
        final int width = this.bitmap.width();
        final GeometryFactory gf = new GeometryFactory();
        final Geometry[] circles = new Geometry[8];
        for (int i = 0; i < circles.length; i++) {
            circles[i] = gf.createPoint(new Coordinate(
                    random.nextInt(width),
                    random.nextInt(height)))
                    .buffer(20 + random.nextInt(this.bitmap.height() / 4));
        }
        final Geometry plains = gf.createGeometryCollection(circles);
        final int halfMax = MAX_VALUE / 2;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final Geometry point = gf.createPoint(new Coordinate(x, y));
                int value = random.nextInt(halfMax);
                if (plains.intersects(point)) {
                    value += halfMax;
                }
                this.bitmap.set(x, y, value);
            }
        }
        this.hintGeometry = plains;
        this.expansionRange = new Bitmap.MinMax(140, 220);
    }

    public void seedLakes() {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final int height = this.bitmap.height();
        final int width = this.bitmap.width();
        final GeometryFactory gf = new GeometryFactory();
        final Geometry[] circles = new Geometry[5];
        for (int i = 0; i < circles.length; i++) {
            final int centerX = random.nextInt(width / 10, width - width / 10);
            final int centerY = random.nextInt(height / 10, height - height / 10);
            final int maxRadius = random.nextInt(20, height / 5);
            final Coordinate[] coordinates = new Coordinate[5];
            for (int ipt = 0; ipt < coordinates.length; ipt++) {
                final double angle = random.nextDouble(Math.PI * 2);
                final double radius = random.nextDouble(maxRadius);
                coordinates[ipt] = new Coordinate(
                        centerX + Math.cos(angle) * radius,
                        centerY + Math.sin(angle) * radius);
            }
            circles[i] = gf.createLineString(coordinates);
        }
        final Geometry lakes = gf.createGeometryCollection(circles);
        final int halfMax = MAX_VALUE / 2;
        final double maxDistance = Math.sqrt(width * width + height * height);
        final double atan1000 = Math.atan(1000);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final Geometry point = gf.createPoint(new Coordinate(x, y));
                final double distance = point.distance(lakes);
                final double ratio = Math.atan(10 * distance / maxDistance) / atan1000;
                final int value = (int) (ratio * halfMax);
                this.bitmap.set(x, y, value + random.nextInt(halfMax));
            }
        }
        this.hintGeometry = lakes;
        this.expansionRange = null;
    }

    public void generate() {
        for (int j = 0; j < 3; j++) {
            for (int i = 0; i < 10; i++) {
                smoothen();
            }
            expand();
        }
    }

    public void smoothen() {
        final int height = this.bitmap.height();
        final int width = this.bitmap.width();
        final Bitmap source = Bitmap.copyOf(this.bitmap);
        final Bitmap kernel = Bitmap.kernel(
                0, 1, 2, 1, 0,
                1, 2, 4, 2, 1,
                2, 4, 8, 4, 2,
                1, 2, 4, 2, 1,
                0, 1, 2, 1, 0);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int v = source.convolute(x, y, kernel);
                this.bitmap.set(x, y, clamp(v));
            }
        }
    }

    public void sharpen() {
        final int height = this.bitmap.height();
        final int width = this.bitmap.width();
        final Bitmap source = Bitmap.copyOf(this.bitmap);
        final Bitmap kernel = Bitmap.kernel(
                -1, -1, -1,
                -1,  8, -1,
                -1, -1, -1);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int v = source.get(x, y) + source.convolute(x, y, kernel);
                this.bitmap.set(x, y, clamp(v));
            }
        }
    }

    public void expand() {
        final int height = this.bitmap.height();
        final int width = this.bitmap.width();
        final Bitmap source = Bitmap.copyOf(this.bitmap);
        final Bitmap.MinMax minMax = source.minMax();
        final double sourceRange = minMax.max() - minMax.min();
        final int min = this.expansionRange != null ? this.expansionRange.min() : 0;
        final int max = this.expansionRange != null ? this.expansionRange.max() : MAX_VALUE;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final double value = source.get(x, y);
                final double ratio = (value - minMax.min()) / sourceRange;
                this.bitmap.set(x, y, min + (int) (ratio * (max - min)));
            }
        }
    }

    private static int clamp(int value) {
        if (value > MAX_VALUE) {
            return MAX_VALUE;
        }
        return Math.max(value, 0);
    }
}
