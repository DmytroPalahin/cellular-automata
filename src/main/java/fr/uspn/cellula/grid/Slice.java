package fr.uspn.cellula.grid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

/**
 * The part of a grid that is shown on screen.
 *
 * <p>
 * A screen is flat, a grid may not be. A slice selects a plane by pinning every
 * axis but two,
 * using the Matlab-like notation of the subject: {@code (37, :, :)} is the
 * plane {@code x = 37} of
 * a three-dimensional grid, where {@code :} marks a <em>free</em> axis. The
 * first free axis maps to
 * the rows of the picture and the second to its columns.
 *
 * <p>
 * The notion degrades gracefully in low dimension: a two-dimensional grid has
 * the single slice
 * {@code (:, :)}, and a one-dimensional grid the single slice {@code (:)}, a
 * lone row.
 *
 * <p>
 * Slices are immutable.
 */
public final class Slice {

    /** Marks a free axis in the textual notation. */
    public static final String FREE = ":";

    private static final int NONE = -1;

    private final int[] pinned;
    private final boolean[] free;
    private final int rowAxis;
    private final int columnAxis;

    private Slice(int[] pinned, boolean[] free) {
        this.pinned = pinned;
        this.free = free;
        int dimension = free.length;
        List<Integer> freeAxes = new ArrayList<>();
        for (int axis = 0; axis < dimension; axis++) {
            if (free[axis]) {
                freeAxes.add(axis);
            } else if (pinned[axis] < 0) {
                throw new IllegalArgumentException(
                        "slice coordinate on axis " + axis + " must not be negative");
            }
        }
        int expected = Math.min(dimension, 2);
        if (freeAxes.size() != expected) {
            throw new IllegalArgumentException(
                    "a slice of a " + dimension + "-dimensional grid needs exactly " + expected
                            + " free axis(es) marked ':', found " + freeAxes.size());
        }
        this.rowAxis = dimension == 1 ? NONE : freeAxes.get(0);
        this.columnAxis = freeAxes.get(freeAxes.size() - 1);
    }

