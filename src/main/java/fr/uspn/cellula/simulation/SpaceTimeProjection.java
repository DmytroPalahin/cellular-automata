package fr.uspn.cellula.simulation;

import java.util.ArrayDeque;
import java.util.Deque;

import fr.uspn.cellula.grid.Coordinates;
import fr.uspn.cellula.grid.Frame;
import fr.uspn.cellula.grid.Grid;

/**
 * Shows the history of a one-dimensional grid: one row per generation, the
 * oldest on top.
 *
 * <p>
 * This is the "juxtaposition of successive states" of the subject, the display
 * in which rule
 * 18 draws the Sierpiński triangle. The history is bounded: once it is full,
 * the oldest generation
 * is dropped for each new one, so the picture scrolls.
 */
public final class SpaceTimeProjection implements Projection {

    /** Default number of generations kept. */
    public static final int DEFAULT_CAPACITY = 1024;

    private final int capacity;
    private final Deque<int[]> rows = new ArrayDeque<>();

    /** Creates a projection that keeps {@value #DEFAULT_CAPACITY} generations. */
    public SpaceTimeProjection() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * Creates a projection.
     *
     * @param capacity maximum number of generations kept, at least one
     */
    public SpaceTimeProjection(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be positive, got " + capacity);
        }
        this.capacity = capacity;
    }

    @Override
    public void restarted(Grid grid) {
        rows.clear();
        rows.addLast(row(grid));
    }

    @Override
    public void advanced(Grid grid) {
        if (rows.size() == capacity) {
            rows.removeFirst();
        }
        rows.addLast(row(grid));
    }

    @Override
    public void edited(Grid grid) {
        rows.pollLast();
        rows.addLast(row(grid));
    }

    @Override
    public Frame frame(Grid grid) {
        if (rows.isEmpty()) {
            restarted(grid);
        }
        int columns = grid.shape().size(0);
        int[] states = new int[rows.size() * columns];
        int offset = 0;
        for (int[] row : rows) {
            System.arraycopy(row, 0, states, offset, columns);
            offset += columns;
        }
        return new Frame(rows.size(), columns, states);
    }

    /**
     * Whatever the row, a column of the picture stands for the cell of the current
     * generation.
     */
    @Override
    public Coordinates locate(int row, int column) {
        return Coordinates.of(column);
    }

    private static int[] row(Grid grid) {
        if (grid.dimension() != 1) {
            throw new IllegalArgumentException(
                    "a space-time diagram needs a one-dimensional grid, got " + grid.dimension()
                            + " dimensions");
        }
        return grid.snapshot();
    }
}
