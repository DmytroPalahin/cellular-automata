package fr.uspn.cellula.rule.expression;

/**
 * An operator with two operands.
 *
 * <p>
 * Evaluation is a template method: this class evaluates both operands, left to
 * right, and the
 * subclass only says how to combine the two resulting values. Since expressions
 * have no side
 * effects, evaluating both operands eagerly is unobservable, which keeps the
 * logical operators as
 * simple as the arithmetic ones.
 */
public abstract class BinaryOperator extends Operator {

    private final Expression left;
    private final Expression right;

    /**
     * Initializes the operator.
     *
     * @param symbol keyword of the operator in the rule language
     * @param left   first operand
     * @param right  second operand
     */
    protected BinaryOperator(String symbol, Expression left, Expression right) {
        super(symbol, left, right);
        this.left = left;
        this.right = right;
    }

    @Override
    public final int evaluate(EvaluationContext context) {
        return apply(left.evaluate(context), right.evaluate(context));
    }

    /**
     * Computes the result of the operator.
     *
     * @param left  the value of the first operand
     * @param right the value of the second operand
     */
    protected abstract int apply(int left, int right);
}
