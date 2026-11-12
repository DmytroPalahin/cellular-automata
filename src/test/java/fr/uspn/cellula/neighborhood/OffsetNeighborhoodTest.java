package fr.uspn.cellula.neighborhood;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import fr.uspn.cellula.grid.BoundaryCondition;
import fr.uspn.cellula.grid.Cell;
import fr.uspn.cellula.grid.Coordinates;
import fr.uspn.cellula.grid.Grid;
import fr.uspn.cellula.grid.Shape;
import fr.uspn.cellula.grid.VoidCell;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class OffsetNeighborhoodTest {

    private static List<Cell> walk(Neighborhood neighborhood, Grid grid, int... center) {
        List<Cell> cells = new ArrayList<>();
        for (Cell cell : neighborhood.around(grid, grid.cell(center))) {
            cells.add(cell);
        }
        return cells;
    }

    @Test
    void walksTheCellsAtItsOffsets() {
        Grid grid = new Grid(Shape.of(5, 5));
        Neighborhood neighborhood = new OffsetNeighborhood(
                "G3", List.of(Coordinates.of(0, 0), Coordinates.of(1, 1), Coordinates.of(-1, 0)));

        assertEquals(
                List.of(grid.cell(2, 2), grid.cell(3, 3), grid.cell(1, 2)),
                walk(neighborhood, grid, 2, 2));
    }

    @Test
    void beyondAFixedEdgeItFindsCellsThatAreOff() {
        Grid grid = new Grid(Shape.of(3, 3), BoundaryCondition.FIXED);
        grid.fill(position -> Cell.ON);

        List<Cell> corner = walk(Neighborhoods.moore(2), grid, 0, 0);

        assertEquals(9, corner.size());
        assertEquals(5, corner.stream().filter(cell -> cell == VoidCell.INSTANCE).count());
        assertEquals(4, corner.stream().filter(Cell::isOn).count());
    }

    @Test
    void beyondAPeriodicEdgeItFindsTheCellsOfTheOppositeEdge() {
        Grid grid = new Grid(Shape.of(4), BoundaryCondition.PERIODIC);

        List<Cell> neighbors = walk(Neighborhoods.punctured(Neighborhoods.vonNeumann(1)), grid, 0);

        assertEquals(List.of(grid.cell(3), grid.cell(1)), neighbors);
    }

    @Test
    void everyWalkStartsAfresh() {
        Grid grid = new Grid(Shape.of(3));
        Iterable<Cell> around = Neighborhoods.vonNeumann(1).around(grid, grid.cell(1));

        Iterator<Cell> first = around.iterator();
        first.next();
        Iterator<Cell> second = around.iterator();

        assertSame(grid.cell(0), second.next());
        assertSame(grid.cell(1), first.next());
        first.next();
        assertFalse(first.hasNext());
        assertThrows(NoSuchElementException.class, first::next);
    }

    @Test
    void isASet() {
        OffsetNeighborhood neighborhood = new OffsetNeighborhood(
                "G1", List.of(Coordinates.of(1), Coordinates.of(1), Coordinates.of(-1)));

        assertEquals(2, neighborhood.size());
        assertEquals("G1 = {(1), (-1)}", neighborhood.toString());
    }

    @Test
    void rejectsInconsistentDefinitions() {
        assertThrows(IllegalArgumentException.class, () -> new OffsetNeighborhood("G1", List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new OffsetNeighborhood("G1", List.of(Coordinates.of(1), Coordinates.of(1, 0))));
        assertThrows(
                IllegalArgumentException.class,
                () -> Neighborhoods.self(2).withoutCenter("G0*"));
    }

    @Test
    void cannotBeWalkedOnAGridOfAnotherDimension() {
        Grid grid = new Grid(Shape.of(3, 3, 3));

        assertThrows(
                IllegalArgumentException.class,
                () -> Neighborhoods.moore(2).around(grid, grid.cell(1, 1, 1)));
    }

    @Test
    void measuresItsReach() {
        assertEquals(0, Neighborhoods.self(2).radius());
        assertEquals(
                2,
                new OffsetNeighborhood("G2", List.of(Coordinates.of(-2), Coordinates.of(1))).radius());
    }
}
