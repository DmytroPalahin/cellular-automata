package fr.uspn.cellula.rule.expression;

import java.util.Objects;

import fr.uspn.cellula.grid.Grid;
import fr.uspn.cellula.grid.GridCell;

/**
 * Everything an expression may look at while it is evaluated: the cell whose
 * next state is being
 * computed, and the grid around it.
 *
 * @param grid the grid, in its current generation
 * @param cell the cell under examination
 */
public record EvaluationContext(Grid grid, GridCell cell) {

    /** Rejects missing components. */
    public EvaluationContext {
        Objects.requireNonNull(grid, "grid");
        Objects.requireNonNull(cell, "cell");
    }
}
