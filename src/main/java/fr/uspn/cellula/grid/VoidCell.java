package fr.uspn.cellula.grid;

/**
 * The cell found at every position outside a grid with a {@link BoundaryCondition#FIXED fixed}
 * boundary.
 *
 * <p>Cellular automata are defined on an infinite grid; a computer can only store a finite part of
 * it. The subject settles the question by stating that any cell outside the grid is permanently
 * off. Rather than sprinkling bounds checks over the rules, this Null Object lets a neighborhood
 * hand out a perfectly ordinary {@link Cell} whose state happens to be {@link Cell#OFF} forever.
 *
 * <p>It is a singleton, implemented as a single-element enum.
 */
public enum VoidCell implements Cell {
    /** The unique instance. */
    INSTANCE;

    @Override
    public int state() {
        return OFF;
    }
}
