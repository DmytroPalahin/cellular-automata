package fr.uspn.cellula.config;

import fr.uspn.cellula.grid.Cell;
import fr.uspn.cellula.grid.Coordinates;
import fr.uspn.cellula.grid.Grid;
import fr.uspn.cellula.grid.Shape;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * How the grid of an automaton is filled at generation 0.
 *
 * <p>The subject allows two forms: the list of the cells that start in state 1, or the keyword
 * {@code RANDOM(k)} which switches each cell on with probability {@code k}%. The two forms are
 * the two implementations of this sealed interface.
 */
public sealed interface InitialState {

    /**
     * Writes the initial state into a grid, overwriting every cell.
     *
     * @param grid the grid to initialize
     */
    void applyTo(Grid grid);

    /**
     * Checks that this initial state makes sense for a grid of the given shape.
     *
     * @throws IllegalArgumentException if it does not
     */
    void requireWithin(Shape shape);

    /** Returns the notation of this initial state in definition files. */
    String describe();

    /**
     * Parses the content of the {@code <initial>} element.
     *
     * @param text either {@code RANDOM(k)} or a list of coordinates
     * @param seed seed of the random generator, if the file fixes one
     * @return the initial state
     * @throws IllegalArgumentException if the text is malformed
     */
    static InitialState parse(String text, OptionalLong seed) {
        String trimmed = text.strip();
        Matcher random = RandomFill.SYNTAX.matcher(trimmed);
        if (random.matches()) {
            try {
                return new RandomFill(Integer.parseInt(random.group(1)), seed);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "RANDOM(k) needs k between 0 and 100, got " + random.group(1));
            }
        }
        if (trimmed.toUpperCase(Locale.ROOT).startsWith("RANDOM")) {
            throw new IllegalArgumentException(
                    "invalid initial state '" + trimmed
                            + "': expected RANDOM(k) with k an integer between 0 and 100");
        }
        return new ExplicitCells(CoordinateLists.parse(text));
    }

    /**
     * Every cell is independently switched on with a given probability.
     *
     * @param percentage probability, in percent, that a cell starts in state 1
     * @param seed seed of the random generator; when absent, each application draws a new state
     */
    record RandomFill(int percentage, OptionalLong seed) implements InitialState {

        private static final Pattern SYNTAX =
                Pattern.compile("RANDOM\\s*\\(\\s*(\\d+)\\s*\\)", Pattern.CASE_INSENSITIVE);

        /** Validates the percentage. */
        public RandomFill {
            Objects.requireNonNull(seed, "seed");
            if (percentage < 0 || percentage > 100) {
                throw new IllegalArgumentException(
                        "RANDOM(k) needs k between 0 and 100, got " + percentage);
            }
        }

        @Override
        public void applyTo(Grid grid) {
            Random random = seed.isPresent() ? new Random(seed.getAsLong()) : new Random();
            grid.fill(position -> random.nextInt(100) < percentage ? Cell.ON : Cell.OFF);
        }

        @Override
        public void requireWithin(Shape shape) {
            // A random fill suits any grid.
        }

        @Override
        public String describe() {
            return "RANDOM(" + percentage + ")";
        }
    }

    /**
     * The listed cells start in state 1, all the others in state 0.
     *
     * @param cells positions of the cells that are initially on
     */
    record ExplicitCells(List<Coordinates> cells) implements InitialState {

        /** Takes a defensive copy of the list. */
        public ExplicitCells {
            cells = List.copyOf(cells);
        }

        @Override
        public void applyTo(Grid grid) {
            requireWithin(grid.shape());
            grid.clear();
            for (Coordinates cell : cells) {
                grid.setState(cell, Cell.ON);
            }
        }

        @Override
        public void requireWithin(Shape shape) {
            for (Coordinates cell : cells) {
                if (cell.dimension() != shape.dimension()) {
                    throw new IllegalArgumentException(
                            "initial cell " + cell + " has " + cell.dimension()
                                    + " coordinate(s) but the grid has " + shape.dimension()
                                    + " dimension(s)");
                }
                if (!shape.contains(cell.toArray())) {
                    throw new IllegalArgumentException(
                            "initial cell " + cell + " is outside the " + shape + " grid");
                }
            }
        }

        @Override
        public String describe() {
            return cells.stream().map(Coordinates::toString).collect(Collectors.joining(", "));
        }
    }
}
