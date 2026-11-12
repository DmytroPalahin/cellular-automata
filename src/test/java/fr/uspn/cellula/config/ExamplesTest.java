package fr.uspn.cellula.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.uspn.cellula.automaton.CellularAutomaton;
import fr.uspn.cellula.config.DefinitionLibrary.Entry;
import fr.uspn.cellula.grid.Frame;
import fr.uspn.cellula.simulation.Projection;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.OptionalLong;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Guards the definition files shipped in the {@code examples} directory. */
class ExamplesTest {

    private static final Path EXAMPLES = Path.of("examples");

    static Stream<Entry> examples() throws IOException {
        return DefinitionLibrary.scan(EXAMPLES).stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("examples")
    void everyExampleIsValidAndRuns(Entry entry) {
        Entry.Loaded loaded = assertInstanceOf(Entry.Loaded.class, entry, () -> entry.toString());
        AutomatonDefinition definition = loaded.definition();
        assertFalse(definition.description().isBlank(), "examples document themselves");

        CellularAutomaton automaton = AutomatonFactory.create(definition);
        Projection projection = Projection.of(definition);
        projection.restarted(automaton.grid());
        automaton.step();
        projection.advanced(automaton.grid());
        Frame frame = projection.frame(automaton.grid());

        assertEquals(1, automaton.generation());
        assertTrue(frame.rows() > 0 && frame.columns() > 0);
    }

    @Test
    void theTwoFilesRequiredByTheSubjectArePresent() throws IOException {
        List<String> names = DefinitionLibrary.scan(EXAMPLES).stream().map(Entry::fileName).toList();

        assertTrue(names.contains("game-of-life.xml"), names.toString());
        assertTrue(names.contains("sierpinski.xml"), names.toString());
    }

    @Test
    void theGameOfLifeStartsFromFifteenPercentOfRandomCells() throws DefinitionException {
        AutomatonDefinition life = XmlDefinitionReader.read(EXAMPLES.resolve("game-of-life.xml"));

        assertEquals(new InitialState.RandomFill(15, OptionalLong.empty()), life.initialState());
        assertEquals(2, life.dimension());
    }
}
