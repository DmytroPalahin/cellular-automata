package fr.uspn.cellula.grid;

import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * The extent of a grid along each of its axes.
 *
 * <p>
 * A shape {@code k1 × k2 × ... × kd} describes a {@code d}-dimensional box
 * holding
 * {@code k1 * k2 * ... * kd} cells. Shapes are immutable.
 */
public final class Shape {

    /**
     * Upper bound on the number of cells, to fail fast instead of exhausting the
     * heap.
     */
    public static final int MAX_CELLS = 64_000_000;

    private final int[] sizes;
    private final int cellCount;

    private Shape(int[] sizes, int cellCount) {
        this.sizes = sizes;
        this.cellCount = cellCount;
    }

    /**
     * Creates a shape.
     *
     * @param sizes extent along each axis; every extent must be positive
     * @return the shape
     * @throws IllegalArgumentException if no size is given, a size is not positive,
     *                                  or the total
     *                                  number of cells exceeds {@link #MAX_CELLS}
     */
    public static Shape of(int... sizes) {
        Objects.requireNonNull(sizes, "sizes");
        if (sizes.length == 0) {
            throw new IllegalArgumentException("a shape needs at least one axis");
        }
        long count = 1;
        for (int axis = 0; axis < sizes.length; axis++) {
            if (sizes[axis] < 1) {
                throw new IllegalArgumentException(
                        "size along axis " + axis + " must be positive, got " + sizes[axis]);
            }
            count *= sizes[axis];
            if (count > MAX_CELLS) {
                throw new IllegalArgumentException(
                        "a grid of " + describe(sizes) + " cells is too large (limit: "
                                + MAX_CELLS + " cells)");
            }
        }
        return new Shape(sizes.clone(), (int) count);
    }

    /** Returns the number of axes. */
    public int dimension() {
        return sizes.length;
    }

    /**
     * Returns the extent along one axis.
     *
     * @param axis zero-based axis index
     */
    public int size(int axis) {
        return sizes[axis];
    }

    /** Returns the total number of cells. */
    public int cellCount() {
        return cellCount;
    }

    /**
     * Tells whether the given coordinates designate a cell of the box.
     *
     * @param coordinates one value per axis
     * @return {@code true} if every coordinate lies in {@code [0, size(axis))}
     * @throws IllegalArgumentException if the number of coordinates is not
     *                                  {@link #dimension()}
     */
    public boolean contains(int... coordinates) {
        requireArity(coordinates.length);
        for (int axis = 0; axis < sizes.length; axis++) {
            if (coordinates[axis] < 0 || coordinates[axis] >= sizes[axis]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Maps arbitrary coordinates onto the box as if it were a torus.
     *
     * @param coordinates one value per axis, possibly out of range
     * @return a new array where each coordinate was reduced modulo the extent of
     *         its axis
     */
    public int[] wrap(int... coordinates) {
        requireArity(coordinates.length);
        int[] wrapped = new int[sizes.length];
        for (int axis = 0; axis < sizes.length; axis++) {
            wrapped[axis] = Math.floorMod(coordinates[axis], sizes[axis]);
        }
        return wrapped;
    }

    /** Returns a fresh array holding the extent of every axis. */
    public int[] toArray() {
        return sizes.clone();
    }

    void requireArity(int arity) {
        if (arity != sizes.length) {
            throw new IllegalArgumentException(
                    "expected " + sizes.length + " coordinate(s) but got " + arity);
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Shape that && Arrays.equals(sizes, that.sizes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(sizes);
    }

    /** Returns a human-readable form such as {@code 96 × 64}. */
    @Override
    public String toString() {
        return describe(sizes);
    }

    private static String describe(int[] sizes) {
        StringJoiner joiner = new StringJoiner(" × ");
        for (int size : sizes) {
            joiner.add(Integer.toString(size));
        }
        return joiner.toString();
    }
}
