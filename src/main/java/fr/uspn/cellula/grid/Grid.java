package fr.uspn.cellula.grid;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.ToIntFunction;

/**
 * A finite, regular, {@code d}-dimensional grid of cells.
 *
 * <p>
 * The cells live in a {@link DynamicArrayND}, so the dimension is a run-time
 * value. On top of
 * storage, the grid is responsible for two things:
 *
 * <ul>
 * <li>answering "which cell is at these coordinates?" for <em>any</em>
 * coordinates, delegating
 * positions beyond the edge to its {@link BoundaryCondition};
 * <li>applying a transition to all cells <em>simultaneously</em>
 * ({@link #evolve}).
 * </ul>
 *
 * <p>
 * A grid is not thread-safe: it must be confined to one thread, or handed over
 * between threads
 * with a proper happens-before edge. {@link #evolve} internally fans out to
 * several threads but
 * returns only once they are all done.
 */
public final class Grid {

    /** Below this number of cells a generation is computed sequentially. */
    private static final int PARALLEL_THRESHOLD = 8_192;

    private final Shape shape;
    private final BoundaryCondition boundary;
    private final DynamicArrayND<GridCell> storage;
    private final List<GridCell> cells;

    /**
     * Creates a grid whose cells are all {@link Cell#OFF off}.
     *
     * @param shape    extent of the grid along each axis
     * @param boundary what lies beyond the edges
     */
    public Grid(Shape shape, BoundaryCondition boundary) {
        this.shape = Objects.requireNonNull(shape, "shape");
        this.boundary = Objects.requireNonNull(boundary, "boundary");
        this.storage = DynamicArrayND.create(GridCell::new, shape.toArray());
        this.cells = Collections.unmodifiableList(storage.toList());
    }

    /** Creates a grid with a {@link BoundaryCondition#FIXED fixed} boundary. */
    public Grid(Shape shape) {
        this(shape, BoundaryCondition.FIXED);
    }

    /** Returns the extent of the grid. */
    public Shape shape() {
        return shape;
    }

    /** Returns the number of axes of the grid. */
    public int dimension() {
        return shape.dimension();
    }

    /** Returns the boundary condition. */
    public BoundaryCondition boundary() {
        return boundary;
    }

    /**
     * Returns every cell of the grid in row-major order, as an unmodifiable list.
     */
    public List<GridCell> cells() {
        return cells;
    }

    /**
     * Returns the cell a rule sees at the given coordinates.
     *
     * <p>
     * Unlike {@link #cell(int...)}, this method accepts coordinates beyond the
     * edges: they are
     * resolved by the boundary condition. The argument array is never modified nor
     * retained, so
     * callers may reuse it as a scratch buffer.
     *
     * @param coordinates one value per axis, possibly out of range
     * @throws IllegalArgumentException if the number of coordinates is not
     *                                  {@link #dimension()}
     */
    public Cell cellAt(int... coordinates) {
        if (shape.contains(coordinates)) {
            return storage.get(coordinates);
        }
        return boundary.outside(this, coordinates);
    }

    /**
     * Returns the cell stored at the given coordinates.
     *
     * @param coordinates one value per axis, all within the grid
     * @throws IndexOutOfBoundsException if the coordinates lie outside the grid
     */
    public GridCell cell(int... coordinates) {
        return storage.get(coordinates);
    }

    /**
     * Returns the cell stored at the given position.
     *
     * @throws IndexOutOfBoundsException if the position lies outside the grid
     */
    public GridCell cell(Coordinates position) {
        return storage.get(position.toArray());
    }

    /**
     * Sets the state of one cell immediately.
     *
     * @throws IndexOutOfBoundsException if the position lies outside the grid
     */
    public void setState(Coordinates position, int state) {
        cell(position).set(state);
    }

    /**
     * Sets the state of every cell immediately.
     *
     * @param states computes the state of the cell at a given position
     */
    public void fill(ToIntFunction<? super Coordinates> states) {
        for (GridCell cell : cells) {
            cell.set(states.applyAsInt(cell.position()));
        }
    }

    /** Switches every cell {@link Cell#OFF off}. */
    public void clear() {
        fill(position -> Cell.OFF);
    }

    /**
     * Advances the grid by one time step.
     *
     * <p>
     * The update is synchronous, as the definition of a cellular automaton demands:
     * the
     * transition is first evaluated for every cell against the current generation,
     * and only then
     * do all cells take their new state. Because the first phase only reads current
     * states and
     * writes each cell's private buffer, it is safely run in parallel on large
     * grids.
     *
     * @param transition computes the next state of a cell; must not modify the grid
     * @return the number of cells whose state changed
     */
    public int evolve(ToIntFunction<? super GridCell> transition) {
        Objects.requireNonNull(transition, "transition");
        if (cells.size() >= PARALLEL_THRESHOLD) {
            cells.parallelStream().forEach(cell -> cell.stage(transition.applyAsInt(cell)));
        } else {
            for (GridCell cell : cells) {
                cell.stage(transition.applyAsInt(cell));
            }
        }
        int changed = 0;
        for (GridCell cell : cells) {
            if (cell.commit()) {
                changed++;
            }
        }
        return changed;
    }

    /** Returns the number of cells that are not {@link Cell#OFF off}. */
    public int population() {
        int population = 0;
        for (GridCell cell : cells) {
            if (cell.state() != Cell.OFF) {
                population++;
            }
        }
        return population;
    }

    /** Returns the state of every cell, in row-major order. */
    public int[] snapshot() {
        int[] states = new int[cells.size()];
        for (int index = 0; index < states.length; index++) {
            states[index] = cells.get(index).state();
        }
        return states;
    }

    /**
     * Restores states previously captured by {@link #snapshot()}.
     *
     * @throws IllegalArgumentException if the snapshot was taken from a grid of
     *                                  another size
     */
    public void restore(int[] states) {
        if (states.length != cells.size()) {
            throw new IllegalArgumentException(
                    "snapshot holds " + states.length + " states but the grid has "
                            + cells.size() + " cells");
        }
        for (int index = 0; index < states.length; index++) {
            cells.get(index).set(states[index]);
        }
    }

    /**
     * Returns a 64-bit digest of the states (FNV-1a over the row-major sequence).
     *
     * <p>
     * Two grids of the same shape in the same configuration always have the same
     * fingerprint;
     * different configurations collide with negligible probability. This is what
     * cycle detection
     * relies on to remember past generations cheaply.
     */
    public long fingerprint() {
        long hash = 0xcbf29ce484222325L;
        for (GridCell cell : cells) {
            hash ^= cell.state();
            hash *= 0x100000001b3L;
        }
        return hash;
    }

    @Override
    public String toString() {
        return "Grid[" + shape + ", " + boundary.label() + "]";
    }
}
