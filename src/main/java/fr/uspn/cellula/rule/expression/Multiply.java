package fr.uspn.cellula.rule.expression;

/** {@code MUL(a, b)}: yields the product of the operands. */
public final class Multiply extends BinaryOperator {

    /** The keyword of this operator in the rule language. */
    public static final String SYMBOL = "MUL";

    /**
     * Creates the operator.
     *
     * @param left first operand
     * @param right second operand
     */
    public Multiply(Expression left, Expression right) {
        super(SYMBOL, left, right);
    }

    @Override
    protected int apply(int left, int right) {
        return left * right;
    }
}
