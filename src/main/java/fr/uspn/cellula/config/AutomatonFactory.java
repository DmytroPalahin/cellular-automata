package fr.uspn.cellula.config;

import fr.uspn.cellula.automaton.CellularAutomaton;
import fr.uspn.cellula.grid.Grid;
import fr.uspn.cellula.neighborhood.NeighborhoodCatalog;
import fr.uspn.cellula.rule.ExpressionRule;

/**
 * Turns a definition into a live automaton.
 *
 * <p>
 * This is where the pieces of the model are wired together: the neighborhood
 * catalog is built
 * for the dimension of the grid and extended with the user's neighborhoods, the
 * rule is compiled
 * against that catalog, the grid is allocated and filled.
 */
public final class AutomatonFactory {

    private AutomatonFactory() {
    }

    /**
     * Instantiates an automaton at generation 0.
     *
     * @param definition the description of the automaton
     * @return a new, independent automaton
     * @throws IllegalArgumentException if the definition is inconsistent, for
     *                                  example if its rule
     *                                  does not parse
     *                                  ({@link fr.uspn.cellula.rule.parser.RuleSyntaxException})
     */
    public static CellularAutomaton create(AutomatonDefinition definition) {
        ExpressionRule rule = compileRule(definition);
        Grid grid = new Grid(definition.shape(), definition.boundary());
        definition.initialState().applyTo(grid);
        return new CellularAutomaton(grid, rule);
    }

    /**
     * Compiles the rule of a definition without allocating its grid.
     *
     * <p>
     * Besides being the first half of {@link #create}, this is a cheap way to check
     * that a
     * definition is usable, which the file reader does before accepting a file.
     *
     * @param definition the description of the automaton
     * @return the compiled rule
     * @throws IllegalArgumentException if a neighborhood is ill-defined or the rule
     *                                  does not parse
     */
    public static ExpressionRule compileRule(AutomatonDefinition definition) {
        NeighborhoodCatalog catalog = new NeighborhoodCatalog(definition.dimension());
        definition.neighborhoods().forEach(catalog::define);
        return ExpressionRule.compile(definition.rule(), catalog);
    }
}
