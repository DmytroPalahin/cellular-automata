package fr.uspn.cellula.rule.expression;

import fr.uspn.cellula.grid.Cell;
import fr.uspn.cellula.neighborhood.Neighborhood;
import java.util.Objects;

/**
 * {@code COMPTER(G)}: the number of cells in state 1 in the neighborhood {@code G} of the cell
 * under examination.
 *
 * <p>This is the only operator that reads the grid, and the only one whose operand is a
 * neighborhood rather than a value. It simply walks the neighborhood, which is handed out as an
 * {@link Iterable}; positions beyond the edge of the grid show up as cells that are off (or as
 * wrapped cells on a periodic grid) without this class knowing anything about it.
 *
 * @param neighborhood the neighborhood to explore
 */
public record Count(Neighborhood neighborhood) implements Expression {

    /** The keyword of this operator in the rule language. */
    public static final String SYMBOL = "COMPTER";

    /** Rejects a missing neighborhood. */
    public Count {
        Objects.requireNonNull(neighborhood, "neighborhood");
    }

    @Override
    public int evaluate(EvaluationContext context) {
        int count = 0;
        for (Cell neighbor : neighborhood.around(context.grid(), context.cell())) {
            if (neighbor.isOn()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public String toString() {
        return SYMBOL + "(" + neighborhood.name() + ")";
    }
}
