package fr.uspn.cellula.explore;

import fr.uspn.cellula.grid.Frame;
import java.util.Arrays;
import java.util.zip.Deflater;

/**
 * The successive generations of a one-dimensional, two-state automaton, stacked on top of each
 * other, together with the measurements used to tell boring pictures from interesting ones.
 *
 * <p>Diagrams are immutable.
 */
public final class SpaceTimeDiagram {

    private final byte[][] rows;

    private SpaceTimeDiagram(byte[][] rows) {
        this.rows = rows;
    }

    /**
     * Runs a rule from a given first row.
     *
     * @param rule the rule to apply
     * @param initial the row at generation 0, one 0/1 value per cell
     * @param steps number of generations to compute after the initial one
     * @return a diagram of {@code steps + 1} rows
     */
    public static SpaceTimeDiagram of(TableRule rule, byte[] initial, int steps) {
        if (steps < 0) {
            throw new IllegalArgumentException("steps must not be negative, got " + steps);
        }
        byte[][] rows = new byte[steps + 1][];
        rows[0] = initial.clone();
        for (int generation = 1; generation <= steps; generation++) {
            rows[generation] = rule.step(rows[generation - 1]);
        }
        return new SpaceTimeDiagram(rows);
    }

    /**
     * Runs a rule from a single cell switched on, on a row just wide enough for the pattern never
     * to reach the edges: {@code 2 * radius * steps + 1} cells.
     *
     * @param rule the rule to apply
     * @param steps number of generations to compute after the initial one
     */
    public static SpaceTimeDiagram fromSingleCell(TableRule rule, int steps) {
        return of(rule, singleCell(widthFor(rule.radius(), steps)), steps);
    }

    /** Returns the width at which a pattern grown from one cell cannot reach the edges. */
    public static int widthFor(int radius, int steps) {
        return 2 * radius * steps + 1;
    }

    /** Returns a row of the given width whose middle cell alone is on. */
    public static byte[] singleCell(int width) {
        byte[] row = new byte[width];
        row[width / 2] = 1;
        return row;
    }

    /** Returns the number of rows, i.e. of generations including the initial one. */
    public int generations() {
        return rows.length;
    }

    /** Returns the number of cells per row. */
    public int width() {
        return rows[0].length;
    }

    /** Returns the state of a cell at a generation. */
    public int cell(int generation, int position) {
        return rows[generation][position];
    }

    /** Returns the number of cells that are on at a generation. */
    public int population(int generation) {
        int population = 0;
        for (byte cell : rows[generation]) {
            population += cell;
        }
        return population;
    }

    /**
     * Estimates the box-counting dimension of the set of cells that are on.
     *
     * <p>The picture is covered with boxes of side {@code s} and the boxes containing at least one
     * live cell are counted: {@code N(s)}. For a fractal, {@code N(s)} grows like
     * {@code s^-D} as the boxes shrink; {@code D} is estimated as the slope of the least-squares
     * line through the points {@code (log 1/s, log N(s))} for {@code s = 2, 4, 8, ...}. A line has
     * dimension 1, a filled area dimension 2, the Sierpiński triangle
     * {@code log 3 / log 2 ≈ 1.585}.
     *
     * @return the estimated dimension, or 0 if no cell is on
     */
    public double boxDimension() {
        int limit = Math.min(generations(), width()) / 8;
        double sumX = 0;
        double sumY = 0;
        double sumXX = 0;
        double sumXY = 0;
        int points = 0;
        for (int side = 2; side <= limit; side *= 2) {
            int boxes = occupiedBoxes(side);
            if (boxes == 0) {
                return 0;
            }
            double x = -Math.log(side);
            double y = Math.log(boxes);
            sumX += x;
            sumY += y;
            sumXX += x * x;
            sumXY += x * y;
            points++;
        }
        if (points < 2) {
            return 0;
        }
        return (points * sumXY - sumX * sumY) / (points * sumXX - sumX * sumX);
    }

    private int occupiedBoxes(int side) {
        int boxRows = (generations() + side - 1) / side;
        int boxColumns = (width() + side - 1) / side;
        boolean[] occupied = new boolean[boxRows * boxColumns];
        int count = 0;
        for (int generation = 0; generation < rows.length; generation++) {
            byte[] row = rows[generation];
            int base = generation / side * boxColumns;
            for (int position = 0; position < row.length; position++) {
                if (row[position] != 0 && !occupied[base + position / side]) {
                    occupied[base + position / side] = true;
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Measures how well the picture compresses, as a proxy for its disorder.
     *
     * <p>The cells are packed eight to a byte and deflated. The result is the ratio of the
     * compressed size to the packed size: close to 0 for uniform or periodic pictures, small for
     * nested (self-similar) ones, and large for pictures that look random.
     */
    public double compressionRatio() {
        int bytesPerRow = (width() + 7) / 8;
        byte[] packed = new byte[rows.length * bytesPerRow];
        for (int generation = 0; generation < rows.length; generation++) {
            byte[] row = rows[generation];
            int base = generation * bytesPerRow;
            for (int position = 0; position < row.length; position++) {
                if (row[position] != 0) {
                    packed[base + position / 8] |= (byte) (1 << (position % 8));
                }
            }
        }
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        try {
            deflater.setInput(packed);
            deflater.finish();
            byte[] buffer = new byte[4096];
            long compressed = 0;
            while (!deflater.finished()) {
                compressed += deflater.deflate(buffer);
            }
            return (double) compressed / packed.length;
        } finally {
            deflater.end();
        }
    }

    /** Returns the diagram as a displayable frame, one row per generation. */
    public Frame toFrame() {
        int columns = width();
        int[] states = new int[rows.length * columns];
        for (int generation = 0; generation < rows.length; generation++) {
            for (int position = 0; position < columns; position++) {
                states[generation * columns + position] = rows[generation][position];
            }
        }
        return new Frame(rows.length, columns, states);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SpaceTimeDiagram that && Arrays.deepEquals(rows, that.rows);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(rows);
    }
}
