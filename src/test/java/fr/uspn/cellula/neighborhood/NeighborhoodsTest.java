package fr.uspn.cellula.neighborhood;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.uspn.cellula.grid.Coordinates;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class NeighborhoodsTest {

    @Test
    void g0IsTheCellAlone() {
        OffsetNeighborhood g0 = Neighborhoods.self(3);

        assertEquals("G0", g0.name());
        assertEquals(List.of(Coordinates.origin(3)), g0.offsets());
    }

    @Test
    void g2IsTheCellAndItsLeftAndRightNeighbors() {
        OffsetNeighborhood g2 = Neighborhoods.vonNeumann(1);

        assertEquals("G2", g2.name());
        assertEquals(List.of(Coordinates.of(-1), Coordinates.of(0), Coordinates.of(1)), g2.offsets());
        assertEquals(g2.offsets(), Neighborhoods.moore(1).offsets());
    }

    @Test
    void g4IsTheCellAndItsFourSideNeighbors() {
        OffsetNeighborhood g4 = Neighborhoods.vonNeumann(2);

        assertEquals("G4", g4.name());
        assertEquals(
                Set.of(
                        Coordinates.of(0, 0),
                        Coordinates.of(-1, 0),
                        Coordinates.of(1, 0),
                        Coordinates.of(0, -1),
                        Coordinates.of(0, 1)),
                Set.copyOf(g4.offsets()));
    }

    @Test
    void g8AddsTheFourDiagonalNeighbors() {
        OffsetNeighborhood g8 = Neighborhoods.moore(2);

        assertEquals("G8", g8.name());
        assertEquals(9, g8.size());
        assertTrue(g8.offsets().containsAll(Neighborhoods.vonNeumann(2).offsets()));
        assertTrue(g8.offsets().containsAll(List.of(
                Coordinates.of(-1, -1), Coordinates.of(-1, 1), Coordinates.of(1, -1), Coordinates.of(1, 1))));
    }

    @ParameterizedTest(name = "dimension {0}: G{1} and G{2}")
    @CsvSource({"1, 2, 2", "2, 4, 8", "3, 6, 26", "4, 8, 80", "5, 10, 242"})
    void neighborhoodsAreNamedAfterTheirNumberOfNeighbors(int dimension, int vonNeumann, int moore) {
        OffsetNeighborhood faces = Neighborhoods.vonNeumann(dimension);
        OffsetNeighborhood full = Neighborhoods.moore(dimension);

        assertEquals("G" + vonNeumann, faces.name());
        assertEquals(vonNeumann + 1, faces.size());
        assertEquals("G" + moore, full.name());
        assertEquals(moore + 1, full.size());
        assertEquals(1, faces.radius());
        assertEquals(1, full.radius());
        assertTrue(faces.includesCenter());
        assertTrue(full.includesCenter());
    }

    @Test
    void theStarredVariantLeavesOutTheCell() {
        OffsetNeighborhood g26 = Neighborhoods.moore(3);
        OffsetNeighborhood starred = Neighborhoods.punctured(g26);

        assertEquals("G26*", starred.name());
        assertEquals(26, starred.size());
        assertFalse(starred.includesCenter());
        assertTrue(g26.offsets().containsAll(starred.offsets()));
    }

    @Test
    void offsetsComeInADeterministicOrder() {
        assertEquals(
                List.of(
                        Coordinates.of(-1, -1), Coordinates.of(-1, 0), Coordinates.of(-1, 1),
                        Coordinates.of(0, -1), Coordinates.of(0, 0), Coordinates.of(0, 1),
                        Coordinates.of(1, -1), Coordinates.of(1, 0), Coordinates.of(1, 1)),
                Neighborhoods.moore(2).offsets());
    }

    @Test
    void refusesToMaterializeAstronomicalNeighborhoods() {
        assertThrows(IllegalArgumentException.class, () -> Neighborhoods.moore(20));
    }
}
