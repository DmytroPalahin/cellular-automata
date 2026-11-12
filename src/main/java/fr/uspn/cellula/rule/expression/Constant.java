package fr.uspn.cellula.rule.expression;

/**
 * A numeric literal.
 *
 * @param value the value of the literal
 */
public record Constant(int value) implements Expression {

    @Override
    public int evaluate(EvaluationContext context) {
        return value;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
