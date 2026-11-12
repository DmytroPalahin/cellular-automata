package fr.uspn.cellula.automaton;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.uspn.cellula.grid.BoundaryCondition;
import fr.uspn.cellula.grid.Cell;
import fr.uspn.cellula.grid.Coordinates;
import fr.uspn.cellula.grid.Grid;
import fr.uspn.cellula.grid.Shape;
import fr.uspn.cellula.neighborhood.NeighborhoodCatalog;
import fr.uspn.cellula.rule.ExpressionRule;
import fr.uspn.cellula.rule.Rule;
import org.junit.jupiter.api.Test;

class CellularAutomatonTest {

    private static final String LIFE = """
            SI(EQ(COMPTER(G0), 1),
               OU(EQ(COMPTER(G8*), 2), EQ(COMPTER(G8*), 3)),
               EQ(COMPTER(G8*), 3))""";

    private static CellularAutomaton life(BoundaryCondition boundary, int rows, int columns, int[]... alive) {
        Grid grid = new Grid(Shape.of(rows, columns), boundary);
        for (int[] cell : alive) {
            grid.setState(Coordinates.of(cell), Cell.ON);
        }
        return new CellularAutomaton(grid, ExpressionRule.compile(LIFE, new NeighborhoodCatalog(2)));
    }

    private static int[][] cells(int[]... cells) {
        return cells;
    }

    @Test
    void startsAtGenerationZero() {
        CellularAutomaton automaton = life(BoundaryCondition.FIXED, 4, 4);

        assertEquals(0, automaton.generation());
        assertEquals(Regime.TRANSIENT, automaton.regime());
        assertFalse(automaton.regime().isSettled());
    }

    @Test
    void anEmptyGridStaysEmptyAndConvergesAtOnce() {
        CellularAutomaton automaton = life(BoundaryCondition.FIXED, 6, 6);

        StepReport report = automaton.step();

        assertEquals(new StepReport(1, 0, Regime.FIXED_POINT), report);
        assertEquals(0, automaton.grid().population());
        assertTrue(automaton.regime().isSettled());
    }

    @Test
    void aBlockIsAStillLife() {
        CellularAutomaton automaton = life(
                BoundaryCondition.FIXED, 4, 4, cells(new int[] {1, 1}, new int[] {1, 2}, new int[] {2, 1}, new int[] {2, 2}));
        int[] before = automaton.grid().snapshot();

        automaton.step();

        assertArrayEquals(before, automaton.grid().snapshot());
        assertEquals(Regime.FIXED_POINT, automaton.regime());
        assertEquals("converged", automaton.regime().describe());
    }

    @Test
    void aBlinkerOscillatesWithPeriodTwo() {
        CellularAutomaton automaton = life(
                BoundaryCondition.FIXED, 5, 5, cells(new int[] {2, 1}, new int[] {2, 2}, new int[] {2, 3}));
        int[] horizontal = automaton.grid().snapshot();

        StepReport first = automaton.step();
        assertEquals(4, first.changedCells());
        assertEquals(Regime.TRANSIENT, first.regime());
        assertTrue(automaton.grid().cell(1, 2).isOn());
        assertTrue(automaton.grid().cell(3, 2).isOn());
        assertFalse(automaton.grid().cell(2, 1).isOn());

        StepReport second = automaton.step();
        assertArrayEquals(horizontal, automaton.grid().snapshot());
        assertEquals(Regime.cycle(2), second.regime());
        assertEquals("cycle of period 2", second.regime().describe());
        assertEquals(2, automaton.generation());
    }

    @Test
    void aGliderMovesDiagonallyByOneCellEveryFourGenerations() {
        int[][] glider = cells(
                new int[] {0, 1}, new int[] {1, 2}, new int[] {2, 0}, new int[] {2, 1}, new int[] {2, 2});
        CellularAutomaton automaton = life(BoundaryCondition.FIXED, 10, 10, glider);

        automaton.advance(4);

        assertEquals(5, automaton.grid().population());
        for (int[] cell : glider) {
            assertTrue(automaton.grid().cell(cell[0] + 1, cell[1] + 1).isOn());
        }
    }

    @Test
    void onATorusAGliderComesBackToItsStartingPoint() {
        CellularAutomaton automaton = life(
                BoundaryCondition.PERIODIC, 6, 6,
                cells(new int[] {0, 1}, new int[] {1, 2}, new int[] {2, 0}, new int[] {2, 1}, new int[] {2, 2}));
        int[] start = automaton.grid().snapshot();

        automaton.advance(24);

        assertArrayEquals(start, automaton.grid().snapshot());
        assertEquals(Regime.cycle(24), automaton.regime());
    }

    @Test
    void theRuleSeesTheGridOfThePreviousGenerationOnly() {
        // The rule of figure 1 of the subject: a cell lights up next to a lit cell. The lit area
        // must grow by exactly one cell per side and per step, never faster.
        Grid grid = new Grid(Shape.of(9));
        grid.setState(Coordinates.of(4), Cell.ON);
        CellularAutomaton automaton = new CellularAutomaton(
                grid, ExpressionRule.compile("SUPEQ(COMPTER(G2), 1)", new NeighborhoodCatalog(1)));

        automaton.step();
        assertArrayEquals(new int[] {0, 0, 0, 1, 1, 1, 0, 0, 0}, grid.snapshot());
        automaton.step();
        assertArrayEquals(new int[] {0, 0, 1, 1, 1, 1, 1, 0, 0}, grid.snapshot());

        automaton.advance(10);
        assertEquals(9, grid.population());
        assertEquals(Regime.FIXED_POINT, automaton.regime());
    }

    @Test
    void anyRuleImplementationCanDriveAnAutomaton() {
        Rule invert = (grid, cell) -> 1 - cell.state();
        CellularAutomaton automaton = new CellularAutomaton(new Grid(Shape.of(3)), invert);

        automaton.step();
        assertEquals(3, automaton.grid().population());
        automaton.step();
        assertEquals(0, automaton.grid().population());
        assertEquals(Regime.cycle(2), automaton.regime());
    }

    @Test
    void restartingRewindsTheClockAndForgetsTheTrajectory() {
        CellularAutomaton automaton = life(
                BoundaryCondition.FIXED, 5, 5, cells(new int[] {2, 1}, new int[] {2, 2}, new int[] {2, 3}));
        automaton.advance(3);

        automaton.restart(grid -> {
            grid.clear();
            grid.setState(Coordinates.of(0, 0), Cell.ON);
        });

        assertEquals(0, automaton.generation());
        assertEquals(Regime.TRANSIENT, automaton.regime());
        assertEquals(1, automaton.grid().population());
    }

    @Test
    void editingTheGridInvalidatesTheObservedRegime() {
        CellularAutomaton automaton = life(BoundaryCondition.FIXED, 5, 5);
        automaton.step();
        assertEquals(Regime.FIXED_POINT, automaton.regime());

        automaton.grid().setState(Coordinates.of(2, 2), Cell.ON);
        automaton.gridEdited();

        assertEquals(Regime.TRANSIENT, automaton.regime());
    }

    @Test
    void rejectsNonsense() {
        CellularAutomaton automaton = life(BoundaryCondition.FIXED, 3, 3);

        assertThrows(IllegalArgumentException.class, () -> automaton.advance(-1));
        assertThrows(IllegalArgumentException.class, () -> Regime.cycle(1));
        assertThrows(NullPointerException.class, () -> new CellularAutomaton(null, (grid, cell) -> 0));
    }
}