    /**
     * Parses the textual notation.
     *
     * @param text for example {@code "(37, :, :)"}; the parentheses are optional
     * @return the slice
     * @throws IllegalArgumentException if the text is malformed or does not leave
     *                                  the right number
     *                                  of free axes
     */
    public static Slice parse(String text) {
        String body = text.strip();
        if (body.startsWith("(") && body.endsWith(")")) {
            body = body.substring(1, body.length() - 1);
        }
        String[] parts = body.split(",", -1);
        int[] pinned = new int[parts.length];
        boolean[] free = new boolean[parts.length];
        for (int axis = 0; axis < parts.length; axis++) {
            String part = parts[axis].strip();
            if (part.equals(FREE)) {
                free[axis] = true;
            } else {
                try {
                    pinned[axis] = Integer.parseInt(part);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "invalid slice '" + text.strip() + "': '" + part
                                    + "' is neither an integer nor ':'");
                }
            }
        }
        return new Slice(pinned, free);
    }

    /**
     * Returns the default slice of a grid: the first two axes are free and every
     * other axis is
     * pinned at its middle.
     */
    public static Slice centered(Shape shape) {
        int dimension = shape.dimension();
        int[] pinned = new int[dimension];
        boolean[] free = new boolean[dimension];
        for (int axis = 0; axis < dimension; axis++) {
            if (axis < 2) {
                free[axis] = true;
            } else {
                pinned[axis] = shape.size(axis) / 2;
            }
        }
        return new Slice(pinned, free);
    }

    /** Returns the dimension of the grids this slice applies to. */
    public int dimension() {
        return free.length;
    }

    /**
     * Tells whether the given axis is free (spans the picture) rather than pinned.
     */
    public boolean isFree(int axis) {
        return free[axis];
    }

    /** Returns the axes that are pinned to a coordinate, in increasing order. */
    public List<Integer> pinnedAxes() {
        List<Integer> axes = new ArrayList<>();
        for (int axis = 0; axis < free.length; axis++) {
            if (!free[axis]) {
                axes.add(axis);
            }
        }
        return axes;
    }

    /**
     * Returns the coordinate a pinned axis is fixed at.
     *
     * @throws IllegalArgumentException if the axis is free
     */
    public int pinnedAt(int axis) {
        if (free[axis]) {
            throw new IllegalArgumentException("axis " + axis + " is free");
        }
        return pinned[axis];
    }

    /**
     * Returns a slice parallel to this one, with one pinned axis moved to another
     * coordinate.
     *
     * @throws IllegalArgumentException if the axis is free or the coordinate
     *                                  negative
     */
    public Slice moved(int axis, int coordinate) {
        if (free[axis]) {
            throw new IllegalArgumentException("axis " + axis + " is free and cannot be pinned");
        }
        int[] moved = pinned.clone();
        moved[axis] = coordinate;
        return new Slice(moved, free);
    }

    /**
     * Checks that this slice designates a plane of a grid of the given shape.
     *
     * @throws IllegalArgumentException if the dimensions differ or a pinned
     *                                  coordinate is out of
     *                                  range
     */
    public void requireWithin(Shape shape) {
        if (shape.dimension() != dimension()) {
            throw new IllegalArgumentException(
                    "slice " + this + " has " + dimension() + " coordinate(s) but the grid has "
                            + shape.dimension() + " dimension(s)");
        }
        for (int axis : pinnedAxes()) {
            if (pinned[axis] >= shape.size(axis)) {
                throw new IllegalArgumentException(
                        "slice " + this + " is outside the grid: axis " + axis
                                + " only has coordinates 0.." + (shape.size(axis) - 1));
            }
        }
    }

    /** Returns the number of rows of the pictures taken through this slice. */
    public int rows(Shape shape) {
        return rowAxis == NONE ? 1 : shape.size(rowAxis);
    }

    /** Returns the number of columns of the pictures taken through this slice. */
    public int columns(Shape shape) {
        return shape.size(columnAxis);
    }

    /**
     * Returns the grid position shown at a given row and column of the picture.
     *
     * @param row    row in the picture (ignored for one-dimensional grids)
     * @param column column in the picture
     */
    public Coordinates locate(int row, int column) {
        int[] coordinates = pinned.clone();
        if (rowAxis != NONE) {
            coordinates[rowAxis] = row;
        }
        coordinates[columnAxis] = column;
        return Coordinates.of(coordinates);
    }

    /**
     * Takes a picture of the grid through this slice.
     *
     * @param grid the grid to read
     * @return the states of the selected plane
     * @throws IllegalArgumentException if the slice does not fit the grid
     */
    public Frame extract(Grid grid) {
        Shape shape = grid.shape();
        requireWithin(shape);
        int rows = rows(shape);
        int columns = columns(shape);
        int[] states = new int[rows * columns];
        int[] coordinates = pinned.clone();
        for (int row = 0; row < rows; row++) {
            if (rowAxis != NONE) {
                coordinates[rowAxis] = row;
            }
            for (int column = 0; column < columns; column++) {
                coordinates[columnAxis] = column;
                states[row * columns + column] = grid.cell(coordinates).state();
            }
        }
        return new Frame(rows, columns, states);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Slice that
                && Arrays.equals(free, that.free)
                && Arrays.equals(pinned, that.pinned);
    }

    @Override
    public int hashCode() {
        return 31 * Arrays.hashCode(free) + Arrays.hashCode(pinned);
    }

    /** Returns the textual notation, for example {@code (37, :, :)}. */
    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", "(", ")");
        for (int axis = 0; axis < free.length; axis++) {
            joiner.add(free[axis] ? FREE : Integer.toString(pinned[axis]));
        }
        return joiner.toString();
    }
}
