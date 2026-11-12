package fr.uspn.cellula.rule.expression;

import java.util.List;
import java.util.function.Consumer;

/**
 * A node of the syntax tree of an evolution rule.
 *
 * <p>
 * A rule such as {@code SI(EQ(COMPTER(G0), 1), 0, 1)} is a tree: operators are
 * inner nodes,
 * numeric constants and neighborhood counts are leaves. Every node evaluates to
 * an integer for a
 * given cell, and an operator computes its value from the values of its
 * operands. This is the
 * Composite pattern (leaves and composites share one interface) used as an
 * Interpreter (the tree
 * evaluates itself).
 *
 * <p>
 * Expressions are immutable and free of side effects, so one tree is safely
 * evaluated for
 * many cells at the same time.
 */
public interface Expression {

    /**
     * Computes the value of this expression for one cell.
     *
     * @param context the cell under examination and the grid it lives in
     * @return the value; when used as a truth value, {@code 0} is false and
     *         anything else is true
     */
    int evaluate(EvaluationContext context);

    /**
     * Returns the direct sub-expressions of this node, in source order; empty for a
     * leaf.
     */
    default List<Expression> operands() {
        return List.of();
    }

    /**
     * Visits this node and all its descendants in pre-order.
     *
     * @param visitor called once per node
     */
    default void walk(Consumer<? super Expression> visitor) {
        visitor.accept(this);
        for (Expression operand : operands()) {
            operand.walk(visitor);
        }
    }

    /**
     * Returns this expression written in the rule language, in canonical form
     * (French keywords,
     * single spaces after commas). Parsing the result yields an equal tree.
     */
    @Override
    String toString();
}
