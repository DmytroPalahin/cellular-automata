package fr.uspn.cellula.grid;

import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * An immutable integer vector of arbitrary dimension.
 *
 * <p>
 * The same value type is used for two closely related notions: the
 * <em>position</em> of a cell
 * in a grid, and the <em>offset</em> of a neighbor relative to the cell under
 * examination (the
 * subject expresses neighborhoods as "the coordinates of the neighbors of a
 * cell located at the
 * origin").
 *
 * <p>
 * Instances are compared component-wise and ordered lexicographically, which
 * makes them usable
 * as map keys and gives collections of offsets a deterministic order.
 */
public final class Coordinates implements Comparable<Coordinates> {

    private final int[] components;

    private Coordinates(int[] components) {
        this.components = components;
    }

    /**
     * Creates coordinates from their components.
     *
     * @param components one value per axis; at least one is required
     * @return the corresponding coordinates
     * @throws IllegalArgumentException if no component is given
     */
    public static Coordinates of(int... components) {
        Objects.requireNonNull(components, "components");
        if (components.length == 0) {
            throw new IllegalArgumentException("coordinates need at least one component");
        }
        return new Coordinates(components.clone());
    }

    /**
     * Returns the origin of a space of the given dimension.
     *
     * @param dimension number of axes, at least one
     * @return the all-zero coordinates
     */
    public static Coordinates origin(int dimension) {
        if (dimension < 1) {
            throw new IllegalArgumentException("dimension must be positive, got " + dimension);
        }
        return new Coordinates(new int[dimension]);
    }

    /** Returns the number of axes. */
    public int dimension() {
        return components.length;
    }

    /**
     * Returns the component along one axis.
     *
     * @param axis zero-based axis index
     */
    public int get(int axis) {
        return components[axis];
    }

    /**
     * Returns the component-wise sum of these coordinates and {@code other}.
     *
     * @throws IllegalArgumentException if the dimensions differ
     */
    public Coordinates plus(Coordinates other) {
        if (other.dimension() != dimension()) {
            throw new IllegalArgumentException(
                    "cannot add " + other + " to " + this + ": dimensions differ");
        }
        int[] sum = new int[components.length];
        for (int axis = 0; axis < sum.length; axis++) {
            sum[axis] = components[axis] + other.components[axis];
        }
        return new Coordinates(sum);
    }

    /** Returns {@code true} when every component is zero. */
    public boolean isOrigin() {
        for (int component : components) {
            if (component != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the Chebyshev norm, i.e. the largest absolute component.
     *
     * <p>
     * For an offset this is the number of "rings" separating the neighbor from the
     * center.
     */
    public int chebyshevNorm() {
        int norm = 0;
        for (int component : components) {
            norm = Math.max(norm, Math.abs(component));
        }
        return norm;
    }

    /** Returns a fresh array holding the components. */
    public int[] toArray() {
        return components.clone();
    }

    @Override
    public int compareTo(Coordinates other) {
        return Arrays.compare(components, other.components);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Coordinates that && Arrays.equals(components, that.components);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(components);
    }

    /**
     * Returns the tuple notation used in definition files, for example
     * {@code (1, -2, 0)}.
     */
    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", "(", ")");
        for (int component : components) {
            joiner.add(Integer.toString(component));
        }
        return joiner.toString();
    }
}
