package fr.uspn.cellula.neighborhood;

import fr.uspn.cellula.grid.Cell;
import fr.uspn.cellula.grid.Grid;
import fr.uspn.cellula.grid.GridCell;

/**
 * A rule for walking through the cells that surround a given cell.
 *
 * <p>That is all a neighborhood is: given a grid and a cell of that grid, it yields the cells an
 * evolution rule is allowed to look at. The walk is exposed through the standard
 * {@link Iterable}/{@link java.util.Iterator} protocol, so operators consume a neighborhood with a
 * plain {@code for} loop and never deal with coordinates or grid edges.
 */
public interface Neighborhood {

    /** Returns the name the neighborhood is referred to in rules, such as {@code G8*}. */
    String name();

    /** Returns the dimension of the grids this neighborhood can be walked on. */
    int dimension();

    /** Returns the number of cells visited by one walk. */
    int size();

    /**
     * Walks the neighborhood of a cell.
     *
     * <p>Positions beyond the edge of the grid are not skipped: the grid's boundary condition
     * decides which cell stands there, so a walk always yields exactly {@link #size()} cells.
     *
     * @param grid the grid being explored
     * @param center the cell under examination
     * @return the cells of the neighborhood; each call to {@code iterator()} starts a new walk
     * @throws IllegalArgumentException if the grid does not have the right dimension
     */
    Iterable<Cell> around(Grid grid, GridCell center);
}
