package fr.uspn.cellula.simulation;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import fr.uspn.cellula.automaton.CellularAutomaton;
import fr.uspn.cellula.automaton.Regime;
import fr.uspn.cellula.config.AutomatonDefinition;
import fr.uspn.cellula.config.AutomatonFactory;
import fr.uspn.cellula.grid.Coordinates;
import fr.uspn.cellula.grid.Grid;
import fr.uspn.cellula.grid.Slice;

/**
 * A running automaton that can be driven from another thread: played, paused,
 * stepped, reset,
 * edited.
 *
 * <p>
 * <strong>Threading model.</strong> The automaton and its grid are confined to
 * a single
 * background thread owned by this class. Every public method merely posts a
 * command to that
 * thread and returns at once, so commands are executed one after the other, in
 * the order they
 * were issued, without any lock. Nothing mutable ever leaves the simulation
 * thread: observers
 * receive immutable {@link Snapshot}s. A user interface therefore stays
 * responsive however long a
 * generation takes to compute, and never sees a half-updated grid.
 *
 * <p>
 * A simulation holds a thread and must be {@linkplain #close() closed} when no
 * longer needed.
 */
public final class Simulation implements AutoCloseable {

    private static final Logger LOG = System.getLogger(Simulation.class.getName());

    private final AutomatonDefinition definition;
    private final ScheduledExecutorService worker;
    private final List<SimulationListener> listeners = new CopyOnWriteArrayList<>();

    // Everything below is only touched by the worker thread once the constructor
    // has returned.
    private final CellularAutomaton automaton;
    private final Projection projection;
    private int[] initialStates;
    private boolean running;
    private int lastChanged;
    private long intervalMillis = 100;
    private ScheduledFuture<?> nextTick;

