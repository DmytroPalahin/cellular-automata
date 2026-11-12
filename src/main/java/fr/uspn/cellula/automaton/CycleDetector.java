package fr.uspn.cellula.automaton;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Notices when an automaton comes back to a configuration it has already been
 * in.
 *
 * <p>
 * Keeping whole past generations would cost one grid per generation. The
 * detector remembers a
 * 64-bit fingerprint per generation instead, for a bounded window of recent
 * generations. Since
 * the transition is deterministic, meeting a known fingerprint again means the
 * automaton has
 * entered a cycle whose period is the distance between the two sightings
 * (barring a fingerprint
 * collision, which has a probability in the order of 2<sup>-64</sup> per
 * comparison).
 *
 * <p>
 * Cycles longer than the window go unnoticed; that is the price of bounded
 * memory.
 */
final class CycleDetector {

    /**
     * Number of past generations remembered, hence the longest detectable period.
     */
    static final int WINDOW = 1024;

    private final Map<Long, Long> lastSeen = new LinkedHashMap<>(256, 0.75f, false) {
        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Long> eldest) {
            return size() > WINDOW;
        }
    };

    /** Forgets everything, for instance because the grid was edited by hand. */
    void reset() {
        lastSeen.clear();
    }

    /**
     * Records the configuration reached at a generation.
     *
     * @param fingerprint digest of the configuration
     * @param generation  the generation at which it was reached
     * @return the number of generations since the same configuration was last seen,
     *         or 0 if it is
     *         new
     */
    int observe(long fingerprint, long generation) {
        // Re-inserting keeps the entry fresh in the window (insertion order is eviction
        // order).
        Long previous = lastSeen.remove(fingerprint);
        lastSeen.put(fingerprint, generation);
        return previous == null ? 0 : (int) (generation - previous);
    }
}
