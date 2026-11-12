package fr.uspn.cellula.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import fr.uspn.cellula.grid.Cell;
import fr.uspn.cellula.grid.Coordinates;
import fr.uspn.cellula.grid.Grid;
import fr.uspn.cellula.grid.Shape;
import fr.uspn.cellula.neighborhood.Neighborhood;
import fr.uspn.cellula.neighborhood.NeighborhoodCatalog;
import fr.uspn.cellula.rule.expression.Constant;
import fr.uspn.cellula.rule.parser.RuleSyntaxException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExpressionRuleTest {

    private final NeighborhoodCatalog catalog = new NeighborhoodCatalog(2);

    @Test
    void theNextStateIsTheValueOfTheExpression() {
        Grid grid = new Grid(Shape.of(3, 3));
        grid.setState(Coordinates.of(0, 0), Cell.ON);
        grid.setState(Coordinates.of(2, 2), Cell.ON);
        ExpressionRule rule = ExpressionRule.compile("ADD(COMPTER(G8*), 10)", catalog);

        assertEquals(12, rule.nextState(grid, grid.cell(1, 1)));
        assertEquals(10, rule.nextState(grid, grid.cell(0, 0)));
        assertEquals(7, new ExpressionRule(new Constant(7)).nextState(grid, grid.cell(0, 0)));
    }

    @Test
    void knowsWhichNeighborhoodsItExplores() {
        ExpressionRule rule = ExpressionRule.compile(
                "SI(COMPTER(G0), EQ(COMPTER(G8*), 2), ADD(COMPTER(G4), COMPTER(G8*)))", catalog);

        assertEquals(
                List.of("G0", "G8*", "G4"),
                rule.neighborhoods().stream().map(Neighborhood::name).toList());
        assertEquals(List.of(), List.copyOf(ExpressionRule.compile("ADD(1, ETAT())", catalog).neighborhoods()));
    }

    @Test
    void printsItselfInCanonicalForm() {
        ExpressionRule rule = ExpressionRule.compile("  if( count(G0) ,1,\n 0 )", catalog);

        assertEquals("SI(COMPTER(G0), 1, 0)", rule.toString());
        assertEquals(rule.expression(), ExpressionRule.compile(rule.toString(), catalog).expression());
    }

    @Test
    void compilingAnInvalidRuleFails() {
        assertThrows(RuleSyntaxException.class, () -> ExpressionRule.compile("COMPTER(G26)", catalog));
        assertThrows(NullPointerException.class, () -> new ExpressionRule(null));
    }
}
