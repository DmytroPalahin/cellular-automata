package fr.uspn.cellula.neighborhood;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.uspn.cellula.grid.Coordinates;

/**
 * The neighborhoods a rule may refer to by name, for grids of one given
 * dimension.
 *
 * <p>
 * A catalog knows the predefined neighborhoods of its dimension and the ones
 * declared by the
 * user. Names always follow the convention of the subject: a capital {@code G},
 * a number, and an
 * optional star.
 *
 * <p>
 * Predefined names are resolved against the dimension of the catalog.
 * {@code G0} exists
 * everywhere; {@code G<2d>} is the von Neumann neighborhood and
 * {@code G<3^d - 1>} the Moore
 * neighborhood, each with a starred variant that leaves out the central cell.
 * Hence {@code G4}
 * and {@code G8} exist only in 2D, {@code G6} and {@code G26} only in 3D,
 * exactly as specified,
 * and a four-dimensional grid gets {@code G8} (von Neumann) and {@code G80}
 * (Moore) for free.
 *
 * <p>
 * A user-defined neighborhood takes precedence over a predefined one with the
 * same name.
 */
public final class NeighborhoodCatalog {

    private static final Pattern NAME = Pattern.compile("G(\\d+)(\\*?)");

    private final int dimension;
    private final Map<String, Neighborhood> defined = new LinkedHashMap<>();
    private final Map<String, Neighborhood> predefined = new HashMap<>();

    /**
     * Creates a catalog holding the predefined neighborhoods of a dimension.
     *
     * @param dimension dimension of the grids the neighborhoods will be walked on
     */
    public NeighborhoodCatalog(int dimension) {
        if (dimension < 1) {
            throw new IllegalArgumentException("dimension must be positive, got " + dimension);
        }
        this.dimension = dimension;
    }

    /** Returns the dimension this catalog is bound to. */
    public int dimension() {
        return dimension;
    }

    /** Tells whether a name follows the {@code G<number>[*]} convention. */
    public static boolean isValidName(String name) {
        return NAME.matcher(name).matches();
    }

    /**
     * Declares a user-defined neighborhood.
     *
     * @param name    a name following the {@code G<number>[*]} convention
     * @param offsets the coordinates of the neighbors of a cell located at the
     *                origin
     * @return the new neighborhood
     * @throws IllegalArgumentException if the name is malformed or already
     *                                  declared, if there is
     *                                  no offset, or if an offset does not have the
     *                                  dimension of the catalog
     */
    public Neighborhood define(String name, Collection<Coordinates> offsets) {
        if (!isValidName(name)) {
            throw new IllegalArgumentException(
                    "invalid neighborhood name '" + name
                            + "': expected a capital G, a number and an optional star, like G12*");
        }
        if (defined.containsKey(name)) {
            throw new IllegalArgumentException("neighborhood " + name + " is defined twice");
        }
        for (Coordinates offset : offsets) {
            if (offset.dimension() != dimension) {
                throw new IllegalArgumentException(
                        "neighborhood " + name + ": offset " + offset + " has "
                                + offset.dimension() + " coordinate(s) but the grid has "
                                + dimension + " dimension(s)");
            }
        }
        Neighborhood neighborhood = new OffsetNeighborhood(name, offsets);
        defined.put(name, neighborhood);
        return neighborhood;
    }

    /** Looks a neighborhood up by name. */
    public Optional<Neighborhood> find(String name) {
        Neighborhood custom = defined.get(name);
        if (custom != null) {
            return Optional.of(custom);
        }
        return Optional.ofNullable(predefined.computeIfAbsent(name, this::generate));
    }

    /**
     * Returns the neighborhood with the given name.
     *
     * @throws UnknownNeighborhoodException if no such neighborhood exists in this
     *                                      dimension
     */
    public Neighborhood resolve(String name) {
        return find(name).orElseThrow(() -> new UnknownNeighborhoodException(name, this));
    }

    /**
     * Returns every usable name: the predefined ones first, then the user-defined
     * ones.
     */
    public List<String> names() {
        List<String> names = new ArrayList<>();
        names.add("G0");
        long vonNeumann = Neighborhoods.vonNeumannCount(dimension);
        long moore = Neighborhoods.mooreCount(dimension);
        names.add("G" + vonNeumann);
        names.add("G" + vonNeumann + "*");
        if (moore != vonNeumann && find("G" + moore).isPresent()) {
            names.add("G" + moore);
            names.add("G" + moore + "*");
        }
        for (String name : defined.keySet()) {
            if (!names.contains(name)) {
                names.add(name);
            }
        }
        return names;
    }

    /**
     * Builds the predefined neighborhood with the given name, or returns
     * {@code null}.
     */
    private Neighborhood generate(String name) {
        Matcher matcher = NAME.matcher(name);
        if (!matcher.matches()) {
            return null;
        }
        long neighbors;
        try {
            neighbors = Long.parseLong(matcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
        boolean starred = !matcher.group(2).isEmpty();

        OffsetNeighborhood base;
        if (neighbors == 0) {
            base = Neighborhoods.self(dimension);
        } else if (neighbors == Neighborhoods.vonNeumannCount(dimension)) {
            base = Neighborhoods.vonNeumann(dimension);
        } else if (neighbors == Neighborhoods.mooreCount(dimension)) {
            try {
                base = Neighborhoods.moore(dimension);
            } catch (IllegalArgumentException tooLarge) {
                return null;
            }
        } else {
            return null;
        }
        if (!starred) {
            return base;
        }
        // "G0*" would be the empty neighborhood, which the subject does not define.
        return neighbors == 0 ? null : Neighborhoods.punctured(base);
    }
}
