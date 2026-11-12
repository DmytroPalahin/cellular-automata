package fr.uspn.cellula.explore;

import fr.uspn.cellula.config.AutomatonDefinition;
import fr.uspn.cellula.config.InitialState;
import fr.uspn.cellula.grid.Coordinates;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * A two-state, one-dimensional rule given by its truth table.
 *
 * <p>A rule of radius {@code r} looks at the {@code 2r + 1} cells centered on the cell under
 * examination. Reading those cells from left to right as a binary number gives the index of the
 * neighborhood, and bit {@code i} of the rule's <em>code</em> is the next state for neighborhood
 * {@code i}. For radius 1 this is exactly Wolfram's numbering of the 256 elementary automata
 * (rule 18, rule 30, rule 110, ...); for radius 2 the code has 32 bits, hence the 2<sup>32</sup>
 * rules mentioned by the bonus question.
 *
 * <p>This compact form exists for one purpose: to simulate very many rules very fast while
 * searching for interesting ones. A rule worth keeping is then translated into the rule language
 * ({@link #toExpression()}) and becomes an ordinary definition ({@link #toDefinition}), run by the
 * general engine like any other.
 */
public final class TableRule {

    /** Largest supported radius: the truth table must fit in a {@code long}. */
    public static final int MAX_RADIUS = 2;

    private final int radius;
    private final long code;

    private TableRule(int radius, long code) {
        this.radius = radius;
        this.code = code;
    }

    /**
     * Creates a rule from its code.
     *
     * @param radius 1 or 2
     * @param code the truth table; only its {@code 2^(2 * radius + 1)} low bits may be set
     */
    public static TableRule of(int radius, long code) {
        if (radius < 1 || radius > MAX_RADIUS) {
            throw new IllegalArgumentException("radius must be 1 or 2, got " + radius);
        }
        int entries = 1 << (2 * radius + 1);
        if (code < 0 || (entries < Long.SIZE && code >>> entries != 0)) {
            throw new IllegalArgumentException(
                    "code " + code + " does not fit in the " + entries + " entries of a radius-"
                            + radius + " rule");
        }
        return new TableRule(radius, code);
    }

    /**
     * Creates a totalistic rule: the next state only depends on how many cells of the
     * neighborhood are on.
     *
     * @param radius 1 or 2
     * @param sumCode bit {@code s} is the next state when exactly {@code s} cells are on
     */
    public static TableRule totalistic(int radius, int sumCode) {
        int span = 2 * radius + 1;
        if (sumCode < 0 || sumCode >>> (span + 1) != 0) {
            throw new IllegalArgumentException(
                    "totalistic code " + sumCode + " does not fit in " + (span + 1) + " bits");
        }
        long code = 0;
        for (int neighborhood = 0; neighborhood < 1 << span; neighborhood++) {
            if ((sumCode >>> Integer.bitCount(neighborhood) & 1) != 0) {
                code |= 1L << neighborhood;
            }
        }
        return of(radius, code);
    }

    /** Returns the radius of the rule. */
    public int radius() {
        return radius;
    }

    /** Returns the number of cells the rule looks at: {@code 2 * radius + 1}. */
    public int span() {
        return 2 * radius + 1;
    }

    /** Returns the truth table as a number. */
    public long code() {
        return code;
    }

    /**
     * Returns the next state for a neighborhood.
     *
     * @param neighborhood the cells of the neighborhood read left to right as a binary number
     */
    public int output(int neighborhood) {
        return (int) (code >>> neighborhood & 1);
    }

    /**
     * Tells whether the cell at a given offset has any influence on the result.
     *
     * @param offset position relative to the cell under examination, in {@code [-radius, radius]}
     */
    public boolean dependsOn(int offset) {
        int bit = 1 << (radius - offset);
        for (int neighborhood = 0; neighborhood < 1 << span(); neighborhood++) {
            if (output(neighborhood) != output(neighborhood ^ bit)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tells whether this rule ignores every cell farther than {@code smallerRadius}, i.e. whether
     * it is a rule of smaller radius in disguise.
     */
    public boolean fitsRadius(int smallerRadius) {
        for (int offset = smallerRadius + 1; offset <= radius; offset++) {
            if (dependsOn(offset) || dependsOn(-offset)) {
                return false;
            }
        }
        return true;
    }

    /** Returns the totalistic code of this rule, if the rule is totalistic. */
    public OptionalInt totalisticCode() {
        int sumCode = 0;
        for (int sum = 0; sum <= span(); sum++) {
            sumCode |= output((1 << sum) - 1) << sum;
        }
        return totalistic(radius, sumCode).code == code
                ? OptionalInt.of(sumCode)
                : OptionalInt.empty();
    }

    /**
     * Computes the next generation of a row of cells. Cells beyond both ends count as off, which
     * is the fixed boundary of the engine.
     *
     * @param cells the current generation, one 0/1 value per cell
     * @return the next generation, in a new array
     */
    public byte[] step(byte[] cells) {
        int mask = (1 << span()) - 1;
        byte[] next = new byte[cells.length];
        // The window holds the neighborhood of cell i; sliding it right shifts one cell in.
        int window = 0;
        for (int position = 0; position < radius && position < cells.length; position++) {
            window = window << 1 | cells[position];
        }
        for (int position = 0; position < cells.length; position++) {
            int entering = position + radius;
            window = (window << 1 | (entering < cells.length ? cells[entering] : 0)) & mask;
            next[position] = (byte) output(window);
        }
        return next;
    }

    /**
     * Translates the rule into the rule language of the engine.
     *
     * <p>A totalistic rule becomes a disjunction of tests on {@code COMPTER} over the whole
     * neighborhood. Any other rule becomes a binary decision tree of {@code SI} operators that
     * tests the cells one by one, from left to right, and stops as soon as the result is settled.
     *
     * @return an expression equivalent to this truth table; it refers to the neighborhoods
     *     declared by {@link #toDefinition}
     */
    public String toExpression() {
        OptionalInt sumCode = totalisticCode();
        if (sumCode.isPresent()) {
            return totalisticExpression(sumCode.getAsInt());
        }
        return decisionTree(0, 0);
    }

    private String totalisticExpression(int sumCode) {
        String count = "COMPTER(" + wholeNeighborhoodName() + ")";
        String expression = null;
        for (int sum = span(); sum >= 0; sum--) {
            if ((sumCode >>> sum & 1) != 0) {
                String test = "EQ(" + count + ", " + sum + ")";
                expression = expression == null ? test : "OU(" + test + ", " + expression + ")";
            }
        }
        return expression == null ? "0" : expression;
    }

    /**
     * Builds the sub-tree deciding the result once the {@code depth} leftmost cells are known.
     *
     * @param depth number of cells already tested
     * @param prefix their states, read as a binary number
     */
    private String decisionTree(int depth, int prefix) {
        int remaining = span() - depth;
        int first = prefix << remaining;
        boolean constant = true;
        for (int index = first + 1; index < first + (1 << remaining); index++) {
            if (output(index) != output(first)) {
                constant = false;
                break;
            }
        }
        if (constant) {
            return Integer.toString(output(first));
        }
        String cell = "COMPTER(" + cellNeighborhoodName(depth - radius) + ")";
        String whenOn = decisionTree(depth + 1, prefix << 1 | 1);
        String whenOff = decisionTree(depth + 1, prefix << 1);
        if (whenOn.equals(whenOff)) {
            return whenOn; // this cell does not matter here
        }
        if (whenOn.equals("1") && whenOff.equals("0")) {
            return cell;
        }
        if (whenOn.equals("0") && whenOff.equals("1")) {
            return "NON(" + cell + ")";
        }
        return "SI(" + cell + ", " + whenOn + ", " + whenOff + ")";
    }

    /**
     * Returns a complete automaton definition running this rule on the general engine.
     *
     * @param name display name of the automaton
     * @param description free-form description
     * @param width number of cells
     * @param initialState how the row is filled at generation 0
     */
    public AutomatonDefinition toDefinition(
            String name, String description, int width, InitialState initialState) {
        String expression = toExpression();
        AutomatonDefinition.Builder builder = AutomatonDefinition.builder()
                .name(name)
                .description(description)
                .size(width)
                .rule(expression)
                .initialState(initialState);

        // Declare only the neighborhoods the expression actually uses.
        if (radius > 1 && expression.contains("(" + wholeNeighborhoodName() + ")")) {
            List<Coordinates> all = new ArrayList<>();
            for (int offset = -radius; offset <= radius; offset++) {
                all.add(Coordinates.of(offset));
            }
            builder.neighborhood(wholeNeighborhoodName(), all);
        }
        for (int offset = -radius; offset <= radius; offset++) {
            String cellName = cellNeighborhoodName(offset);
            if (offset != 0 && expression.contains("(" + cellName + ")")) {
                builder.neighborhood(cellName, List.of(Coordinates.of(offset)));
            }
        }
        return builder.build();
    }

    /**
     * Name of the neighborhood holding all the cells of the rule. For radius 1 it is the
     * predefined {@code G2}; for radius 2 it is declared as {@code G4} (four neighbors and the
     * cell), a name that is free on one-dimensional grids.
     */
    private String wholeNeighborhoodName() {
        return "G" + 2 * radius;
    }

    /**
     * Name of the one-cell neighborhood at a given offset: the predefined {@code G0} for the
     * cell itself, and {@code G1k} for the others, numbered from left to right ({@code G11} is
     * the leftmost cell).
     */
    private String cellNeighborhoodName(int offset) {
        if (offset == 0) {
            return "G0";
        }
        int rank = offset < 0 ? offset + radius + 1 : offset + radius;
        return "G1" + rank;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TableRule that && radius == that.radius && code == that.code;
    }

    @Override
    public int hashCode() {
        return 31 * radius + Long.hashCode(code);
    }

    @Override
    public String toString() {
        return "radius-" + radius + " rule " + code;
    }
}
