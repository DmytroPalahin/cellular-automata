package fr.uspn.cellula.explore;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.uspn.cellula.automaton.CellularAutomaton;
import fr.uspn.cellula.config.AutomatonDefinition;
import fr.uspn.cellula.config.AutomatonFactory;
import fr.uspn.cellula.config.InitialState;
import fr.uspn.cellula.grid.Coordinates;
import java.util.List;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TableRuleTest {

    private static byte[] row(int... cells) {
        byte[] row = new byte[cells.length];
        for (int index = 0; index < cells.length; index++) {
            row[index] = (byte) cells[index];
        }
        return row;
    }

    @Test
    void followsWolframNumbering() {
        TableRule rule18 = TableRule.of(1, 18);

        // 18 = 00010010b: only the neighborhoods 100 and 001 give a live cell.
        for (int neighborhood = 0; neighborhood < 8; neighborhood++) {
            assertEquals(neighborhood == 0b100 || neighborhood == 0b001 ? 1 : 0, rule18.output(neighborhood));
        }
        assertEquals(3, rule18.span());
        assertEquals(5, TableRule.of(2, 0).span());
    }

    @Test
    void stepsARowWithCellsBeyondTheEdgesOff() {
        assertArrayEquals(row(0, 1, 0, 1, 0), TableRule.of(1, 18).step(row(0, 0, 1, 0, 0)));
        assertArrayEquals(row(1, 1, 1, 1, 1), TableRule.of(1, 254).step(row(1, 0, 0, 1, 0)));
        // Rule 2 moves a lone cell to the left; at the left edge it falls off the row.
        assertArrayEquals(row(1, 0, 0), TableRule.of(1, 2).step(row(0, 1, 0)));
        assertArrayEquals(row(0, 0, 0), TableRule.of(1, 2).step(row(1, 0, 0)));
        assertArrayEquals(row(1), TableRule.of(1, 4).step(row(1)));
    }

    @Test
    void aRadiusTwoRuleReachesTwoCellsAway() {
        // Code 2 = only the neighborhood 00001: a cell lights up when the cell two steps to its
        // right is the only live one, so a lone cell jumps two cells to the left.
        assertArrayEquals(row(0, 1, 0, 0, 0, 0), TableRule.of(2, 2).step(row(0, 0, 0, 1, 0, 0)));
    }

    @Test
    void knowsWhichCellsMatter() {
        TableRule rule90 = TableRule.of(1, 90); // left XOR right
        assertTrue(rule90.dependsOn(-1));
        assertFalse(rule90.dependsOn(0));
        assertTrue(rule90.dependsOn(1));

        TableRule parity = TableRule.totalistic(2, 0b101010);
        assertFalse(parity.fitsRadius(1));
        assertTrue(parity.fitsRadius(2));

        // Rule 90 written as a radius-2 table that ignores the two outer cells.
        long disguised = 0;
        for (int neighborhood = 0; neighborhood < 32; neighborhood++) {
            if (rule90.output(neighborhood >> 1 & 0b111) == 1) {
                disguised |= 1L << neighborhood;
            }
        }
        assertTrue(TableRule.of(2, disguised).fitsRadius(1));
    }

    @Test
    void recognizesTotalisticRules() {
        assertEquals(OptionalInt.of(0b101010), TableRule.totalistic(2, 0b101010).totalisticCode());
        assertEquals(OptionalInt.of(0b0110), TableRule.of(1, 126).totalisticCode());
        assertEquals(OptionalInt.empty(), TableRule.of(1, 30).totalisticCode());
    }

    @Test
    void translatesTotalisticRulesIntoTestsOnTheCount() {
        assertEquals("EQ(COMPTER(G4), 1)", TableRule.totalistic(2, 0b000010).toExpression());
        assertEquals(
                "OU(EQ(COMPTER(G4), 1), OU(EQ(COMPTER(G4), 3), EQ(COMPTER(G4), 5)))",
                TableRule.totalistic(2, 0b101010).toExpression());
        assertEquals("0", TableRule.of(2, 0).toExpression());
    }

    @Test
    void translatesOtherRulesIntoDecisionTrees() {
        assertEquals("SI(COMPTER(G11), NON(COMPTER(G12)), COMPTER(G12))", TableRule.of(1, 90).toExpression());
        assertEquals("COMPTER(G11)", TableRule.of(1, 240).toExpression());
        assertEquals("NON(COMPTER(G0))", TableRule.of(1, 51).toExpression());
    }

    @Test
    void declaresOnlyTheNeighborhoodsItsExpressionUses() {
        InitialState empty = new InitialState.ExplicitCells(List.of());

        assertEquals(
                List.of("G4"),
                List.copyOf(TableRule.totalistic(2, 2).toDefinition("t", "", 9, empty).neighborhoods().keySet()));
        assertEquals(
                List.of("G11", "G12"),
                List.copyOf(TableRule.of(1, 90).toDefinition("t", "", 9, empty).neighborhoods().keySet()));
        assertEquals(
                List.of(Coordinates.of(-2)),
                TableRule.of(2, 0xFFFF0000L).toDefinition("t", "", 9, empty).neighborhoods().get("G11"));
        assertTrue(TableRule.of(1, 126).toDefinition("t", "", 9, empty).neighborhoods().isEmpty());
    }

    /**
     * The key property of the translation: a table rule and the definition generated from it are
     * the same automaton. The fast simulator and the general engine are run side by side from the
     * same random row and must agree on every cell of every generation.
     */
    @ParameterizedTest
    @ValueSource(longs = {18, 30, 90, 110, 150, 254})
    void radiusOneRulesBehaveIdenticallyOnTheGeneralEngine(long code) {
        assertSameEvolution(TableRule.of(1, code));
    }

    @Test
    void radiusTwoRulesBehaveIdenticallyOnTheGeneralEngine() {
        Random random = new Random(2024);
        for (int sample = 0; sample < 25; sample++) {
            assertSameEvolution(TableRule.of(2, random.nextLong() & 0xFFFF_FFFFL));
        }
        for (int sumCode = 0; sumCode < 64; sumCode += 7) {
            assertSameEvolution(TableRule.totalistic(2, sumCode));
        }
    }

    private static void assertSameEvolution(TableRule rule) {
        int width = 40;
        AutomatonDefinition definition = rule.toDefinition(
                "check", "", width, new InitialState.RandomFill(50, OptionalLong.of(rule.code())));
        CellularAutomaton automaton = AutomatonFactory.create(definition);
        byte[] cells = new byte[width];
        int[] initial = automaton.grid().snapshot();
        for (int index = 0; index < width; index++) {
            cells[index] = (byte) initial[index];
        }
        for (int generation = 1; generation <= 12; generation++) {
            automaton.step();
            cells = rule.step(cells);
            int[] expected = new int[width];
            for (int index = 0; index < width; index++) {
                expected[index] = cells[index];
            }
            assertArrayEquals(
                    expected,
                    automaton.grid().snapshot(),
                    rule + " as " + definition.rule() + ", generation " + generation);
        }
    }

    @Test
    void rejectsInvalidRules() {
        assertThrows(IllegalArgumentException.class, () -> TableRule.of(0, 1));
        assertThrows(IllegalArgumentException.class, () -> TableRule.of(3, 1));
        assertThrows(IllegalArgumentException.class, () -> TableRule.of(1, 256));
        assertThrows(IllegalArgumentException.class, () -> TableRule.of(1, -1));
        assertThrows(IllegalArgumentException.class, () -> TableRule.of(2, 1L << 32));
        assertThrows(IllegalArgumentException.class, () -> TableRule.totalistic(2, 64));
        assertEquals(TableRule.of(2, 0xFFFF_FFFFL), TableRule.totalistic(2, 63));
    }
}
