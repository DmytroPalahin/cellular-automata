package fr.uspn.cellula.simulation;

import fr.uspn.cellula.config.AutomatonDefinition;
import fr.uspn.cellula.grid.Coordinates;
import fr.uspn.cellula.grid.Frame;
import fr.uspn.cellula.grid.Grid;

/**
 * How a grid of arbitrary dimension becomes a flat picture.
 *
 * <p>
 * The subject asks for two different displays. A grid with two dimensions or
 * more is shown as
 * it is at the current time step, through a plane ({@link PlaneProjection}). A
 * one-dimensional
 * grid would be a single line of pixels; what is interesting is the
 * juxtaposition of its
 * successive states, where time runs down the screen
 * ({@link SpaceTimeProjection}).
 *
 * <p>
 * A projection follows the life of one automaton through the three notification
 * methods and
 * can be asked for a {@link Frame} at any time. Implementations are not
 * thread-safe; they live on
 * the same thread as the automaton they observe.
 */
public interface Projection {

    /**
     * Returns the projection suited to an automaton: space-time for one dimension,
     * the plane
     * chosen in the definition otherwise.
     */
    static Projection of(AutomatonDefinition definition) {
        return definition.dimension() == 1
                ? new SpaceTimeProjection()
                : new PlaneProjection(definition.slice());
    }

    /**
     * Notifies that the automaton is at generation 0, at start-up or after a reset.
     *
     * @param grid the grid in its initial state
     */
    void restarted(Grid grid);

    /**
     * Notifies that the automaton took one step.
     *
     * @param grid the grid in its new generation
     */
    void advanced(Grid grid);

    /**
     * Notifies that the current generation was modified in place, for instance by a
     * user drawing
     * cells.
     *
     * @param grid the modified grid
     */
    void edited(Grid grid);

    /**
     * Takes the picture to display.
     *
     * @param grid the grid in its current generation
     */
    Frame frame(Grid grid);

    /**
     * Returns the position of the grid cell that a cell of the picture stands for,
     * as of the
     * current generation.
     *
     * @param row    row in the last frame
     * @param column column in the last frame
     */
    Coordinates locate(int row, int column);
}
