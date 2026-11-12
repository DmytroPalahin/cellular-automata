package fr.uspn.cellula.explore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * Searches the one-dimensional rules of radius 2 for those that draw a fractal
 * or chaotic picture
 * (bonus question of the subject).
 *
 * <p>
 * There are 2<sup>32</sup> such rules, too many to look at, so the explorer
 * works on families:
 * the 64 totalistic rules exhaustively, and random samples of the left-right
 * symmetric rules or of
 * all rules. Each rule is run from a single cell with the fast
 * {@link TableRule} simulator and its
 * picture is classified from two measurements, the box-counting dimension and
 * the compression
 * ratio (see {@link SpaceTimeDiagram}).
 *
 * <p>
 * A candidate is only reported if it is <em>genuinely</em> of radius 2, as the
 * question
 * demands: its truth table depends on the outer cells, and its picture differs
 * from the picture
 * of each of the 256 rules of radius 1 started from the same row.
 */
public final class RuleExplorer {

    /** The families of radius-2 rules that can be explored. */
    public enum Family {
        /** The 64 rules whose result only depends on the number of live cells. */
        TOTALISTIC,
        /**
         * Rules unchanged by mirroring left and right; sampled among 2<sup>20</sup>.
         */
        SYMMETRIC,
        /** All rules; sampled among 2<sup>32</sup>. */
        GENERAL
    }

    /** What kind of picture a rule draws from a single cell. */
    public enum Verdict {
        /** Every cell ends up off. */
        EXTINCT,
        /** Uniform, periodic or otherwise trivially predictable. */
        REGULAR,
        /** Nested and self-similar, with a non-integer dimension. */
        FRACTAL,
        /** Fills its light cone with a disordered, incompressible texture. */
        CHAOTIC
    }

    /**
     * A rule together with the measurements of its picture.
     *
     * @param rule             the rule
     * @param verdict          the kind of picture it draws
     * @param boxDimension     estimated box-counting dimension of the picture
     * @param compressionRatio compressed size over raw size of the picture
     */
    public record Candidate(
            TableRule rule, Verdict verdict, double boxDimension, double compressionRatio) {
    }

    /** Pictures compressing below this ratio are too orderly to be interesting. */
    static final double REGULAR_COMPRESSION = 0.015;

    /** Pictures of dimension below this are essentially lines. */
    static final double MIN_FRACTAL_DIMENSION = 1.25;

    /** Pictures of dimension above this fill the plane. */
    static final double MAX_FRACTAL_DIMENSION = 1.85;

    /** Space-filling pictures compressing above this ratio are disordered. */
    static final double CHAOTIC_COMPRESSION = 0.30;

    private static final int RADIUS = 2;

    private final int steps;
    private Map<Integer, List<Long>> radiusOneCodes;
    private int radiusOneWidth;

    /**
     * Creates an explorer.
     *
     * @param steps number of generations each rule is run for; 255 gives pictures
     *              of 256 rows,
     *              a power of two that suits box counting
     */
    public RuleExplorer(int steps) {
        if (steps < 32) {
            throw new IllegalArgumentException("at least 32 steps are needed, got " + steps);
        }
        this.steps = steps;
    }

    /** Returns the number of generations each rule is run for. */
    public int steps() {
        return steps;
    }

    /**
     * Runs a rule from a single cell and classifies the resulting picture.
     *
     * @param rule the rule to assess
     */
    public Candidate assess(TableRule rule) {
        SpaceTimeDiagram diagram = SpaceTimeDiagram.fromSingleCell(rule, steps);
        double dimension = diagram.boxDimension();
        double compression = diagram.compressionRatio();
        return new Candidate(rule, classify(diagram, dimension, compression), dimension, compression);
    }

    private Verdict classify(SpaceTimeDiagram diagram, double dimension, double compression) {
        if (diagram.population(diagram.generations() - 1) == 0) {
            return Verdict.EXTINCT;
        }
        if (compression < REGULAR_COMPRESSION || dimension < MIN_FRACTAL_DIMENSION) {
            return Verdict.REGULAR;
        }
        if (dimension <= MAX_FRACTAL_DIMENSION) {
            return Verdict.FRACTAL;
        }
        return compression >= CHAOTIC_COMPRESSION ? Verdict.CHAOTIC : Verdict.REGULAR;
    }

