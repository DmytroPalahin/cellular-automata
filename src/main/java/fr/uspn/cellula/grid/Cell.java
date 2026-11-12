package fr.uspn.cellula.grid;

/**
 * A cell as seen by an evolution rule: something that has a state.
 *
 * <p>
 * Rules only ever read cells. The two implementations are {@link GridCell}, a
 * real cell stored
 * in a {@link Grid}, and {@link VoidCell}, which stands for every position
 * lying outside the grid.
 */
public interface Cell {

    /** The state of a cell that is switched off ("dead"). */
    int OFF = 0;

    /** The state of a cell that is switched on ("alive"). */
    int ON = 1;

    /** Returns the current state of the cell. */
    int state();

    /** Returns {@code true} when the cell is in state {@link #ON}. */
    default boolean isOn() {
        return state() == ON;
    }
}
