package fr.uspn.cellula.grid;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShapeTest {

    @Test
    void describesABox() {
        Shape shape = Shape.of(4, 3, 2);

        assertEquals(3, shape.dimension());
        assertEquals(4, shape.size(0));
        assertEquals(2, shape.size(2));
        assertEquals(24, shape.cellCount());
        assertEquals("4 × 3 × 2", shape.toString());
        assertEquals(Shape.of(4, 3, 2), shape);
    }

    @Test
    void rejectsEmptyAndNonPositiveSizes() {
        assertThrows(IllegalArgumentException.class, Shape::of);
        assertThrows(IllegalArgumentException.class, () -> Shape.of(3, 0));
        assertThrows(IllegalArgumentException.class, () -> Shape.of(-1));
    }

    @Test
    void rejectsGridsThatWouldNotFitInMemory() {
        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> Shape.of(100_000, 100_000));

        assertTrue(error.getMessage().contains("too large"), error.getMessage());
    }

    @Test
    void tellsInsideFromOutside() {
        Shape shape = Shape.of(4, 3);

        assertTrue(shape.contains(0, 0));
        assertTrue(shape.contains(3, 2));
        assertFalse(shape.contains(4, 0));
        assertFalse(shape.contains(0, -1));
        assertThrows(IllegalArgumentException.class, () -> shape.contains(1));
    }

    @Test
    void wrapsCoordinatesLikeATorus() {
        Shape shape = Shape.of(4, 3);

        assertArrayEquals(new int[] {3, 0}, shape.wrap(-1, 3));
        assertArrayEquals(new int[] {1, 2}, shape.wrap(9, -4));
        assertArrayEquals(new int[] {2, 1}, shape.wrap(2, 1));
    }
}
