package fr.uspn.cellula.rule.expression;

/** {@code OU(a, b)}: yields 0 if both operands are equal to 0, and 1 otherwise. */
public final class Or extends BinaryOperator {

    /** The keyword of this operator in the rule language. */
    public static final String SYMBOL = "OU";

    /**
     * Creates the operator.
     *
     * @param left first operand
     * @param right second operand
     */
    public Or(Expression left, Expression right) {
        super(SYMBOL, left, right);
    }

    @Override
    protected int apply(int left, int right) {
        return left != 0 || right != 0 ? 1 : 0;
    }
}
