package fr.uspn.cellula.rule;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import fr.uspn.cellula.grid.Grid;
import fr.uspn.cellula.grid.GridCell;
import fr.uspn.cellula.neighborhood.Neighborhood;
import fr.uspn.cellula.neighborhood.NeighborhoodCatalog;
import fr.uspn.cellula.rule.expression.Count;
import fr.uspn.cellula.rule.expression.EvaluationContext;
import fr.uspn.cellula.rule.expression.Expression;
import fr.uspn.cellula.rule.parser.RuleParser;

/**
 * A rule given as an expression of the rule language: the next state of a cell
 * is the value of
 * the expression evaluated for that cell.
 */
public final class ExpressionRule implements Rule {

    private final Expression expression;

    /**
     * Wraps an already built syntax tree.
     *
     * @param expression the expression giving the next state of a cell
     */
    public ExpressionRule(Expression expression) {
        this.expression = Objects.requireNonNull(expression, "expression");
    }

    /**
     * Compiles the text of a rule.
     *
     * @param source        the rule, in the prefix notation of the rule language
     * @param neighborhoods the neighborhoods the rule may refer to
     * @return the compiled rule
     * @throws fr.uspn.cellula.rule.parser.RuleSyntaxException if the text is not a
     *                                                         valid rule
     */
    public static ExpressionRule compile(String source, NeighborhoodCatalog neighborhoods) {
        return new ExpressionRule(new RuleParser(neighborhoods).parse(source));
    }

    /** Returns the syntax tree of the rule. */
    public Expression expression() {
        return expression;
    }

    /**
     * Returns the neighborhoods the rule explores, in order of first appearance.
     */
    public Set<Neighborhood> neighborhoods() {
        Set<Neighborhood> used = new LinkedHashSet<>();
        expression.walk(node -> {
            if (node instanceof Count count) {
                used.add(count.neighborhood());
            }
        });
        return used;
    }

    @Override
    public int nextState(Grid grid, GridCell cell) {
        return expression.evaluate(new EvaluationContext(grid, cell));
    }

    /** Returns the rule in canonical form. */
    @Override
    public String toString() {
        return expression.toString();
    }
}
