package fr.uspn.cellula.rule.expression;

/** {@code SUP(a, b)}: yields 1 if {@code a} is strictly greater than {@code b}, and 0 otherwise. */
public final class GreaterThan extends BinaryOperator {

    /** The keyword of this operator in the rule language. */
    public static final String SYMBOL = "SUP";

    /**
     * Creates the operator.
     *
     * @param left first operand
     * @param right second operand
     */
    public GreaterThan(Expression left, Expression right) {
        super(SYMBOL, left, right);
    }

    @Override
    protected int apply(int left, int right) {
        return left > right ? 1 : 0;
    }
}
