package fr.uspn.cellula.rule.expression;

/**
 * {@code SI(condition, then, otherwise)}: yields {@code then} if the condition is different from
 * 0, and {@code otherwise} if it equals 0.
 *
 * <p>Only the selected branch is evaluated. This cannot change the result, because expressions
 * have no side effects, but it does spare whole sub-trees and their neighborhood walks.
 */
public final class Conditional extends Operator {

    /** The keyword of this operator in the rule language. */
    public static final String SYMBOL = "SI";

    private final Expression condition;
    private final Expression whenTrue;
    private final Expression whenFalse;

    /**
     * Creates the operator.
     *
     * @param condition the value to test
     * @param whenTrue the result when the condition is different from 0
     * @param whenFalse the result when the condition equals 0
     */
    public Conditional(Expression condition, Expression whenTrue, Expression whenFalse) {
        super(SYMBOL, condition, whenTrue, whenFalse);
        this.condition = condition;
        this.whenTrue = whenTrue;
        this.whenFalse = whenFalse;
    }

    @Override
    public int evaluate(EvaluationContext context) {
        return condition.evaluate(context) != 0
                ? whenTrue.evaluate(context)
                : whenFalse.evaluate(context);
    }
}
