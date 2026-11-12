package fr.uspn.cellula.rule.expression;

/**
 * An operator with one operand.
 *
 * <p>Evaluation is a template method: this class evaluates the operand, the subclass only says how
 * to transform the resulting value.
 */
public abstract class UnaryOperator extends Operator {

    private final Expression operand;

    /**
     * Initializes the operator.
     *
     * @param symbol keyword of the operator in the rule language
     * @param operand the sub-expression
     */
    protected UnaryOperator(String symbol, Expression operand) {
        super(symbol, operand);
        this.operand = operand;
    }

    @Override
    public final int evaluate(EvaluationContext context) {
        return apply(operand.evaluate(context));
    }

    /**
     * Computes the result of the operator.
     *
     * @param value the value of the operand
     */
    protected abstract int apply(int value);
}
