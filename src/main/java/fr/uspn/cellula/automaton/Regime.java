package fr.uspn.cellula.automaton;

/**
 * The long-term behavior an automaton has been observed to settle into.
 *
 * <p>The subject distinguishes three fates for a run: the grid may become identical from one time
 * step to the next (the automaton has <em>converged</em>), it may loop through a repeating
 * sequence of configurations, or it may do neither for as long as one cares to watch.
 *
 * @param kind which of the three behaviors was observed
 * @param period length of the repeating sequence: 1 for a fixed point, the cycle length for a
 *     cycle, and 0 while no repetition has been observed
 */
public record Regime(Kind kind, int period) {

    /** The possible behaviors. */
    public enum Kind {
        /** No repetition observed so far. */
        TRANSIENT,
        /** The last step changed nothing: the configuration is a fixed point. */
        FIXED_POINT,
        /** The configuration is one that was already seen: the automaton is in a cycle. */
        CYCLE
    }

    /** The regime of an automaton that has not been seen repeating itself. */
    public static final Regime TRANSIENT = new Regime(Kind.TRANSIENT, 0);

    /** The regime of an automaton that has converged. */
    public static final Regime FIXED_POINT = new Regime(Kind.FIXED_POINT, 1);

    /**
     * Returns the regime of an automaton cycling with the given period.
     *
     * @param period number of steps after which the configuration repeats, at least 2
     */
    public static Regime cycle(int period) {
        if (period < 2) {
            throw new IllegalArgumentException("a cycle has a period of at least 2, got " + period);
        }
        return new Regime(Kind.CYCLE, period);
    }

    /** Tells whether the automaton will never show a new configuration again. */
    public boolean isSettled() {
        return kind != Kind.TRANSIENT;
    }

    /** Returns a short human-readable description, such as {@code "cycle of period 2"}. */
    public String describe() {
        return switch (kind) {
            case TRANSIENT -> "evolving";
            case FIXED_POINT -> "converged";
            case CYCLE -> "cycle of period " + period;
        };
    }
}