    /**
     * Explores a family and returns its fractal and chaotic members.
     *
     * @param family  the family to explore
     * @param samples number of rules to draw; ignored for
     *                {@link Family#TOTALISTIC}, which is
     *                explored exhaustively
     * @param seed    seed of the random generator, for reproducible searches
     * @return the interesting rules, genuinely of radius 2; fractal ones first,
     *         ordered by
     *         increasing dimension, then chaotic ones, ordered by decreasing
     *         compression ratio
     */
    public List<Candidate> search(Family family, int samples, long seed) {
        List<TableRule> rules = switch (family) {
            case TOTALISTIC -> totalisticRules();
            case SYMMETRIC -> sample(samples, seed, true);
            case GENERAL -> sample(samples, seed, false);
        };
        List<Candidate> interesting = rules.parallelStream()
                // A rule that switches cells on in an empty row makes the whole background
                // flicker: there is no picture to speak of.
                .filter(rule -> rule.output(0) == 0)
                .filter(rule -> !rule.fitsRadius(1))
                .map(this::assess)
                .filter(candidate -> candidate.verdict() == Verdict.FRACTAL
                        || candidate.verdict() == Verdict.CHAOTIC)
                .collect(Collectors.toCollection(ArrayList::new));
        interesting.removeIf(candidate -> reproducibleWithRadiusOne(candidate.rule()));
        interesting.sort(Comparator.comparing(Candidate::verdict)
                .thenComparingDouble(candidate -> candidate.verdict() == Verdict.FRACTAL
                        ? candidate.boxDimension()
                        : -candidate.compressionRatio())
                .thenComparingLong(candidate -> candidate.rule().code()));
        return interesting;
    }

    /**
     * Tells whether one of the 256 rules of radius 1 draws exactly the same picture
     * as the given
     * rule, from the same single cell on the same row.
     */
    public boolean reproducibleWithRadiusOne(TableRule rule) {
        SpaceTimeDiagram picture = SpaceTimeDiagram.fromSingleCell(rule, steps);
        byte[] start = SpaceTimeDiagram.singleCell(picture.width());
        // Equal pictures have equal hash codes: only the few rules sharing the hash are
        // re-run.
        for (long code : radiusOneCodesByHash(picture.width()).getOrDefault(picture.hashCode(), List.of())) {
            if (SpaceTimeDiagram.of(TableRule.of(1, code), start, steps).equals(picture)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Indexes the pictures of the 256 radius-1 rules by hash code. Keeping the
     * pictures themselves
     * would take tens of megabytes; their hash codes take a few kilobytes.
     */
    private synchronized Map<Integer, List<Long>> radiusOneCodesByHash(int width) {
        if (radiusOneCodes == null || radiusOneWidth != width) {
            byte[] start = SpaceTimeDiagram.singleCell(width);
            radiusOneCodes = LongStream.range(0, 256)
                    .parallel()
                    .boxed()
                    .collect(Collectors.groupingBy(
                            code -> SpaceTimeDiagram.of(TableRule.of(1, code), start, steps).hashCode()));
            radiusOneWidth = width;
        }
        return radiusOneCodes;
    }

    private static List<TableRule> totalisticRules() {
        List<TableRule> rules = new ArrayList<>();
        for (int sumCode = 0; sumCode < 64; sumCode++) {
            rules.add(TableRule.totalistic(RADIUS, sumCode));
        }
        return rules;
    }

    /** Draws distinct random rules that leave an empty row empty. */
    private static List<TableRule> sample(int samples, long seed, boolean symmetric) {
        if (samples < 1) {
            throw new IllegalArgumentException("at least one sample is needed, got " + samples);
        }
        SplittableRandom random = new SplittableRandom(seed);
        return LongStream.generate(() -> {
            long code = random.nextLong() & 0xFFFF_FFFEL;
            return symmetric ? symmetrize(code) : code;
        })
                .distinct()
                .limit(samples)
                .mapToObj(code -> TableRule.of(RADIUS, code))
                .toList();
    }

    /**
     * Makes a radius-2 truth table mirror-symmetric by copying each entry onto its
     * mirror.
     */
    private static long symmetrize(long code) {
        long symmetric = 0;
        for (int neighborhood = 0; neighborhood < 32; neighborhood++) {
            int mirror = Integer.reverse(neighborhood) >>> 27;
            if ((code >>> Math.min(neighborhood, mirror) & 1) != 0) {
                symmetric |= 1L << neighborhood;
            }
        }
        return symmetric;
    }
}
