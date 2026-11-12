package fr.uspn.cellula.rule.expression;

/**
 * {@code ETAT()}: the state of the cell under examination.
 *
 * <p>This operator is an extension to the subject. With two states it is redundant, since
 * {@code COMPTER(G0)} already tells whether the cell is on. It becomes necessary as soon as a rule
 * produces a third state: {@code COMPTER} only counts cells in state 1, so a cell in state 2 would
 * otherwise be indistinguishable from a cell in state 0 (see the Brian's Brain example).
 */
public record CurrentState() implements Expression {

    /** The keyword of this operator in the rule language. */
    public static final String SYMBOL = "ETAT";

    @Override
    public int evaluate(EvaluationContext context) {
        return context.cell().state();
    }

    @Override
    public String toString() {
        return SYMBOL + "()";
    }
}
