package fr.uspn.cellula.automaton;

import java.util.Objects;
import java.util.function.Consumer;

import fr.uspn.cellula.grid.Grid;
import fr.uspn.cellula.rule.Rule;

/**
 * A cellular automaton: a grid of cells, a rule, and a discrete clock.
 *
 * <p>
 * Each call to {@link #step()} makes the clock tick once: the rule is applied
 * to every cell of
 * the grid simultaneously and the generation counter is incremented. The
 * automaton also watches
 * its own trajectory and reports when it converges or starts cycling (see
 * {@link Regime}).
 *
 * <p>
 * This class knows nothing about files, threads or display. Like the grid it
 * owns, an
 * automaton must be confined to one thread at a time.
 */
public final class CellularAutomaton {

    private final Grid grid;
    private final Rule rule;
    private final CycleDetector cycles = new CycleDetector();
    private long generation;
    private Regime regime = Regime.TRANSIENT;

    /**
     * Creates an automaton at generation 0.
     *
     * @param grid the grid, already holding the initial state
     * @param rule the evolution rule
     */
    public CellularAutomaton(Grid grid, Rule rule) {
        this.grid = Objects.requireNonNull(grid, "grid");
        this.rule = Objects.requireNonNull(rule, "rule");
        cycles.observe(grid.fingerprint(), generation);
    }

    /** Returns the grid, in its current generation. */
    public Grid grid() {
        return grid;
    }

    /** Returns the evolution rule. */
    public Rule rule() {
        return rule;
    }

    /** Returns the number of steps taken since the initial state. */
    public long generation() {
        return generation;
    }

    /** Returns the long-term behavior observed so far. */
    public Regime regime() {
        return regime;
    }

    /**
     * Advances the automaton by one time step.
     *
     * @return what the step did
     */
    public StepReport step() {
        int changed = grid.evolve(cell -> rule.nextState(grid, cell));
        generation++;
        if (changed == 0) {
            regime = Regime.FIXED_POINT;
        } else {
            // A genuine repetition after a step that changed cells is at least two steps
            // apart.
            int period = cycles.observe(grid.fingerprint(), generation);
            regime = period >= 2 ? Regime.cycle(period) : Regime.TRANSIENT;
        }
        return new StepReport(generation, changed, regime);
    }

    /**
     * Advances the automaton by several time steps.
     *
     * @param steps number of steps to take; must not be negative
     */
    public void advance(int steps) {
        if (steps < 0) {
            throw new IllegalArgumentException("cannot advance by " + steps + " steps");
        }
        for (int step = 0; step < steps; step++) {
            step();
        }
    }

    /**
     * Rewinds the automaton to generation 0 with a new initial state.
     *
     * @param initializer writes the initial state into the grid
     */
    public void restart(Consumer<? super Grid> initializer) {
        initializer.accept(grid);
        generation = 0;
        forgetTrajectory();
    }

    /**
     * Tells the automaton that its grid was modified from the outside, for instance
     * by a user
     * drawing cells. Past generations are no longer relevant for cycle detection.
     */
    public void gridEdited() {
        forgetTrajectory();
    }

    private void forgetTrajectory() {
        regime = Regime.TRANSIENT;
        cycles.reset();
        cycles.observe(grid.fingerprint(), generation);
    }

    @Override
    public String toString() {
        return "CellularAutomaton[" + grid + ", generation " + generation + ", " + regime.describe() + "]";
    }
}
