package fr.uspn.cellula.rule.expression;

/** {@code SUB(a, b)}: yields the difference {@code a - b}. */
public final class Subtract extends BinaryOperator {

    /** The keyword of this operator in the rule language. */
    public static final String SYMBOL = "SUB";

    /**
     * Creates the operator.
     *
     * @param left first operand
     * @param right second operand
     */
    public Subtract(Expression left, Expression right) {
        super(SYMBOL, left, right);
    }

    @Override
    protected int apply(int left, int right) {
        return left - right;
    }
}