    /**
     * Creates a paused simulation at generation 0.
     *
     * @param definition the automaton to simulate
     * @throws IllegalArgumentException if the definition cannot be instantiated
     */
    public Simulation(AutomatonDefinition definition) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.automaton = AutomatonFactory.create(definition);
        this.projection = Projection.of(definition);
        this.projection.restarted(automaton.grid());
        this.initialStates = automaton.grid().snapshot();
        this.worker = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "cellula-simulation");
            thread.setDaemon(true);
            return thread;
        });
    }

    /** Returns the definition this simulation was created from. */
    public AutomatonDefinition definition() {
        return definition;
    }

    /**
     * Registers an observer. It is immediately sent the current state, then every
     * change.
     *
     * @param listener the observer; called on the simulation thread
     */
    public void addListener(SimulationListener listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(listener);
        post(() -> listener.simulationChanged(snapshot()));
    }

    /** Unregisters an observer. */
    public void removeListener(SimulationListener listener) {
        listeners.remove(listener);
    }

    /** Lets the automaton advance on its own, one generation per interval. */
    public void play() {
        post(() -> {
            if (!running) {
                running = true;
                publish();
                nextTick = worker.schedule(guarded(this::tick), 0, TimeUnit.MILLISECONDS);
            }
        });
    }

    /** Stops the automatic advance. */
    public void pause() {
        post(() -> {
            if (running) {
                halt();
                publish();
            }
        });
    }

    /** Pauses if needed, then advances by exactly one generation. */
    public void step() {
        post(() -> {
            halt();
            advanceOnce();
            publish();
        });
    }

    /**
     * Pauses and goes back to the initial state, exactly as it was at generation 0.
     */
    public void reset() {
        post(() -> {
            halt();
            int[] states = initialStates;
            automaton.restart(grid -> grid.restore(states));
            restarted();
        });
    }

    /**
     * Pauses and draws a new initial state from the definition. For a
     * {@code RANDOM(k)} definition
     * without a fixed seed this yields a different grid each time.
     */
    public void reseed() {
        post(() -> {
            halt();
            automaton.restart(definition.initialState()::applyTo);
            initialStates = automaton.grid().snapshot();
            restarted();
        });
    }

    /** Pauses and switches every cell off, leaving a blank grid to draw on. */
    public void clear() {
        post(() -> {
            halt();
            automaton.restart(Grid::clear);
            initialStates = automaton.grid().snapshot();
            restarted();
        });
    }

    /**
     * Sets the pause between two generations while playing.
     *
     * @param millis the delay in milliseconds; 0 means as fast as possible
     */
    public void setInterval(long millis) {
        if (millis < 0) {
            throw new IllegalArgumentException("interval must not be negative, got " + millis);
        }
        post(() -> intervalMillis = millis);
    }

    /**
     * Moves the displayed plane. Has no effect on one-dimensional automata, and a
     * slice that does
     * not fit the grid is ignored.
     *
     * @param slice the new plane
     */
    public void setSlice(Slice slice) {
        Objects.requireNonNull(slice, "slice");
        post(() -> {
            if (projection instanceof PlaneProjection plane) {
                try {
                    slice.requireWithin(automaton.grid().shape());
                } catch (IllegalArgumentException outside) {
                    return;
                }
                plane.setSlice(slice);
                publish();
            }
        });
    }

    /**
     * Sets the state of the grid cell displayed at a given place of the picture.
     *
     * @param row    row in the displayed frame
     * @param column column in the displayed frame
     * @param state  the new state of the cell
     */
    public void setCell(int row, int column, int state) {
        post(() -> {
            Grid grid = automaton.grid();
            Coordinates position = projection.locate(row, column);
            if (!grid.shape().contains(position.toArray())) {
                return;
            }
            if (grid.cell(position).state() == state) {
                return;
            }
            grid.setState(position, state);
            automaton.gridEdited();
            projection.edited(grid);
            if (automaton.generation() == 0) {
                // Drawing on the initial state redefines what "reset" goes back to.
                initialStates = grid.snapshot();
            }
            publish();
        });
    }

    /**
     * Returns the state of the simulation once all the commands issued so far have
     * been executed.
     *
     * @return a future completed on the simulation thread
     */
    public CompletableFuture<Snapshot> state() {
        CompletableFuture<Snapshot> future = new CompletableFuture<>();
        try {
            worker.execute(() -> {
                try {
                    future.complete(snapshot());
                } catch (RuntimeException e) {
                    future.completeExceptionally(e);
                }
            });
        } catch (RejectedExecutionException e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    /** Stops the simulation thread. Commands issued afterwards are ignored. */
    @Override
    public void close() {
        worker.shutdownNow();
    }

    // ---------------------------------------------------------------------------------------
    // Worker thread only
    // ---------------------------------------------------------------------------------------

    private void tick() {
        if (!running) {
            return;
        }
        advanceOnce();
        if (automaton.regime().equals(Regime.FIXED_POINT)) {
            // Nothing will ever change again: no point in burning CPU.
            running = false;
        }
        publish();
        if (running) {
            nextTick = worker.schedule(guarded(this::tick), intervalMillis, TimeUnit.MILLISECONDS);
        }
    }

    private void advanceOnce() {
        lastChanged = automaton.step().changedCells();
        projection.advanced(automaton.grid());
    }

    private void halt() {
        running = false;
        if (nextTick != null) {
            nextTick.cancel(false);
            nextTick = null;
        }
    }

    private void restarted() {
        lastChanged = 0;
        projection.restarted(automaton.grid());
        publish();
    }

    private Snapshot snapshot() {
        Grid grid = automaton.grid();
        Optional<Slice> slice = projection instanceof PlaneProjection plane
                ? Optional.of(plane.slice())
                : Optional.empty();
        return new Snapshot(
                automaton.generation(),
                grid.population(),
                grid.shape().cellCount(),
                lastChanged,
                automaton.regime(),
                running,
                slice,
                projection.frame(grid));
    }

    private void publish() {
        Snapshot snapshot = snapshot();
        for (SimulationListener listener : listeners) {
            listener.simulationChanged(snapshot);
        }
    }

    private void post(Runnable command) {
        try {
            worker.execute(guarded(command));
        } catch (RejectedExecutionException e) {
            // The simulation was closed: late commands from the user interface are dropped.
        }
    }

    /** Keeps a failing command from silently killing the periodic task. */
    private Runnable guarded(Runnable command) {
        return () -> {
            try {
                command.run();
            } catch (RuntimeException e) {
                running = false;
                LOG.log(Level.ERROR, "simulation command failed", e);
            }
        };
    }
}
