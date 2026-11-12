package fr.uspn.cellula.config;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.uspn.cellula.config.InitialState.ExplicitCells;
import fr.uspn.cellula.config.InitialState.RandomFill;
import fr.uspn.cellula.grid.Coordinates;
import fr.uspn.cellula.grid.Grid;
import fr.uspn.cellula.grid.Shape;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class InitialStateTest {

    @Test
    void parsesTheRandomKeyword() {
        assertEquals(new RandomFill(15, OptionalLong.empty()), InitialState.parse("RANDOM(15)", OptionalLong.empty()));
        assertEquals(new RandomFill(0, OptionalLong.of(7)), InitialState.parse("  random ( 0 ) ", OptionalLong.of(7)));
        assertEquals("RANDOM(15)", new RandomFill(15, OptionalLong.empty()).describe());
    }

    @Test
    void parsesAListOfCells() {
        InitialState state = InitialState.parse("(0, 1), (2, 2)", OptionalLong.empty());

        assertEquals(new ExplicitCells(List.of(Coordinates.of(0, 1), Coordinates.of(2, 2))), state);
        assertEquals("(0, 1), (2, 2)", state.describe());
    }

    @ParameterizedTest
    @ValueSource(strings = {"RANDOM(101)", "RANDOM(-5)", "RANDOM", "RANDOM()", "RANDOM(1.5)", "RANDOM(99999999999)", "RANDOM(10) (1, 2)"})
    void rejectsInvalidRandomSpecifications(String text) {
        assertThrows(IllegalArgumentException.class, () -> InitialState.parse(text, OptionalLong.empty()));
    }

    @Test
    void explicitCellsAreSwitchedOnAndTheRestOff() {
        Grid grid = new Grid(Shape.of(2, 3));
        grid.fill(position -> 5);

        new ExplicitCells(List.of(Coordinates.of(0, 1), Coordinates.of(1, 2))).applyTo(grid);

        assertArrayEquals(new int[] {0, 1, 0, 0, 0, 1}, grid.snapshot());
    }

    @Test
    void explicitCellsMustLieInsideTheGrid() {
        Shape shape = Shape.of(4, 4);

        new ExplicitCells(List.of(Coordinates.of(3, 3))).requireWithin(shape);
        IllegalArgumentException outside = assertThrows(
                IllegalArgumentException.class,
                () -> new ExplicitCells(List.of(Coordinates.of(4, 0))).requireWithin(shape));
        assertEquals("initial cell (4, 0) is outside the 4 × 4 grid", outside.getMessage());
        assertThrows(
                IllegalArgumentException.class,
                () -> new ExplicitCells(List.of(Coordinates.of(1))).requireWithin(shape));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ExplicitCells(List.of(Coordinates.of(9, 9))).applyTo(new Grid(shape)));
    }

    @Test
    void randomFillRespectsTheRequestedDensity() {
        Grid grid = new Grid(Shape.of(200, 200));

        new RandomFill(15, OptionalLong.of(1)).applyTo(grid);

        double density = grid.population() / 40_000.0;
        assertTrue(density > 0.13 && density < 0.17, "density " + density);
        assertTrue(Arrays.stream(grid.snapshot()).allMatch(state -> state == 0 || state == 1));
    }

    @Test
    void theExtremePercentagesAreExact() {
        Grid grid = new Grid(Shape.of(50, 50));

        new RandomFill(0, OptionalLong.empty()).applyTo(grid);
        assertEquals(0, grid.population());
        new RandomFill(100, OptionalLong.empty()).applyTo(grid);
        assertEquals(2500, grid.population());
    }

    @Test
    void aSeedMakesTheRandomFillReproducible() {
        Grid first = new Grid(Shape.of(30, 30));
        Grid second = new Grid(Shape.of(30, 30));
        Grid third = new Grid(Shape.of(30, 30));

        new RandomFill(50, OptionalLong.of(42)).applyTo(first);
        new RandomFill(50, OptionalLong.of(42)).applyTo(second);
        new RandomFill(50, OptionalLong.of(43)).applyTo(third);

        assertArrayEquals(first.snapshot(), second.snapshot());
        assertFalse(Arrays.equals(first.snapshot(), third.snapshot()));
    }

    @Test
    void withoutASeedEachFillIsDifferent() {
        Grid first = new Grid(Shape.of(30, 30));
        Grid second = new Grid(Shape.of(30, 30));
        InitialState random = new RandomFill(50, OptionalLong.empty());

        random.applyTo(first);
        random.applyTo(second);

        assertFalse(Arrays.equals(first.snapshot(), second.snapshot()));
    }
}
