package fr.uspn.cellula.automaton;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import fr.uspn.cellula.grid.Cell;
import fr.uspn.cellula.grid.Coordinates;
import fr.uspn.cellula.grid.Grid;
import fr.uspn.cellula.grid.Shape;
import fr.uspn.cellula.neighborhood.NeighborhoodCatalog;
import fr.uspn.cellula.rule.ExpressionRule;
import org.junit.jupiter.api.Test;

/** Reproduces the two one-dimensional automata of section 1.1 of the subject. */
class ElementaryRulesTest {

    /** The formula of the subject for the Sierpiński triangle, transcribed with the operators. */
    private static final String SIERPINSKI = "MUL(EQ(COMPTER(G2*), 1), SUB(1, COMPTER(G0)))";

    private static int[][] run(String rule, int width, int steps) {
        Grid grid = new Grid(Shape.of(width));
        grid.setState(Coordinates.of(width / 2), Cell.ON);
        CellularAutomaton automaton =
                new CellularAutomaton(grid, ExpressionRule.compile(rule, new NeighborhoodCatalog(1)));
        int[][] rows = new int[steps + 1][];
        rows[0] = grid.snapshot();
        for (int step = 1; step <= steps; step++) {
            automaton.step();
            rows[step] = grid.snapshot();
        }
        return rows;
    }

    @Test
    void theSierpinskiRuleMatchesTheTruthTableOfFigure4() {
        // Figure 4: only the neighborhoods 001 and 100 give a live cell (Wolfram's rule 18).
        ExpressionRule rule = ExpressionRule.compile(SIERPINSKI, new NeighborhoodCatalog(1));
        for (int neighborhood = 0; neighborhood < 8; neighborhood++) {
            Grid grid = new Grid(Shape.of(3));
            grid.setState(Coordinates.of(0), neighborhood >> 2 & 1);
            grid.setState(Coordinates.of(1), neighborhood >> 1 & 1);
            grid.setState(Coordinates.of(2), neighborhood & 1);

            int expected = neighborhood == 0b001 || neighborhood == 0b100 ? 1 : 0;

            assertEquals(expected, rule.nextState(grid, grid.cell(1)), "neighborhood " + neighborhood);
        }
    }

    @Test
    void theSierpinskiRuleFollowsTheArithmeticFormulaOfTheSubject() {
        int[][] rows = run(SIERPINSKI, 65, 31);

        for (int time = 0; time < 31; time++) {
            for (int i = 0; i < 65; i++) {
                int left = i > 0 ? rows[time][i - 1] : 0;
                int right = i < 64 ? rows[time][i + 1] : 0;
                int expected = (Math.max(left, right) - Math.min(left, right)) * (1 - rows[time][i]);
                assertEquals(expected, rows[time + 1][i], "t = " + time + ", i = " + i);
            }
        }
    }

    @Test
    void theSierpinskiTriangleAppears() {
        int[][] rows = run(SIERPINSKI, 17, 7);

        String[] expected = {
            "........#........",
            ".......#.#.......",
            "......#...#......",
            ".....#.#.#.#.....",
            "....#.......#....",
            "...#.#.....#.#...",
            "..#...#...#...#..",
            ".#.#.#.#.#.#.#.#.",
        };
        for (int time = 0; time < expected.length; time++) {
            StringBuilder row = new StringBuilder();
            for (int state : rows[time]) {
                row.append(state == 1 ? '#' : '.');
            }
            assertEquals(expected[time], row.toString(), "t = " + time);
        }
    }

    @Test
    void theGrowthRuleReproducesFigure2() {
        // c(t+1, i) = max(c(t, i-1), c(t, i), c(t, i+1))
        int[][] rows = run("SUPEQ(COMPTER(G2), 1)", 19, 9);

        for (int time = 0; time <= 9; time++) {
            int[] expected = new int[19];
            for (int i = 9 - time; i <= 9 + time; i++) {
                expected[i] = 1;
            }
            assertArrayEquals(expected, rows[time], "t = " + time);
        }
    }
}
