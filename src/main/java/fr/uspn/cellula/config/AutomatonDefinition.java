package fr.uspn.cellula.config;

import fr.uspn.cellula.grid.BoundaryCondition;
import fr.uspn.cellula.grid.Coordinates;
import fr.uspn.cellula.grid.Shape;
import fr.uspn.cellula.grid.Slice;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The complete, validated description of an automaton, as read from a definition file.
 *
 * <p>A definition is plain immutable data. It deliberately holds the rule as text and the
 * neighborhoods as offset lists: turning them into live objects is the job of
 * {@link AutomatonFactory}, so the same definition can be instantiated several times, displayed
 * in a menu, or written back to a file.
 *
 * @param name short display name
 * @param description free-form text shown next to the name; may be empty
 * @param shape extent of the grid
 * @param boundary what lies beyond the edges of the grid
 * @param slice the plane of the grid shown on screen
 * @param neighborhoods user-defined neighborhoods: offsets by name, in declaration order
 * @param rule the evolution rule, in the rule language
 * @param initialState how the grid is filled at generation 0
 * @param colors display colors as {@code 0xRRGGBB} values, by state; states without an entry get
 *     a default color
 */
public record AutomatonDefinition(
        String name,
        String description,
        Shape shape,
        BoundaryCondition boundary,
        Slice slice,
        Map<String, List<Coordinates>> neighborhoods,
        String rule,
        InitialState initialState,
        Map<Integer, Integer> colors) {

    /** Validates the components and makes the collections unmodifiable. */
    public AutomatonDefinition {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(shape, "shape");
        Objects.requireNonNull(boundary, "boundary");
        Objects.requireNonNull(slice, "slice");
        Objects.requireNonNull(rule, "rule");
        Objects.requireNonNull(initialState, "initialState");
        if (name.isBlank()) {
            throw new IllegalArgumentException("the automaton needs a name");
        }
        if (rule.isBlank()) {
            throw new IllegalArgumentException("the automaton needs a rule");
        }
        slice.requireWithin(shape);
        initialState.requireWithin(shape);

        Map<String, List<Coordinates>> copy = new LinkedHashMap<>();
        neighborhoods.forEach((key, offsets) -> copy.put(key, List.copyOf(offsets)));
        neighborhoods = Collections.unmodifiableMap(copy);
        colors = Collections.unmodifiableMap(new LinkedHashMap<>(colors));
    }

    /** Returns the number of dimensions of the grid. */
    public int dimension() {
        return shape.dimension();
    }

    /** Returns a one-line summary such as {@code "2D · 96 × 64 · fixed boundary"}. */
    public String summary() {
        return dimension() + "D · " + shape + " · " + boundary.label() + " boundary";
    }

    /** Starts the description of a new automaton. */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Step-by-step construction of a definition.
     *
     * <p>Only the name, the shape and the rule are mandatory. By default the boundary is fixed,
     * the slice is {@linkplain Slice#centered centered}, and the grid starts empty.
     */
    public static final class Builder {

        private String name;
        private String description = "";
        private Shape shape;
        private BoundaryCondition boundary = BoundaryCondition.FIXED;
        private Slice slice;
        private final Map<String, List<Coordinates>> neighborhoods = new LinkedHashMap<>();
        private String rule;
        private InitialState initialState = new InitialState.ExplicitCells(List.of());
        private final Map<Integer, Integer> colors = new LinkedHashMap<>();

        private Builder() {}

        /** Sets the display name. */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /** Sets the free-form description. */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /** Sets the extent of the grid. */
        public Builder shape(Shape shape) {
            this.shape = shape;
            return this;
        }

        /** Sets the extent of the grid, one size per axis. */
        public Builder size(int... sizes) {
            return shape(Shape.of(sizes));
        }

        /** Sets the boundary condition. */
        public Builder boundary(BoundaryCondition boundary) {
            this.boundary = boundary;
            return this;
        }

        /** Sets the plane shown on screen. */
        public Builder slice(Slice slice) {
            this.slice = slice;
            return this;
        }

        /**
         * Declares a user-defined neighborhood.
         *
         * @throws IllegalArgumentException if the name was already declared
         */
        public Builder neighborhood(String name, List<Coordinates> offsets) {
            if (neighborhoods.putIfAbsent(name, List.copyOf(offsets)) != null) {
                throw new IllegalArgumentException("neighborhood " + name + " is defined twice");
            }
            return this;
        }

        /** Sets the evolution rule. */
        public Builder rule(String rule) {
            this.rule = rule;
            return this;
        }

        /** Sets how the grid is filled at generation 0. */
        public Builder initialState(InitialState initialState) {
            this.initialState = initialState;
            return this;
        }

        /**
         * Sets the display color of a state.
         *
         * @param state the state
         * @param rgb the color as {@code 0xRRGGBB}
         */
        public Builder color(int state, int rgb) {
            colors.put(state, rgb & 0xFFFFFF);
            return this;
        }

        /**
         * Builds the definition.
         *
         * @throws IllegalArgumentException if a mandatory part is missing or the parts are
         *     inconsistent with each other
         */
        public AutomatonDefinition build() {
            if (name == null) {
                throw new IllegalArgumentException("the automaton needs a name");
            }
            if (shape == null) {
                throw new IllegalArgumentException("the automaton needs a grid size");
            }
            if (rule == null) {
                throw new IllegalArgumentException("the automaton needs a rule");
            }
            return new AutomatonDefinition(
                    name,
                    description,
                    shape,
                    boundary,
                    slice != null ? slice : Slice.centered(shape),
                    neighborhoods,
                    rule,
                    initialState,
                    colors);
        }
    }
}
