package fr.uspn.cellula.grid;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GridTest {

    @Test
    void startsWithEveryCellOff() {
        Grid grid = new Grid(Shape.of(3, 4));

        assertEquals(12, grid.cells().size());
        assertEquals(0, grid.population());
        assertEquals(BoundaryCondition.FIXED, grid.boundary());
        assertEquals(2, grid.dimension());
    }

    @Test
    void cellsKnowTheirPosition() {
        Grid grid = new Grid(Shape.of(2, 3));

        assertEquals(Coordinates.of(1, 2), grid.cell(1, 2).position());
        assertEquals(Coordinates.of(0, 0), grid.cells().get(0).position());
        assertEquals(Coordinates.of(1, 2), grid.cells().get(5).position());
    }

    @Test
    void statesCanBeSetAndRead() {
        Grid grid = new Grid(Shape.of(3, 3));

        grid.setState(Coordinates.of(1, 2), Cell.ON);

        assertTrue(grid.cell(1, 2).isOn());
        assertFalse(grid.cell(2, 1).isOn());
        assertEquals(1, grid.population());
        assertThrows(IndexOutOfBoundsException.class, () -> grid.setState(Coordinates.of(3, 0), 1));
    }

    @Test
    void outsideAFixedGridEveryCellIsOff() {
        Grid grid = new Grid(Shape.of(2, 2), BoundaryCondition.FIXED);
        grid.fill(position -> Cell.ON);

        assertSame(VoidCell.INSTANCE, grid.cellAt(-1, 0));
        assertSame(VoidCell.INSTANCE, grid.cellAt(0, 2));
        assertEquals(Cell.OFF, grid.cellAt(5, 5).state());
        assertSame(grid.cell(1, 1), grid.cellAt(1, 1));
    }

    @Test
    void aPeriodicGridWrapsAround() {
        Grid grid = new Grid(Shape.of(3, 4), BoundaryCondition.PERIODIC);

        assertSame(grid.cell(2, 0), grid.cellAt(-1, 0));
        assertSame(grid.cell(0, 3), grid.cellAt(3, -1));
        assertSame(grid.cell(1, 1), grid.cellAt(7, 9));
    }

    @Test
    void lookingUpACellDoesNotModifyTheCoordinates() {
        Grid grid = new Grid(Shape.of(3, 3), BoundaryCondition.PERIODIC);
        int[] coordinates = {-1, 5};

        grid.cellAt(coordinates);

        assertArrayEquals(new int[] {-1, 5}, coordinates);
    }

    @Test
    void lookingUpACellRequiresOneCoordinatePerAxis() {
        Grid grid = new Grid(Shape.of(3, 3));

        assertThrows(IllegalArgumentException.class, () -> grid.cellAt(1));
    }

    @Test
    void evolutionIsSimultaneous() {
        // Each cell copies its left neighbor. Updating in place, left to right, would smear the
        // first cell over the whole row; a simultaneous update shifts the row by exactly one.
        Grid grid = new Grid(Shape.of(5));
        grid.setState(Coordinates.of(0), Cell.ON);

        int changed = grid.evolve(cell -> grid.cellAt(cell.position().get(0) - 1).state());

        assertArrayEquals(new int[] {0, 1, 0, 0, 0}, grid.snapshot());
        assertEquals(2, changed);
    }

    @Test
    void evolutionReportsHowManyCellsChanged() {
        Grid grid = new Grid(Shape.of(4, 4));

        assertEquals(0, grid.evolve(Cell::state));
        assertEquals(16, grid.evolve(cell -> Cell.ON));
        assertEquals(0, grid.evolve(cell -> Cell.ON));
    }

    @Test
    void largeGridsEvolveInParallelWithTheSameResult() {
        Grid grid = new Grid(Shape.of(200, 200), BoundaryCondition.PERIODIC);
        grid.fill(position -> (position.get(0) * 31 + position.get(1) * 17) % 3 == 0 ? 1 : 0);
        int[] before = grid.snapshot();

        grid.evolve(cell -> grid.cellAt(cell.position().get(0), cell.position().get(1) + 1).state());

        int[] after = grid.snapshot();
        for (int row = 0; row < 200; row++) {
            for (int column = 0; column < 200; column++) {
                assertEquals(before[row * 200 + (column + 1) % 200], after[row * 200 + column]);
            }
        }
    }

    @Test
    void snapshotsCanBeRestored() {
        Grid grid = new Grid(Shape.of(2, 3));
        grid.setState(Coordinates.of(0, 1), 1);
        grid.setState(Coordinates.of(1, 2), 2);
        int[] snapshot = grid.snapshot();

        grid.clear();
        assertEquals(0, grid.population());
        grid.restore(snapshot);

        assertArrayEquals(new int[] {0, 1, 0, 0, 0, 2}, grid.snapshot());
        assertEquals(2, grid.population());
        assertThrows(IllegalArgumentException.class, () -> grid.restore(new int[5]));
    }

    @Test
    void theFingerprintIdentifiesAConfiguration() {
        Grid first = new Grid(Shape.of(4, 4));
        Grid second = new Grid(Shape.of(4, 4));
        assertEquals(first.fingerprint(), second.fingerprint());

        first.setState(Coordinates.of(1, 1), Cell.ON);
        assertNotEquals(first.fingerprint(), second.fingerprint());

        second.setState(Coordinates.of(1, 1), Cell.ON);
        assertEquals(first.fingerprint(), second.fingerprint());

        second.setState(Coordinates.of(1, 1), Cell.OFF);
        second.setState(Coordinates.of(1, 2), Cell.ON);
        assertNotEquals(first.fingerprint(), second.fingerprint());
    }
}
