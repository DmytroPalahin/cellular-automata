package fr.uspn.cellula.simulation;

/**
 * An observer of a {@link Simulation}.
 *
 * <p>Listeners are called on the simulation thread. A graphical listener must therefore forward
 * the snapshot to its own thread (for Swing, with {@code SwingUtilities.invokeLater}) instead of
 * touching components directly, and no listener should block.
 */
@FunctionalInterface
public interface SimulationListener {

    /**
     * Called whenever the visible state of the simulation changed: a step was taken, the grid was
     * reset or edited, the simulation was started or paused, or the slice was moved.
     *
     * @param snapshot the new state
     */
    void simulationChanged(Snapshot snapshot);
}
