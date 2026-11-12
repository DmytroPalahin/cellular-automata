package fr.uspn.cellula.rule.expression;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * An inner node of the syntax tree: a named operator applied to
 * sub-expressions.
 *
 * <p>
 * This class factors out what all value operators share, namely holding their
 * operands and
 * printing themselves, and leaves evaluation to its subclasses. Those are
 * organized by arity:
 *
 * <pre>
 * Operator
 * ├── UnaryOperator    NON
 * ├── BinaryOperator   ET OU SUP SUPEQ EQ ADD SUB MUL
 * └── Conditional      SI
 * </pre>
 *
 * <p>
 * Two operators are equal when they are of the same class and have equal
 * operands.
 */
public abstract class Operator implements Expression {

    private final String symbol;
    private final List<Expression> operands;

    /**
     * Initializes the common part of an operator.
     *
     * @param symbol   keyword of the operator in the rule language
     * @param operands the sub-expressions, in source order
     */
    protected Operator(String symbol, Expression... operands) {
        this.symbol = Objects.requireNonNull(symbol, "symbol");
        this.operands = List.of(operands);
    }

    /** Returns the keyword of this operator in the rule language. */
    public final String symbol() {
        return symbol;
    }

    @Override
    public final List<Expression> operands() {
        return operands;
    }

    @Override
    public final boolean equals(Object other) {
        return other != null
                && getClass() == other.getClass()
                && operands.equals(((Operator) other).operands);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(getClass(), operands);
    }

    @Override
    public final String toString() {
        return operands.stream()
                .map(Expression::toString)
                .collect(Collectors.joining(", ", symbol + "(", ")"));
    }
}
