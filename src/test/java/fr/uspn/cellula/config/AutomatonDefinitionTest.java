package fr.uspn.cellula.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import fr.uspn.cellula.automaton.CellularAutomaton;
import fr.uspn.cellula.grid.BoundaryCondition;
import fr.uspn.cellula.grid.Coordinates;
import fr.uspn.cellula.grid.Slice;
import fr.uspn.cellula.rule.parser.RuleSyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AutomatonDefinitionTest {

    private static AutomatonDefinition.Builder valid() {
        return AutomatonDefinition.builder().name("Test").size(6, 6).rule("COMPTER(G0)");
    }

    @Test
    void theBuilderFillsInSensibleDefaults() {
        AutomatonDefinition definition = valid().build();

        assertEquals(BoundaryCondition.FIXED, definition.boundary());
        assertEquals(Slice.parse("(:, :)"), definition.slice());
        assertEquals(new InitialState.ExplicitCells(List.of()), definition.initialState());
        assertEquals(2, definition.dimension());
    }

    @Test
    void mandatoryPartsAreMandatory() {
        assertThrows(IllegalArgumentException.class, () -> valid().name(null).build());
        assertThrows(IllegalArgumentException.class, () -> valid().name("  ").build());
        assertThrows(IllegalArgumentException.class, () -> valid().rule(null).build());
        assertThrows(IllegalArgumentException.class, () -> valid().rule(" ").build());
        assertThrows(IllegalArgumentException.class, () -> AutomatonDefinition.builder().name("x").rule("1").build());
    }

    @Test
    void thePartsMustBeConsistentWithEachOther() {
        assertThrows(IllegalArgumentException.class, () -> valid().slice(Slice.parse("(1, :, :)")).build());
        assertThrows(
                IllegalArgumentException.class,
                () -> valid().initialState(new InitialState.ExplicitCells(List.of(Coordinates.of(6, 6)))).build());
        assertThrows(
                IllegalArgumentException.class,
                () -> valid().neighborhood("G1", List.of(Coordinates.of(0, 1)))
                        .neighborhood("G1", List.of(Coordinates.of(1, 0))));
    }

    @Test
    void aDefinitionIsImmutable() {
        List<Coordinates> offsets = new ArrayList<>(List.of(Coordinates.of(0, 1)));
        AutomatonDefinition definition = valid().neighborhood("G1", offsets).color(1, 0xFF0000).build();

        offsets.add(Coordinates.of(1, 0));

        assertEquals(List.of(Coordinates.of(0, 1)), definition.neighborhoods().get("G1"));
        assertThrows(UnsupportedOperationException.class, () -> definition.neighborhoods().clear());
        assertThrows(UnsupportedOperationException.class, () -> definition.colors().put(2, 0));
    }

    @Test
    void theFactoryInstantiatesIndependentAutomata() {
        AutomatonDefinition definition = valid()
                .rule("NON(COMPTER(G0))")
                .initialState(new InitialState.ExplicitCells(List.of(Coordinates.of(2, 3))))
                .build();

        CellularAutomaton first = AutomatonFactory.create(definition);
        CellularAutomaton second = AutomatonFactory.create(definition);
        first.step();

        assertEquals(35, first.grid().population());
        assertEquals(1, second.grid().population());
        assertEquals(0, second.generation());
    }

    @Test
    void theFactoryWiresUserDefinedNeighborhoodsIntoTheRule() {
        // Each cell copies the cell two columns to its right.
        AutomatonDefinition definition = AutomatonDefinition.builder()
                .name("Shift")
                .size(1, 6)
                .neighborhood("G1", List.of(Coordinates.of(0, 2)))
                .rule("COMPTER(G1)")
                .initialState(new InitialState.ExplicitCells(List.of(Coordinates.of(0, 5))))
                .build();

        CellularAutomaton automaton = AutomatonFactory.create(definition);
        automaton.step();

        assertEquals(1, automaton.grid().cell(0, 3).state());
        assertEquals(1, automaton.grid().population());
    }

    @Test
    void theFactoryRejectsRulesThatDoNotCompile() {
        assertThrows(RuleSyntaxException.class, () -> AutomatonFactory.create(valid().rule("COMPTER(G6)").build()));
        assertThrows(
                IllegalArgumentException.class,
                () -> AutomatonFactory.compileRule(valid().neighborhood("left", List.of(Coordinates.of(0, -1))).build()));
    }
}
