package fr.uspn.cellula.rule.expression;

/**
 * {@code ET(a, b)}: yields 1 if both operands are different from 0, and 0
 * otherwise.
 */
public final class And extends BinaryOperator {

    /** The keyword of this operator in the rule language. */
    public static final String SYMBOL = "ET";

    /**
     * Creates the operator.
     *
     * @param left  first operand
     * @param right second operand
     */
    public And(Expression left, Expression right) {
        super(SYMBOL, left, right);
    }

    @Override
    protected int apply(int left, int right) {
        return left != 0 && right != 0 ? 1 : 0;
    }
}
