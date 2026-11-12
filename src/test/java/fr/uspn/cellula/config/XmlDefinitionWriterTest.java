package fr.uspn.cellula.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.uspn.cellula.grid.BoundaryCondition;
import fr.uspn.cellula.grid.Coordinates;
import fr.uspn.cellula.grid.Slice;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class XmlDefinitionWriterTest {

    private static AutomatonDefinition reread(AutomatonDefinition definition) throws DefinitionException {
        String xml = XmlDefinitionWriter.toXml(definition);
        return XmlDefinitionReader.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), "memory");
    }

    @Test
    void aMinimalDefinitionSurvivesARoundTrip() throws DefinitionException {
        AutomatonDefinition definition = AutomatonDefinition.builder()
                .name("Minimal")
                .size(12)
                .rule("COMPTER(G2*)")
                .build();

        assertEquals(definition, reread(definition));
    }

    @Test
    void aFullDefinitionSurvivesARoundTrip() throws DefinitionException {
        AutomatonDefinition definition = AutomatonDefinition.builder()
                .name("Full <&> \"quoted\"")
                .description("Uses every feature; even 1 < 2 & 3 > 2.")
                .size(9, 8, 7)
                .boundary(BoundaryCondition.PERIODIC)
                .slice(Slice.parse("(:, 3, :)"))
                .neighborhood("G3", List.of(Coordinates.of(0, 0, 0), Coordinates.of(1, 1, 0), Coordinates.of(-1, 0, 0)))
                .neighborhood("G1*", List.of(Coordinates.of(0, 0, 2)))
                .rule("SI(SUP(COMPTER(G3), 1),\n   COMPTER(G1*),\n   ETAT())")
                .initialState(new InitialState.RandomFill(40, OptionalLong.of(-12345)))
                .color(0, 0x000000)
                .color(2, 0xABCDEF)
                .build();

        assertEquals(definition, reread(definition));
    }

    @Test
    void explicitCellsSurviveARoundTrip() throws DefinitionException {
        AutomatonDefinition definition = AutomatonDefinition.builder()
                .name("Cells")
                .size(5, 5)
                .rule("COMPTER(G8*)")
                .initialState(new InitialState.ExplicitCells(List.of(Coordinates.of(0, 1), Coordinates.of(4, 4))))
                .build();

        assertEquals(definition, reread(definition));
    }

    @Test
    void theSliceIsOnlyWrittenWhenItCarriesInformation() {
        String flat = XmlDefinitionWriter.toXml(
                AutomatonDefinition.builder().name("Flat").size(4, 4).rule("1").build());
        String solid = XmlDefinitionWriter.toXml(
                AutomatonDefinition.builder().name("Solid").size(4, 4, 4).rule("1").build());

        assertFalse(flat.contains("<slice>"));
        assertTrue(solid.contains("<slice>(:, :, 2)</slice>"), solid);
    }

    @Test
    void writesFilesCreatingTheDirectories(@TempDir Path directory) throws IOException, DefinitionException {
        AutomatonDefinition definition =
                AutomatonDefinition.builder().name("On disk").size(3).rule("ETAT()").build();
        Path file = directory.resolve("deep/er/file.xml");

        XmlDefinitionWriter.write(definition, file);

        assertEquals(definition, XmlDefinitionReader.read(file));
    }
}
