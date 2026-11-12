package fr.uspn.cellula.simulation;

import fr.uspn.cellula.grid.Coordinates;
import fr.uspn.cellula.grid.Frame;
import fr.uspn.cellula.grid.Grid;
import fr.uspn.cellula.grid.Slice;
import java.util.Objects;

/**
 * Shows the current generation of a grid through a plane.
 *
 * <p>For a two-dimensional grid the plane is the grid itself. For three dimensions and more it is
 * the cut selected by a {@link Slice}, which can be moved while the simulation runs.
 */
public final class PlaneProjection implements Projection {

    private Slice slice;

    /**
     * Creates the projection.
     *
     * @param slice the plane to show
     */
    public PlaneProjection(Slice slice) {
        this.slice = Objects.requireNonNull(slice, "slice");
    }

    /** Returns the plane being shown. */
    public Slice slice() {
        return slice;
    }

    /** Moves the projection to another plane. */
    public void setSlice(Slice slice) {
        this.slice = Objects.requireNonNull(slice, "slice");
    }

    @Override
    public void restarted(Grid grid) {
        // The picture only depends on the current generation: nothing to remember.
    }

    @Override
    public void advanced(Grid grid) {
        // Idem.
    }

    @Override
    public void edited(Grid grid) {
        // Idem.
    }

    @Override
    public Frame frame(Grid grid) {
        return slice.extract(grid);
    }

    @Override
    public Coordinates locate(int row, int column) {
        return slice.locate(row, column);
    }
}
