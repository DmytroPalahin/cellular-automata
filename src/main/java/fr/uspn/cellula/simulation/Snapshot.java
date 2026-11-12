package fr.uspn.cellula.simulation;

import fr.uspn.cellula.automaton.Regime;
import fr.uspn.cellula.grid.Frame;
import fr.uspn.cellula.grid.Slice;
import java.util.Optional;

/**
 * An immutable view of a simulation at one instant, safe to hand to another thread.
 *
 * <p>The simulation never shares its grid. Each time something changes it publishes a snapshot,
 * which carries everything a user interface needs to redraw itself.
 *
 * @param generation number of steps taken since the initial state
 * @param population number of cells of the whole grid that are not off
 * @param cellCount number of cells of the whole grid
 * @param changedCells number of cells modified by the last step; 0 at generation 0
 * @param regime long-term behavior observed so far
 * @param running whether the simulation is currently advancing on its own
 * @param slice the plane shown by {@code frame}, when the automaton is displayed through a plane
 * @param frame the picture to display
 */
public record Snapshot(
        long generation,
        int population,
        int cellCount,
        int changedCells,
        Regime regime,
        boolean running,
        Optional<Slice> slice,
        Frame frame) {

    /** Returns the proportion of cells that are not off, between 0 and 1. */
    public double density() {
        return cellCount == 0 ? 0 : (double) population / cellCount;
    }
}
