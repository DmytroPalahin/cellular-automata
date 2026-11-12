package fr.uspn.cellula.rule.expression;

/** {@code SUPEQ(a, b)}: yields 1 if {@code a} is greater than or equal to {@code b}, and 0 otherwise. */
public final class GreaterOrEqual extends BinaryOperator {

    /** The keyword of this operator in the rule language. */
    public static final String SYMBOL = "SUPEQ";

    /**
     * Creates the operator.
     *
     * @param left first operand
     * @param right second operand
     */
    public GreaterOrEqual(Expression left, Expression right) {
        super(SYMBOL, left, right);
    }

    @Override
    protected int apply(int left, int right) {
        return left >= right ? 1 : 0;
    }
}
