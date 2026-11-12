package fr.uspn.cellula.rule.expression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import fr.uspn.cellula.grid.BoundaryCondition;
import fr.uspn.cellula.grid.Coordinates;
import fr.uspn.cellula.grid.Grid;
import fr.uspn.cellula.grid.Shape;
import fr.uspn.cellula.neighborhood.NeighborhoodCatalog;
import fr.uspn.cellula.rule.parser.RuleParser;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Checks every operator against the definition given in section 2.3 of the subject. */
class OperatorsTest {

    /**
     * A 3 × 3 grid, where state 2 marks a cell that is neither off nor on:
     *
     * <pre>
     * 1 0 1
     * 0 1 2
     * 0 1 0
     * </pre>
     */
    private static Grid grid() {
        Grid grid = new Grid(Shape.of(3, 3), BoundaryCondition.FIXED);
        int[][] states = {{1, 0, 1}, {0, 1, 2}, {0, 1, 0}};
        grid.fill(position -> states[position.get(0)][position.get(1)]);
        return grid;
    }

    private static int evaluate(String source, int row, int column) {
        Grid grid = grid();
        Expression expression = new RuleParser(new NeighborhoodCatalog(2)).parse(source);
        return expression.evaluate(new EvaluationContext(grid, grid.cell(row, column)));
    }

    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource(delimiter = '|', value = {
        "ET(1, 1)     | 1", "ET(1, 0)     | 0", "ET(0, 1)     | 0", "ET(0, 0)     | 0", "ET(-3, 7)    | 1",
        "OU(1, 1)     | 1", "OU(1, 0)     | 1", "OU(0, 1)     | 1", "OU(0, 0)     | 0", "OU(0, -2)    | 1",
        "NON(0)       | 1", "NON(1)       | 0", "NON(5)       | 0", "NON(-1)      | 0",
        "SUP(3, 2)    | 1", "SUP(2, 2)    | 0", "SUP(1, 2)    | 0", "SUP(-1, -2)  | 1",
        "SUPEQ(3, 2)  | 1", "SUPEQ(2, 2)  | 1", "SUPEQ(1, 2)  | 0",
        "EQ(4, 4)     | 1", "EQ(4, 5)     | 0", "EQ(0, 0)     | 1",
        "ADD(2, 3)    | 5", "ADD(2, -3)   | -1",
        "SUB(2, 3)    | -1", "SUB(7, 2)   | 5",
        "MUL(4, 3)    | 12", "MUL(4, 0)   | 0", "MUL(-2, 3)   | -6",
        "SI(1, 10, 20)  | 10", "SI(0, 10, 20) | 20", "SI(-4, 10, 20) | 10",
        "ADD(MUL(2, 3), SI(EQ(1, 1), 4, 5)) | 10",
    })
    void valueOperatorsFollowTheirDefinition(String source, int expected) {
        assertEquals(expected, evaluate(source, 1, 1));
    }

    @ParameterizedTest(name = "{0} at ({1}, {2}) = {3}")
    @CsvSource(delimiter = '|', value = {
        "COMPTER(G0)  | 1 | 1 | 1",
        "COMPTER(G0)  | 1 | 0 | 0",
        "COMPTER(G4)  | 1 | 1 | 2",
        "COMPTER(G4*) | 1 | 1 | 1",
        "COMPTER(G8)  | 1 | 1 | 4",
        "COMPTER(G8*) | 1 | 1 | 3",
        "COMPTER(G8)  | 0 | 0 | 2",
        "COMPTER(G8*) | 0 | 0 | 1",
        "COMPTER(G8*) | 2 | 2 | 2",
        "COMPTER(G4*) | 0 | 1 | 3",
    })
    void countingOnlySeesCellsInStateOne(String source, int row, int column, int expected) {
        assertEquals(expected, evaluate(source, row, column));
    }

    @Test
    void aCellInAnotherStateIsNeitherCountedNorOff() {
        assertEquals(0, evaluate("COMPTER(G0)", 1, 2));
        assertEquals(2, evaluate("ETAT()", 1, 2));
        assertEquals(1, evaluate("ETAT()", 1, 1));
        assertEquals(0, evaluate("ETAT()", 2, 0));
    }

    @Test
    void countingWrapsOnAPeriodicGrid() {
        Grid grid = new Grid(Shape.of(4), BoundaryCondition.PERIODIC);
        grid.setState(Coordinates.of(3), 1);
        Expression count = new RuleParser(new NeighborhoodCatalog(1)).parse("COMPTER(G2*)");

        assertEquals(1, count.evaluate(new EvaluationContext(grid, grid.cell(0))));
        assertEquals(0, count.evaluate(new EvaluationContext(grid, grid.cell(1))));
    }

    @Test
    void aConditionalOnlyEvaluatesTheSelectedBranch() {
        List<String> evaluated = new ArrayList<>();
        Expression thenBranch = context -> {
            evaluated.add("then");
            return 1;
        };
        Expression elseBranch = context -> {
            evaluated.add("else");
            return 2;
        };
        Grid grid = grid();
        EvaluationContext context = new EvaluationContext(grid, grid.cell(0, 0));

        assertEquals(1, new Conditional(new Constant(5), thenBranch, elseBranch).evaluate(context));
        assertEquals(2, new Conditional(new Constant(0), thenBranch, elseBranch).evaluate(context));
        assertEquals(List.of("then", "else"), evaluated);
    }

    @Test
    void operatorsExposeTheirStructure() {
        Operator sum = new Add(new Constant(1), new Not(new Constant(2)));

        assertEquals("ADD", sum.symbol());
        assertEquals(List.of(new Constant(1), new Not(new Constant(2))), sum.operands());
        assertEquals(List.of(), new Constant(1).operands());

        List<String> visited = new ArrayList<>();
        sum.walk(node -> visited.add(node.toString()));
        assertEquals(List.of("ADD(1, NON(2))", "1", "NON(2)", "2"), visited);
    }

    @Test
    void operatorsCompareByKindAndOperands() {
        assertEquals(new Add(new Constant(1), new Constant(2)), new Add(new Constant(1), new Constant(2)));
        assertEquals(
                new Add(new Constant(1), new Constant(2)).hashCode(),
                new Add(new Constant(1), new Constant(2)).hashCode());
        assertNotEquals(new Add(new Constant(1), new Constant(2)), new Add(new Constant(2), new Constant(1)));
        assertNotEquals(new Add(new Constant(1), new Constant(2)), new Subtract(new Constant(1), new Constant(2)));
    }

    @Test
    void missingPartsAreRejectedEarly() {
        assertThrows(NullPointerException.class, () -> new Add(new Constant(1), null));
        assertThrows(NullPointerException.class, () -> new Count(null));
        assertThrows(NullPointerException.class, () -> new EvaluationContext(null, null));
    }
}
