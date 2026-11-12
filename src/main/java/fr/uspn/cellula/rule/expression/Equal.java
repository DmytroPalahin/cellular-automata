package fr.uspn.cellula.rule.expression;

/**
 * {@code EQ(a, b)}: yields 1 if {@code a} equals {@code b}, and 0 otherwise.
 */
public final class Equal extends BinaryOperator {

    /** The keyword of this operator in the rule language. */
    public static final String SYMBOL = "EQ";

    /**
     * Creates the operator.
     *
     * @param left  first operand
     * @param right second operand
     */
    public Equal(Expression left, Expression right) {
        super(SYMBOL, left, right);
    }

    @Override
    protected int apply(int left, int right) {
        return left == right ? 1 : 0;
    }
}
