package fr.uspn.cellula.grid;

/**
 * A cell stored in a {@link Grid}.
 *
 * <p>Besides its current state, a cell buffers the state it will take at the next time step. This
 * is what lets a whole grid evolve <em>simultaneously</em>: every cell first computes its next
 * state from the untouched current generation ({@link #stage}), then all cells switch at once
 * ({@link #commit}). Both operations are reserved to {@link Grid}, which owns the protocol.
 */
public final class GridCell implements Cell {

    private final Coordinates position;
    private int state;
    private int staged;

    GridCell(Coordinates position) {
        this.position = position;
    }

    /** Returns the position of this cell in its grid. */
    public Coordinates position() {
        return position;
    }

    @Override
    public int state() {
        return state;
    }

    /** Sets the state immediately, outside of the two-phase update. */
    void set(int newState) {
        state = newState;
        staged = newState;
    }

    /** Records the state this cell will take when the generation is committed. */
    void stage(int nextState) {
        staged = nextState;
    }

    /**
     * Switches to the staged state.
     *
     * @return {@code true} if the state changed
     */
    boolean commit() {
        boolean changed = state != staged;
        state = staged;
        return changed;
    }

    @Override
    public String toString() {
        return "Cell" + position + "=" + state;
    }
}
