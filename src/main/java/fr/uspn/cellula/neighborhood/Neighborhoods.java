package fr.uspn.cellula.neighborhood;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import fr.uspn.cellula.grid.Coordinates;

/**
 * Factory of the classical neighborhoods, in any dimension.
 *
 * <p>
 * The subject lists six predefined neighborhoods ({@code G0}, {@code G2},
 * {@code G4},
 * {@code G8}, {@code G6}, {@code G26}). They are instances of only three
 * families, each of which
 * is defined for every dimension {@code d}:
 *
 * <table>
 * <caption>Predefined neighborhoods and the family they belong to</caption>
 * <tr>
 * <th>Family</th>
 * <th>Neighbors</th>
 * <th>1D</th>
 * <th>2D</th>
 * <th>3D</th>
 * </tr>
 * <tr>
 * <td>{@link #self}</td>
 * <td>0</td>
 * <td>G0</td>
 * <td>G0</td>
 * <td>G0</td>
 * </tr>
 * <tr>
 * <td>{@link #vonNeumann}</td>
 * <td>2d</td>
 * <td>G2</td>
 * <td>G4</td>
 * <td>G6</td>
 * </tr>
 * <tr>
 * <td>{@link #moore}</td>
 * <td>3<sup>d</sup> - 1</td>
 * <td>G2</td>
 * <td>G8</td>
 * <td>G26</td>
 * </tr>
 * </table>
 *
 * <p>
 * Generating them instead of hard-coding six classes keeps a single
 * implementation and makes
 * the catalog work unchanged on four-dimensional grids and beyond. All
 * neighborhoods built here
 * include the cell under examination; {@link #punctured} derives the starred
 * variant.
 */
public final class Neighborhoods {

    /** Largest Moore neighborhood this factory agrees to materialize. */
    private static final long MAX_SIZE = 1_000_000;

    private Neighborhoods() {
    }

    /** Returns {@code G0}: the cell under examination, alone. */
    public static OffsetNeighborhood self(int dimension) {
        return new OffsetNeighborhood("G0", List.of(Coordinates.origin(dimension)));
    }

    /**
     * Returns the von Neumann neighborhood: the cell and the {@code 2d} cells
     * sharing a face with
     * it. It is named after its number of neighbors ({@code G2}, {@code G4},
     * {@code G6}, ...).
     */
    public static OffsetNeighborhood vonNeumann(int dimension) {
        List<Coordinates> offsets = new ArrayList<>();
        offsets.add(Coordinates.origin(dimension));
        for (int axis = 0; axis < dimension; axis++) {
            for (int step : new int[] { -1, 1 }) {
                int[] offset = new int[dimension];
                offset[axis] = step;
                offsets.add(Coordinates.of(offset));
            }
        }
        Collections.sort(offsets);
        return new OffsetNeighborhood("G" + vonNeumannCount(dimension), offsets);
    }

    /**
     * Returns the Moore neighborhood: the cell and the {@code 3^d - 1} cells
     * touching it by a
     * face, an edge or a corner. It is named after its number of neighbors
     * ({@code G2},
     * {@code G8}, {@code G26}, ...).
     *
     * @throws IllegalArgumentException if the dimension is so high that the
     *                                  neighborhood would
     *                                  not reasonably fit in memory
     */
    public static OffsetNeighborhood moore(int dimension) {
        long count = mooreCount(dimension);
        if (count > MAX_SIZE) {
            throw new IllegalArgumentException(
                    "the Moore neighborhood has " + count + " cells in dimension " + dimension
                            + ", which is too many");
        }
        List<Coordinates> offsets = new ArrayList<>((int) count + 1);
        int[] offset = new int[dimension];
        Arrays.fill(offset, -1);
        boolean exhausted = false;
        while (!exhausted) {
            offsets.add(Coordinates.of(offset));
            // Odometer over {-1, 0, 1}^d: the last axis turns fastest, giving lexicographic
            // order.
            int axis = dimension - 1;
            while (axis >= 0 && offset[axis] == 1) {
                offset[axis--] = -1;
            }
            if (axis < 0) {
                exhausted = true;
            } else {
                offset[axis]++;
            }
        }
        return new OffsetNeighborhood("G" + count, offsets);
    }

    /**
     * Returns the starred variant of a neighborhood: the same cells minus the cell
     * under
     * examination, named with a trailing {@code *}.
     */
    public static OffsetNeighborhood punctured(OffsetNeighborhood neighborhood) {
        return neighborhood.withoutCenter(neighborhood.name() + "*");
    }

    /**
     * Returns the number of neighbors in the von Neumann neighborhood: {@code 2d}.
     */
    static long vonNeumannCount(int dimension) {
        return 2L * dimension;
    }

    /**
     * Returns the number of neighbors in the Moore neighborhood: {@code 3^d - 1},
     * saturating.
     */
    static long mooreCount(int dimension) {
        long power = 1;
        for (int axis = 0; axis < dimension; axis++) {
            power *= 3;
            if (power > 4 * MAX_SIZE) {
                return Long.MAX_VALUE;
            }
        }
        return power - 1;
    }
}
