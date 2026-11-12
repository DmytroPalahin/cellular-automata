package fr.uspn.cellula.rule;

import fr.uspn.cellula.grid.Grid;
import fr.uspn.cellula.grid.GridCell;

/**
 * The local transition function of a cellular automaton.
 *
 * <p>A rule decides the next state of one cell from what it can observe around that cell in the
 * current generation. It must not modify the grid: the automaton applies it to every cell
 * "simultaneously", possibly from several threads at once.
 *
 * <p>The engine depends only on this interface. Rules written in the rule language are provided
 * by {@link ExpressionRule}; any other implementation, such as a lambda in a test, works too.
 */
@FunctionalInterface
public interface Rule {

    /**
     * Computes the state a cell takes at the next time step.
     *
     * @param grid the grid in its current generation
     * @param cell the cell under examination
     * @return the next state of {@code cell}
     */
    int nextState(Grid grid, GridCell cell);
}
