package fr.uspn.cellula.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DynamicArrayNDTest {

    @Test
    void aOneDimensionalArrayIsAPlainVector() {
        DynamicArrayND<String> array = DynamicArrayND.create(Coordinates::toString, 3);

        assertEquals(1, array.dimension());
        assertEquals(3, array.length());
        assertEquals(List.of("(0)", "(1)", "(2)"), array.toList());
    }

    @Test
    void theDimensionIsChosenAtRunTime() {
        for (int dimension = 1; dimension <= 6; dimension++) {
            int[] sizes = new int[dimension];
            Arrays.fill(sizes, 2);

            DynamicArrayND<Integer> array = DynamicArrayND.create(position -> 0, sizes);

            assertEquals(dimension, array.dimension());
            assertEquals(1 << dimension, array.toList().size());
            assertEquals(Shape.of(sizes), array.shape());
        }
    }

    @Test
    void theInitializerReceivesTheCoordinatesOfEachElement() {
        DynamicArrayND<Coordinates> array = DynamicArrayND.create(position -> position, 2, 3, 4);

        assertEquals(Coordinates.of(0, 0, 0), array.get(0, 0, 0));
        assertEquals(Coordinates.of(1, 2, 3), array.get(1, 2, 3));
        assertEquals(Coordinates.of(1, 0, 2), array.get(1, 0, 2));
    }

    @Test
    void elementsCanBeReplaced() {
        DynamicArrayND<String> array = DynamicArrayND.create(position -> "-", 2, 2);

        array.set("x", 1, 0);

        assertEquals("x", array.get(1, 0));
        assertEquals("-", array.get(0, 1));
        assertEquals(List.of("-", "-", "x", "-"), array.toList());
    }

    @Test
    void iteratesInRowMajorOrder() {
        DynamicArrayND<String> array =
                DynamicArrayND.create(position -> position.get(0) + "" + position.get(1), 2, 3);

        List<String> visited = new ArrayList<>();
        for (String element : array) {
            visited.add(element);
        }

        assertEquals(List.of("00", "01", "02", "10", "11", "12"), visited);
        assertEquals(visited, array.toList());
    }

    @Test
    void theIteratorIsExhaustedAfterTheLastElement() {
        Iterator<Integer> iterator = DynamicArrayND.create(position -> 1, 1, 2).iterator();
        iterator.next();
        iterator.next();

        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, iterator::next);
    }

    @Test
    void reportsItsShape() {
        assertEquals(Shape.of(5, 1, 7), DynamicArrayND.create(position -> 0, 5, 1, 7).shape());
    }

    @Test
    void rejectsTheWrongNumberOfIndices() {
        DynamicArrayND<Integer> array = DynamicArrayND.create(position -> 0, 2, 2);

        assertThrows(IllegalArgumentException.class, () -> array.get(1));
        assertThrows(IllegalArgumentException.class, () -> array.set(1, 0, 0, 0));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 3, 100})
    void rejectsIndicesOutOfRange(int index) {
        DynamicArrayND<Integer> array = DynamicArrayND.create(position -> 0, 3, 3);

        assertThrows(IndexOutOfBoundsException.class, () -> array.get(index, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> array.get(0, index));
        assertThrows(IndexOutOfBoundsException.class, () -> array.set(1, 0, index));
    }

    @Test
    void rejectsInvalidSizes() {
        assertThrows(IllegalArgumentException.class, () -> DynamicArrayND.create(position -> 0));
        assertThrows(IllegalArgumentException.class, () -> DynamicArrayND.create(position -> 0, 2, 0));
    }
}
