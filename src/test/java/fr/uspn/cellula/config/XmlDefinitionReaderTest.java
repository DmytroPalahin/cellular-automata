package fr.uspn.cellula.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.uspn.cellula.config.InitialState.ExplicitCells;
import fr.uspn.cellula.config.InitialState.RandomFill;
import fr.uspn.cellula.grid.BoundaryCondition;
import fr.uspn.cellula.grid.Coordinates;
import fr.uspn.cellula.grid.Shape;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class XmlDefinitionReaderTest {

    private static AutomatonDefinition read(String xml) throws DefinitionException {
        return XmlDefinitionReader.read(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), "test.xml");
    }

    /** Wraps the given children in a root element, to keep the cases below short. */
    private static String automaton(String children) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<automaton name=\"Test\">\n" + children + "\n</automaton>";
    }

    private static final String MINIMAL = automaton("""
            <dimension>2</dimension>
            <size>8, 6</size>
            <rule>COMPTER(G0)</rule>
            <initial>RANDOM(15)</initial>""");

    @Test
    void readsAMinimalDefinitionAndFillsInTheDefaults() throws DefinitionException {
        AutomatonDefinition definition = read(MINIMAL);

        assertEquals("Test", definition.name());
        assertEquals("", definition.description());
        assertEquals(Shape.of(8, 6), definition.shape());
        assertEquals(BoundaryCondition.FIXED, definition.boundary());
        assertEquals("(:, :)", definition.slice().toString());
        assertEquals("COMPTER(G0)", definition.rule());
        assertEquals(new RandomFill(15, OptionalLong.empty()), definition.initialState());
        assertTrue(definition.neighborhoods().isEmpty());
        assertTrue(definition.colors().isEmpty());
        assertEquals("2D · 8 × 6 · fixed boundary", definition.summary());
    }

    @Test
    void readsEveryFeatureOfTheFormat() throws DefinitionException {
        AutomatonDefinition definition = read("""
                <?xml version="1.0" encoding="UTF-8"?>
                <!-- Elements may come in any order. -->
                <automaton name="  Everything  ">
                  <colors>
                    <state value="0" color="#102030"/>
                    <state value="1" color="#fFaA00"/>
                  </colors>
                  <initial seed="42">  random(30)  </initial>
                  <rule>
                    SI(COMPTER(G3),
                       1,
                       COMPTER(G26*))
                  </rule>
                  <neighborhoods>
                    <neighborhood name="G3">{(0, 0, 0), (1, 1, 0), (-1, 0, 0)}</neighborhood>
                    <neighborhood name="G1*">(0, 0, 2)</neighborhood>
                  </neighborhoods>
                  <slice>(37, :, :)</slice>
                  <boundary> periodic </boundary>
                  <size>(40 x 20 × 10)</size>
                  <dimension> 3 </dimension>
                  <description>
                    A description
                    on two lines.
                  </description>
                </automaton>""");

        assertEquals("Everything", definition.name());
        assertEquals("A description on two lines.", definition.description());
        assertEquals(Shape.of(40, 20, 10), definition.shape());
        assertEquals(BoundaryCondition.PERIODIC, definition.boundary());
        assertEquals("(37, :, :)", definition.slice().toString());
        assertEquals(
                Map.of(
                        "G3", List.of(Coordinates.of(0, 0, 0), Coordinates.of(1, 1, 0), Coordinates.of(-1, 0, 0)),
                        "G1*", List.of(Coordinates.of(0, 0, 2))),
                definition.neighborhoods());
        assertEquals("SI(COMPTER(G3),\n   1,\n   COMPTER(G26*))", definition.rule());
        assertEquals(new RandomFill(30, OptionalLong.of(42)), definition.initialState());
        assertEquals(Map.of(0, 0x102030, 1, 0xFFAA00), definition.colors());
    }

    @Test
    void readsExplicitInitialCells() throws DefinitionException {
        AutomatonDefinition definition = read(automaton("""
                <dimension>1</dimension>
                <size>257</size>
                <rule>COMPTER(G2*)</rule>
                <initial>(128)</initial>"""));

        assertEquals(new ExplicitCells(List.of(Coordinates.of(128))), definition.initialState());
        assertEquals("(:)", definition.slice().toString());
    }

    @Test
    void aGridOfThreeDimensionsOrMoreGetsACenteredSliceByDefault() throws DefinitionException {
        AutomatonDefinition definition = read(automaton("""
                <dimension>4</dimension>
                <size>6, 6, 10, 20</size>
                <rule>COMPTER(G8)</rule>
                <initial></initial>"""));

        assertEquals("(:, :, 5, 10)", definition.slice().toString());
    }

    static Stream<Arguments> invalidDocuments() {
        return Stream.of(
                Arguments.of("not XML at all", "this is not xml", "line 1"),
                Arguments.of("unclosed element", "<automaton name=\"x\"><dimension>1</dimension>", "line 1"),
                Arguments.of("wrong root element", "<project/>", "project"),
                Arguments.of("missing name", "<automaton><dimension>1</dimension><size>3</size><rule>1</rule><initial/></automaton>", "name"),
                Arguments.of(
                        "missing rule",
                        automaton("<dimension>1</dimension><size>3</size><initial/>"),
                        "rule"),
                Arguments.of(
                        "unknown element",
                        automaton("<dimension>1</dimension><size>3</size><rule>1</rule><initial/><colour/>"),
                        "colour"),
                Arguments.of(
                        "duplicated element",
                        automaton("<dimension>1</dimension><dimension>1</dimension><size>3</size><rule>1</rule><initial/>"),
                        "dimension"),
                Arguments.of(
                        "dimension is not a number",
                        automaton("<dimension>two</dimension><size>3, 3</size><rule>1</rule><initial/>"),
                        "two"),
                Arguments.of(
                        "dimension is zero",
                        automaton("<dimension>0</dimension><size>3</size><rule>1</rule><initial/>"),
                        "line 3"),
                Arguments.of(
                        "unknown boundary",
                        automaton("<dimension>1</dimension><size>3</size><boundary>mirror</boundary><rule>1</rule><initial/>"),
                        "mirror"),
                Arguments.of(
                        "malformed color",
                        automaton("<dimension>1</dimension><size>3</size><rule>1</rule><initial/>"
                                + "<colors><state value=\"0\" color=\"blue\"/></colors>"),
                        "blue"),
                Arguments.of(
                        "malformed neighborhood name",
                        automaton("<dimension>1</dimension><size>3</size><rule>1</rule><initial/>"
                                + "<neighborhoods><neighborhood name=\"left\">(-1)</neighborhood></neighborhoods>"),
                        "left"),
                Arguments.of(
                        "number of sizes differs from the dimension",
                        automaton("<dimension>3</dimension><size>4, 4</size><rule>1</rule><initial/>"),
                        "the size lists 2 value(s) but the grid has 3 dimension(s)"),
                Arguments.of(
                        "size is not a number",
                        automaton("<dimension>1</dimension><size>big</size><rule>1</rule><initial/>"),
                        "'big' is not an integer"),
                Arguments.of(
                        "size is not positive",
                        automaton("<dimension>2</dimension><size>4, 0</size><rule>1</rule><initial/>"),
                        "must be positive"),
                Arguments.of(
                        "grid too large",
                        automaton("<dimension>3</dimension><size>1000, 1000, 1000</size><rule>1</rule><initial/>"),
                        "too large"),
                Arguments.of(
                        "slice of the wrong dimension",
                        automaton("<dimension>3</dimension><size>4, 4, 4</size><slice>(:, :)</slice><rule>1</rule><initial/>"),
                        "slice (:, :) has 2 coordinate(s) but the grid has 3 dimension(s)"),
                Arguments.of(
                        "slice outside the grid",
                        automaton("<dimension>3</dimension><size>4, 4, 4</size><slice>(4, :, :)</slice><rule>1</rule><initial/>"),
                        "slice (4, :, :) is outside the grid"),
                Arguments.of(
                        "slice without free axes",
                        automaton("<dimension>3</dimension><size>4, 4, 4</size><slice>(1, 2, 3)</slice><rule>1</rule><initial/>"),
                        "exactly 2 free axis(es)"),
                Arguments.of(
                        "neighborhood of the wrong dimension",
                        automaton("<dimension>2</dimension><size>4, 4</size><rule>COMPTER(G1)</rule><initial/>"
                                + "<neighborhoods><neighborhood name=\"G1\">(1, 0, 0)</neighborhood></neighborhoods>"),
                        "neighborhood G1: offset (1, 0, 0) has 3 coordinate(s) but the grid has 2 dimension(s)"),
                Arguments.of(
                        "malformed neighborhood offsets",
                        automaton("<dimension>2</dimension><size>4, 4</size><rule>1</rule><initial/>"
                                + "<neighborhoods><neighborhood name=\"G1\">(1, 0</neighborhood></neighborhoods>"),
                        "neighborhood G1: invalid coordinate list"),
                Arguments.of(
                        "neighborhood defined twice",
                        automaton("<dimension>1</dimension><size>4</size><rule>1</rule><initial/><neighborhoods>"
                                + "<neighborhood name=\"G1\">(1)</neighborhood><neighborhood name=\"G1\">(2)</neighborhood>"
                                + "</neighborhoods>"),
                        "neighborhood G1 is defined twice"),
                Arguments.of(
                        "syntax error in the rule",
                        automaton("<dimension>2</dimension><size>4, 4</size><rule>SI(1, 2)</rule><initial/>"),
                        "SI expects 3 argument(s) but got 2"),
                Arguments.of(
                        "rule using a neighborhood of another dimension",
                        automaton("<dimension>2</dimension><size>4, 4</size><rule>COMPTER(G26)</rule><initial/>"),
                        "unknown neighborhood 'G26' for a 2-dimensional grid"),
                Arguments.of(
                        "random percentage out of range",
                        automaton("<dimension>1</dimension><size>4</size><rule>1</rule><initial>RANDOM(150)</initial>"),
                        "RANDOM(k) needs k between 0 and 100, got 150"),
                Arguments.of(
                        "initial cell outside the grid",
                        automaton("<dimension>2</dimension><size>4, 4</size><rule>1</rule><initial>(1, 7)</initial>"),
                        "initial cell (1, 7) is outside the 4 × 4 grid"),
                Arguments.of(
                        "initial cell of the wrong dimension",
                        automaton("<dimension>2</dimension><size>4, 4</size><rule>1</rule><initial>(1, 1, 1)</initial>"),
                        "initial cell (1, 1, 1) has 3 coordinate(s)"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidDocuments")
    void explainsWhatIsWrongWithAnInvalidDocument(String label, String xml, String expectedFragment) {
        DefinitionException error = assertThrows(DefinitionException.class, () -> read(xml));

        assertTrue(error.getMessage().startsWith("test.xml: "), error.getMessage());
        assertTrue(error.getMessage().contains(expectedFragment), error.getMessage());
    }

    @Test
    void refusesDocumentTypeDeclarations() {
        // The classic XML external entity attack must not even be parsed.
        String xml = """
                <?xml version="1.0"?>
                <!DOCTYPE automaton [<!ENTITY secret SYSTEM "file:///etc/passwd">]>
                <automaton name="&secret;">
                  <dimension>1</dimension><size>3</size><rule>1</rule><initial/>
                </automaton>""";

        DefinitionException error = assertThrows(DefinitionException.class, () -> read(xml));

        assertTrue(error.getMessage().contains("DOCTYPE"), error.getMessage());
    }

    @Test
    void readsFilesAndReportsUnreadableOnes(@TempDir Path directory) throws IOException, DefinitionException {
        Path file = directory.resolve("life.xml");
        Files.writeString(file, MINIMAL);

        assertEquals("Test", XmlDefinitionReader.read(file).name());

        DefinitionException missing = assertThrows(
                DefinitionException.class, () -> XmlDefinitionReader.read(directory.resolve("nope.xml")));
        assertTrue(missing.getMessage().startsWith("nope.xml: cannot read the file"), missing.getMessage());
    }

    @Test
    void recognizesDefinitionFilesByTheirRootElement(@TempDir Path directory) throws IOException {
        Path definition = Files.writeString(directory.resolve("a.xml"), MINIMAL);
        Path broken = Files.writeString(directory.resolve("b.xml"), "<automaton name=\"x\"><oops>");
        Path pom = Files.writeString(directory.resolve("pom.xml"), "<project><name>x</name></project>");
        Path text = Files.writeString(directory.resolve("notes.xml"), "just some notes");

        assertTrue(XmlDefinitionReader.looksLikeDefinition(definition));
        assertTrue(XmlDefinitionReader.looksLikeDefinition(broken));
        assertFalse(XmlDefinitionReader.looksLikeDefinition(pom));
        assertFalse(XmlDefinitionReader.looksLikeDefinition(text));
        assertFalse(XmlDefinitionReader.looksLikeDefinition(directory.resolve("missing.xml")));
    }
}
