package fr.uspn.cellula.rule.expression;

/** {@code ADD(a, b)}: yields the sum of the operands. */
public final class Add extends BinaryOperator {

    /** The keyword of this operator in the rule language. */
    public static final String SYMBOL = "ADD";

    /**
     * Creates the operator.
     *
     * @param left first operand
     * @param right second operand
     */
    public Add(Expression left, Expression right) {
        super(SYMBOL, left, right);
    }

    @Override
    protected int apply(int left, int right) {
        return left + right;
    }
}
