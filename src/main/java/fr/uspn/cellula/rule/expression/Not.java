package fr.uspn.cellula.rule.expression;

/** {@code NON(a)}: yields 1 if the operand equals 0, and 0 otherwise. */
public final class Not extends UnaryOperator {

    /** The keyword of this operator in the rule language. */
    public static final String SYMBOL = "NON";

    /**
     * Creates the operator.
     *
     * @param operand the value to negate
     */
    public Not(Expression operand) {
        super(SYMBOL, operand);
    }

    @Override
    protected int apply(int value) {
        return value == 0 ? 1 : 0;
    }
}
