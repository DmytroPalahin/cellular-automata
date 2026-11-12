package fr.uspn.cellula.grid;

import java.util.Locale;

/**
 * What a rule sees when it looks beyond the edge of the grid.
 *
 * <p>Each constant is a strategy that resolves out-of-range coordinates to a {@link Cell}.
 */
public enum BoundaryCondition {

    /**
     * Positions outside the grid hold a cell that is permanently off. This is the behavior
     * mandated by the subject and the default.
     */
    FIXED {
        @Override
        Cell outside(Grid grid, int[] coordinates) {
            return VoidCell.INSTANCE;
        }
    },

    /**
     * Opposite edges are glued together, turning the grid into a torus. Useful to let gliders
     * travel forever on a small grid.
     */
    PERIODIC {
        @Override
        Cell outside(Grid grid, int[] coordinates) {
            return grid.cell(grid.shape().wrap(coordinates));
        }
    };

    /**
     * Resolves coordinates that lie outside {@code grid}.
     *
     * @param grid the grid being read
     * @param coordinates out-of-range coordinates; must not be modified
     * @return the cell to use in place of the missing one
     */
    abstract Cell outside(Grid grid, int[] coordinates);

    /**
     * Parses the name used in definition files.
     *
     * @param text {@code "fixed"} or {@code "periodic"}, in any letter case
     * @return the matching condition
     * @throws IllegalArgumentException if the name is unknown
     */
    public static BoundaryCondition parse(String text) {
        try {
            return valueOf(text.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "unknown boundary condition '" + text.strip() + "' (expected fixed or periodic)");
        }
    }

    /** Returns the lower-case name used in definition files. */
    public String label() {
        return name().toLowerCase(Locale.ROOT);
    }
}
