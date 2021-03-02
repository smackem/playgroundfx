package net.smackem.fxplayground.mapgen;

import java.util.concurrent.ThreadLocalRandom;

public class Bitmap {

    private final int width;
    private final int height;
    private final int[] data;

    private Bitmap(int width, int height, int[] data) {
        this.width = width;
        this.height = height;
        this.data = data;
    }

    public Bitmap(int width, int height) {
        this(width, height, new int[width * height]);
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public int get(int x, int y) {
        return this.data[y * this.width + x];
    }

    public void set(int x, int y, int value) {
        this.data[y * this.width + x] = value;
    }

    public static record MinMax(int min, int max) {}

    public MinMax minMax() {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                final int value = get(x, y);
                if (value < min) {
                    min = value;
                }
                if (value > max) {
                    max = value;
                }
            }
        }
        return new MinMax(min, max);
    }

    public static Bitmap kernel(int... values) {
        final int length = sqrt(values.length);
        return new Bitmap(length, length, values);
    }

    private static int sqrt(int n) {
        double sqrt = Math.sqrt(n);
        int result = (int) Math.floor(sqrt);
        if (sqrt - result != 0) {
            throw new IllegalArgumentException("passed array must be quadratic");
        }
        return result;
    }

    public static Bitmap copyOf(Bitmap other) {
        final Bitmap bitmap = new Bitmap(other.width, other.height);
        System.arraycopy(other.data, 0, bitmap.data, 0, bitmap.data.length);
        return bitmap;
    }

    public static Bitmap random(int width, int height, int upperBound) {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final Bitmap bitmap = new Bitmap(width, height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                bitmap.set(x, y, random.nextInt(0, upperBound));
            }
        }
        return bitmap;
    }

    public static Bitmap randomDispersed(int width, int height, int value, int count) {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final Bitmap bitmap = new Bitmap(width, height);
        while (count-- > 0) {
            final int x = random.nextInt(0, width);
            final int y = random.nextInt(0, height);
            bitmap.set(x, y, value);
        }
        return bitmap;
    }

    public int convolve(int x, int y, Bitmap kernel) {
        if (kernel.width > this.width) {
            throw new IllegalArgumentException("kernel width must be less than image width");
        }
        if (kernel.height > this.height) {
            throw new IllegalArgumentException("kernel height must be less thatn image height");
        }
        int kernelSum = 0;
        int v = 0;
        int kernelIndex = 0;

        for (int kernelY = 0; kernelY < kernel.height; kernelY++) {
            for (int kernelX = 0; kernelX < kernel.width; kernelX++) {
                final int sourceY = y - (kernel.height / 2) + kernelY;
                final int sourceX = x - (kernel.width / 2) + kernelX;
                if (sourceX >= 0 && sourceX < this.width && sourceY >= 0 && sourceY < this.height) {
                    int value = kernel.data[kernelIndex];
                    int source = get(sourceX, sourceY);
                    v += value * source;
                    kernelSum += value;
                }
                kernelIndex++;
            }
        }
        if (kernelSum == 0) {
            return v;
        }
        return (int) Math.round((double)v / (double)kernelSum);
    }
}
