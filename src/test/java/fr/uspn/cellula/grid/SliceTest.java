package fr.uspn.cellula.grid;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SliceTest {

    @Test
    void parsesTheNotationOfTheSubject() {
        Slice slice = Slice.parse("(37, :, :)");

        assertEquals(3, slice.dimension());
        assertFalse(slice.isFree(0));
        assertTrue(slice.isFree(1));
        assertTrue(slice.isFree(2));
        assertEquals(37, slice.pinnedAt(0));
        assertEquals(List.of(0), slice.pinnedAxes());
        assertEquals("(37, :, :)", slice.toString());
    }

    @Test
    void toleratesSpacingAndMissingParentheses() {
        assertEquals(Slice.parse("(1,:,:)"), Slice.parse("  1 , : ,:  "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"(1, 2, 3)", "(:, :, :)", "(1, :, 2)", "(a, :, :)", "(-1, :, :)", "(1, 2)", "", "(,)"})
    void rejectsMalformedSlices(String text) {
        assertThrows(IllegalArgumentException.class, () -> Slice.parse(text));
    }

    @Test
    void lowDimensionsHaveASingleSlice() {
        assertEquals("(:)", Slice.centered(Shape.of(9)).toString());
        assertEquals("(:, :)", Slice.centered(Shape.of(9, 9)).toString());
        assertEquals(Slice.parse("(:)"), Slice.centered(Shape.of(9)));
    }

    @Test
    void theDefaultSliceCutsThroughTheMiddle() {
        assertEquals("(:, :, 5, 3)", Slice.centered(Shape.of(4, 4, 10, 7)).toString());
    }

    @Test
    void extractsAPlaneOfAThreeDimensionalGrid() {
        Grid grid = new Grid(Shape.of(2, 3, 4));
        grid.fill(position -> 100 * position.get(0) + 10 * position.get(1) + position.get(2));

        Frame frame = Slice.parse("(:, 2, :)").extract(grid);

        assertEquals(2, frame.rows());
        assertEquals(4, frame.columns());
        assertArrayEquals(new int[] {20, 21, 22, 23, 120, 121, 122, 123}, frame.states());
        assertEquals(122, frame.stateAt(1, 2));
    }

    @Test
    void extractsAOneDimensionalGridAsASingleRow() {
        Grid grid = new Grid(Shape.of(4));
        grid.setState(Coordinates.of(2), Cell.ON);

        Frame frame = Slice.centered(grid.shape()).extract(grid);

        assertEquals(1, frame.rows());
        assertArrayEquals(new int[] {0, 0, 1, 0}, frame.states());
    }

    @Test
    void mapsPictureCellsBackToGridPositions() {
        Slice slice = Slice.parse("(:, 7, :, 2)");

        assertEquals(Coordinates.of(4, 7, 9, 2), slice.locate(4, 9));
        assertEquals(Coordinates.of(5), Slice.parse("(:)").locate(123, 5));
    }

    @Test
    void canBeMovedAlongAPinnedAxis() {
        Slice slice = Slice.parse("(3, :, :)");

        assertEquals("(8, :, :)", slice.moved(0, 8).toString());
        assertEquals("(3, :, :)", slice.toString());
        assertThrows(IllegalArgumentException.class, () -> slice.moved(1, 0));
        assertThrows(IllegalArgumentException.class, () -> slice.moved(0, -1));
        assertThrows(IllegalArgumentException.class, () -> slice.pinnedAt(2));
    }

    @Test
    void mustFitTheGrid() {
        Shape shape = Shape.of(10, 10, 10);

        Slice.parse("(9, :, :)").requireWithin(shape);
        assertThrows(IllegalArgumentException.class, () -> Slice.parse("(10, :, :)").requireWithin(shape));
        assertThrows(IllegalArgumentException.class, () -> Slice.parse("(:, :)").requireWithin(shape));
        assertThrows(
                IllegalArgumentException.class,
                () -> Slice.parse("(10, :, :)").extract(new Grid(shape)));
    }

    @Test
    void framesValidateTheirSize() {
        assertThrows(IllegalArgumentException.class, () -> new Frame(2, 2, new int[3]));
        assertThrows(IndexOutOfBoundsException.class, () -> new Frame(2, 2, new int[4]).stateAt(2, 0));
    }
}
