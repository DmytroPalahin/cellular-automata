package fr.uspn.cellula.neighborhood;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.uspn.cellula.grid.Coordinates;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class NeighborhoodCatalogTest {

    @ParameterizedTest(name = "{1} has {2} cells in dimension {0}")
    @CsvSource({
        "1, G0, 1", "1, G2, 3", "1, G2*, 2",
        "2, G0, 1", "2, G4, 5", "2, G4*, 4", "2, G8, 9", "2, G8*, 8",
        "3, G0, 1", "3, G6, 7", "3, G6*, 6", "3, G26, 27", "3, G26*, 26",
        "4, G8, 9", "4, G8*, 8", "4, G80, 81", "4, G80*, 80",
    })
    void resolvesThePredefinedNeighborhoodsOfEachDimension(int dimension, String name, int size) {
        Neighborhood neighborhood = new NeighborhoodCatalog(dimension).resolve(name);

        assertEquals(name, neighborhood.name());
        assertEquals(size, neighborhood.size());
        assertEquals(dimension, neighborhood.dimension());
    }

    @ParameterizedTest(name = "{1} does not exist in dimension {0}")
    @CsvSource({"1, G4", "1, G8", "2, G2", "2, G6", "2, G26*", "3, G4", "3, G8*", "2, G0*", "2, G5", "2, H8", "2, g8"})
    void predefinedNeighborhoodsOnlyExistInTheirDimension(int dimension, String name) {
        NeighborhoodCatalog catalog = new NeighborhoodCatalog(dimension);

        assertTrue(catalog.find(name).isEmpty());
        UnknownNeighborhoodException error =
                assertThrows(UnknownNeighborhoodException.class, () -> catalog.resolve(name));
        assertEquals(name, error.name());
        assertTrue(error.getMessage().contains("available: G0"), error.getMessage());
    }

    @Test
    void theSameNameMeansDifferentThingsInDifferentDimensions() {
        // G8 is the full neighborhood in 2D but only the face neighbors in 4D.
        assertEquals(Neighborhoods.moore(2), new NeighborhoodCatalog(2).resolve("G8"));
        assertEquals(Neighborhoods.vonNeumann(4), new NeighborhoodCatalog(4).resolve("G8"));
    }

    @Test
    void resolvingTwiceYieldsTheSameInstance() {
        NeighborhoodCatalog catalog = new NeighborhoodCatalog(3);

        assertSame(catalog.resolve("G26*"), catalog.resolve("G26*"));
    }

    @Test
    void listsTheAvailableNames() {
        assertEquals(List.of("G0", "G2", "G2*"), new NeighborhoodCatalog(1).names());
        assertEquals(List.of("G0", "G4", "G4*", "G8", "G8*"), new NeighborhoodCatalog(2).names());
        assertEquals(List.of("G0", "G6", "G6*", "G26", "G26*"), new NeighborhoodCatalog(3).names());

        NeighborhoodCatalog catalog = new NeighborhoodCatalog(2);
        catalog.define("G12", List.of(Coordinates.of(0, 2)));
        assertEquals(List.of("G0", "G4", "G4*", "G8", "G8*", "G12"), catalog.names());
    }

    @Test
    void acceptsUserDefinedNeighborhoods() {
        NeighborhoodCatalog catalog = new NeighborhoodCatalog(3);

        // The example of the subject: G3 = {(0, 0, 0), (1, 1, 0), (-1, 0, 0)}.
        Neighborhood g3 = catalog.define(
                "G3", List.of(Coordinates.of(0, 0, 0), Coordinates.of(1, 1, 0), Coordinates.of(-1, 0, 0)));

        assertSame(g3, catalog.resolve("G3"));
        assertEquals(3, g3.size());
    }

    @Test
    void aUserDefinitionTakesPrecedenceOverAPredefinedName() {
        NeighborhoodCatalog catalog = new NeighborhoodCatalog(2);

        catalog.define("G4*", List.of(Coordinates.of(-2, 0), Coordinates.of(2, 0)));

        assertEquals(2, catalog.resolve("G4*").size());
        assertEquals(5, catalog.resolve("G4").size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"N4", "G", "g4", "G4**", "G-1", "G 4", "G4a", ""})
    void rejectsNamesThatBreakTheConvention(String name) {
        assertFalse(NeighborhoodCatalog.isValidName(name));
        assertThrows(
                IllegalArgumentException.class,
                () -> new NeighborhoodCatalog(1).define(name, List.of(Coordinates.of(1))));
    }

    @Test
    void rejectsInconsistentUserDefinitions() {
        NeighborhoodCatalog catalog = new NeighborhoodCatalog(2);
        catalog.define("G1", List.of(Coordinates.of(0, 1)));

        assertThrows(
                IllegalArgumentException.class,
                () -> catalog.define("G1", List.of(Coordinates.of(1, 0))),
                "defined twice");
        assertThrows(
                IllegalArgumentException.class,
                () -> catalog.define("G2", List.of(Coordinates.of(1, 0, 0))),
                "wrong dimension");
        assertThrows(
                IllegalArgumentException.class, () -> catalog.define("G3", List.of()), "no offset");
        assertThrows(IllegalArgumentException.class, () -> new NeighborhoodCatalog(0));
    }

    @Test
    void hugeDimensionsDoNotBreakTheCatalog() {
        NeighborhoodCatalog catalog = new NeighborhoodCatalog(40);

        assertEquals(81, catalog.resolve("G80").size());
        assertEquals(List.of("G0", "G80", "G80*"), catalog.names());
    }
}
