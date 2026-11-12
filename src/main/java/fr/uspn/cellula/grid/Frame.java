package fr.uspn.cellula.grid;

import java.util.Objects;

/**
 * An immutable two-dimensional picture of cell states, ready to be displayed.
 *
 * <p>
 * A frame is what crosses the boundary between the simulation and whatever
 * shows it: a plane
 * cut through a grid ({@link Slice#extract}), or the successive generations of
 * a one-dimensional
 * automaton stacked on top of each other.
 *
 * @param rows    number of rows
 * @param columns number of columns
 * @param states  the states in row-major order; the array is owned by the frame
 *                and must not be
 *                modified after construction
 */
public record Frame(int rows, int columns, int[] states) {

    /** Validates the dimensions against the number of states. */
    public Frame {
        Objects.requireNonNull(states, "states");
        if (rows < 0 || columns < 0 || (long) rows * columns != states.length) {
            throw new IllegalArgumentException(
                    "a " + rows + " × " + columns + " frame cannot hold " + states.length
                            + " states");
        }
    }

    /** Returns the state shown at the given row and column. */
    public int stateAt(int row, int column) {
        Objects.checkIndex(row, rows);
        Objects.checkIndex(column, columns);
        return states[row * columns + column];
    }
}
