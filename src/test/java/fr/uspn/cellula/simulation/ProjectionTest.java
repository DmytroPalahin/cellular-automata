package fr.uspn.cellula.simulation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import fr.uspn.cellula.config.AutomatonDefinition;
import fr.uspn.cellula.grid.Cell;
import fr.uspn.cellula.grid.Coordinates;
import fr.uspn.cellula.grid.Frame;
import fr.uspn.cellula.grid.Grid;
import fr.uspn.cellula.grid.Shape;
import fr.uspn.cellula.grid.Slice;
import org.junit.jupiter.api.Test;

class ProjectionTest {

    private static Grid row(int... states) {
        Grid grid = new Grid(Shape.of(states.length));
        grid.fill(position -> states[position.get(0)]);
        return grid;
    }

    @Test
    void oneDimensionalAutomataAreShownAsSpaceTimeDiagrams() {
        AutomatonDefinition line = AutomatonDefinition.builder().name("1D").size(9).rule("1").build();
        AutomatonDefinition plane = AutomatonDefinition.builder().name("2D").size(9, 9).rule("1").build();
        AutomatonDefinition volume = AutomatonDefinition.builder().name("3D").size(9, 9, 9).rule("1").build();

        assertInstanceOf(SpaceTimeProjection.class, Projection.of(line));
        assertInstanceOf(PlaneProjection.class, Projection.of(plane));
        assertEquals(Slice.parse("(:, :, 4)"), ((PlaneProjection) Projection.of(volume)).slice());
    }

    @Test
    void aSpaceTimeDiagramStacksTheGenerations() {
        SpaceTimeProjection projection = new SpaceTimeProjection();
        projection.restarted(row(0, 1, 0));
        projection.advanced(row(1, 1, 1));
        projection.advanced(row(1, 0, 1));

        Frame frame = projection.frame(row(1, 0, 1));

        assertEquals(3, frame.rows());
        assertEquals(3, frame.columns());
        assertArrayEquals(new int[] {0, 1, 0, 1, 1, 1, 1, 0, 1}, frame.states());
    }

    @Test
    void aSpaceTimeDiagramScrollsWhenItsHistoryIsFull() {
        SpaceTimeProjection projection = new SpaceTimeProjection(2);
        projection.restarted(row(1, 0));
        projection.advanced(row(2, 0));
        projection.advanced(row(3, 0));

        assertArrayEquals(new int[] {2, 0, 3, 0}, projection.frame(row(3, 0)).states());
    }

    @Test
    void restartingClearsTheHistory() {
        SpaceTimeProjection projection = new SpaceTimeProjection();
        projection.restarted(row(1, 1));
        projection.advanced(row(0, 0));

        projection.restarted(row(0, 1));

        assertArrayEquals(new int[] {0, 1}, projection.frame(row(0, 1)).states());
    }

    @Test
    void editingReplacesTheLatestGeneration() {
        SpaceTimeProjection projection = new SpaceTimeProjection();
        projection.restarted(row(0, 0));
        projection.advanced(row(0, 1));

        projection.edited(row(1, 1));

        assertArrayEquals(new int[] {0, 0, 1, 1}, projection.frame(row(1, 1)).states());
    }

    @Test
    void everyRowOfASpaceTimeDiagramStandsForTheCurrentGeneration() {
        assertEquals(Coordinates.of(4), new SpaceTimeProjection().locate(17, 4));
    }

    @Test
    void aSpaceTimeDiagramNeedsAOneDimensionalGrid() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new SpaceTimeProjection().restarted(new Grid(Shape.of(2, 2))));
        assertThrows(IllegalArgumentException.class, () -> new SpaceTimeProjection(0));
    }

    @Test
    void aPlaneProjectionShowsTheCurrentGenerationThroughItsSlice() {
        Grid grid = new Grid(Shape.of(3, 3, 3));
        grid.setState(Coordinates.of(1, 2, 0), Cell.ON);
        PlaneProjection projection = new PlaneProjection(Slice.parse("(1, :, :)"));

        assertEquals(1, projection.frame(grid).stateAt(2, 0));
        assertEquals(Coordinates.of(1, 2, 0), projection.locate(2, 0));

        projection.setSlice(Slice.parse("(0, :, :)"));
        assertEquals(0, projection.frame(grid).stateAt(2, 0));
        assertEquals(Coordinates.of(0, 2, 0), projection.locate(2, 0));
    }
}
