package fr.uspn.cellula.neighborhood;

import fr.uspn.cellula.grid.Cell;
import fr.uspn.cellula.grid.Coordinates;
import fr.uspn.cellula.grid.Grid;
import fr.uspn.cellula.grid.GridCell;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A neighborhood described by a fixed set of offsets relative to the cell under examination.
 *
 * <p>This is the form used by the subject: {@code G4* = {(-1, 0), (1, 0), (0, -1), (0, 1)}} lists
 * the coordinates of the neighbors of a cell sitting at the origin. Every predefined neighborhood
 * and every neighborhood declared in a definition file is an instance of this class.
 *
 * <p>Instances are immutable and can be shared freely between threads.
 */
public final class OffsetNeighborhood implements Neighborhood {

    private final String name;
    private final int dimension;
    private final List<Coordinates> offsets;
    /** The offsets again, unboxed, for the benefit of the traversal's inner loop. */
    private final int[][] deltas;

    /**
     * Creates a neighborhood.
     *
     * @param name the name used to refer to the neighborhood in rules
     * @param offsets the relative positions of the visited cells; duplicates are ignored, as the
     *     definition is a set
     * @throws IllegalArgumentException if no offset is given or if the offsets do not all have the
     *     same dimension
     */
    public OffsetNeighborhood(String name, Collection<Coordinates> offsets) {
        this.name = Objects.requireNonNull(name, "name");
        Objects.requireNonNull(offsets, "offsets");
        if (offsets.isEmpty()) {
            throw new IllegalArgumentException("neighborhood " + name + " has no cell");
        }
        this.offsets = List.copyOf(new LinkedHashSet<>(offsets));
        this.dimension = this.offsets.get(0).dimension();
        this.deltas = new int[this.offsets.size()][];
        for (int index = 0; index < deltas.length; index++) {
            Coordinates offset = this.offsets.get(index);
            if (offset.dimension() != dimension) {
                throw new IllegalArgumentException(
                        "neighborhood " + name + " mixes offsets of different dimensions: "
                                + this.offsets.get(0) + " and " + offset);
            }
            deltas[index] = offset.toArray();
        }
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public int dimension() {
        return dimension;
    }

    @Override
    public int size() {
        return deltas.length;
    }

    /** Returns the offsets of the visited cells, in visiting order. */
    public List<Coordinates> offsets() {
        return offsets;
    }

    /** Tells whether the cell under examination is part of its own neighborhood. */
    public boolean includesCenter() {
        return offsets.stream().anyMatch(Coordinates::isOrigin);
    }

    /** Returns how far the neighborhood reaches, as the largest Chebyshev norm of its offsets. */
    public int radius() {
        return offsets.stream().mapToInt(Coordinates::chebyshevNorm).max().orElse(0);
    }

    /**
     * Returns the same neighborhood deprived of the cell under examination.
     *
     * @param newName name of the resulting neighborhood
     * @throws IllegalArgumentException if nothing is left once the center is removed
     */
    public OffsetNeighborhood withoutCenter(String newName) {
        List<Coordinates> others = new ArrayList<>(offsets);
        others.removeIf(Coordinates::isOrigin);
        return new OffsetNeighborhood(newName, others);
    }

    @Override
    public Iterable<Cell> around(Grid grid, GridCell center) {
        if (grid.dimension() != dimension) {
            throw new IllegalArgumentException(
                    "neighborhood " + name + " is " + dimension + "-dimensional but the grid has "
                            + grid.dimension() + " dimension(s)");
        }
        return () -> new Walk(grid, center.position());
    }

    /** One traversal of the neighborhood of a given cell. */
    private final class Walk implements Iterator<Cell> {

        private final Grid grid;
        private final Coordinates center;
        /** Reused for every neighbor: {@link Grid#cellAt} does not retain its argument. */
        private final int[] cursor = new int[dimension];
        private int next;

        Walk(Grid grid, Coordinates center) {
            this.grid = grid;
            this.center = center;
        }

        @Override
        public boolean hasNext() {
            return next < deltas.length;
        }

        @Override
        public Cell next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            int[] delta = deltas[next++];
            for (int axis = 0; axis < dimension; axis++) {
                cursor[axis] = center.get(axis) + delta[axis];
            }
            return grid.cellAt(cursor);
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof OffsetNeighborhood that
                && name.equals(that.name)
                && offsets.equals(that.offsets);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, offsets);
    }

    /** Returns the definition in the notation of the subject, e.g. {@code G2* = {(-1), (1)}}. */
    @Override
    public String toString() {
        return name + " = "
                + offsets.stream().map(Coordinates::toString).collect(Collectors.joining(", ", "{", "}"));
    }
}
