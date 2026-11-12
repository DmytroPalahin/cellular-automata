package fr.uspn.cellula.neighborhood;

/** Thrown when a rule refers to a neighborhood that does not exist for the grid's dimension. */
public final class UnknownNeighborhoodException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    private final String name;

    UnknownNeighborhoodException(String name, NeighborhoodCatalog catalog) {
        super("unknown neighborhood '" + name + "' for a " + catalog.dimension()
                + "-dimensional grid (available: " + String.join(", ", catalog.names()) + ")");
        this.name = name;
    }

    /** Returns the name that could not be resolved. */
    public String name() {
        return name;
    }
}
