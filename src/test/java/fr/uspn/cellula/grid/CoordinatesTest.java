package fr.uspn.cellula.grid;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class CoordinatesTest {

    @Test
    void exposesItsComponents() {
        Coordinates coordinates = Coordinates.of(3, -1, 4);

        assertEquals(3, coordinates.dimension());
        assertEquals(3, coordinates.get(0));
        assertEquals(-1, coordinates.get(1));
        assertEquals(4, coordinates.get(2));
        assertArrayEquals(new int[] {3, -1, 4}, coordinates.toArray());
    }

    @Test
    void isIndependentFromTheArraysItIsBuiltFromAndHandsOut() {
        int[] source = {1, 2};
        Coordinates coordinates = Coordinates.of(source);
        source[0] = 99;
        coordinates.toArray()[1] = 99;

        assertEquals(Coordinates.of(1, 2), coordinates);
    }

    @Test
    void rejectsZeroComponents() {
        assertThrows(IllegalArgumentException.class, Coordinates::of);
        assertThrows(IllegalArgumentException.class, () -> Coordinates.origin(0));
    }

    @Test
    void addsComponentWise() {
        assertEquals(Coordinates.of(4, 1), Coordinates.of(3, 2).plus(Coordinates.of(1, -1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> Coordinates.of(1, 2).plus(Coordinates.of(1)));
    }

    @Test
    void knowsTheOrigin() {
        assertTrue(Coordinates.origin(4).isOrigin());
        assertEquals(Coordinates.of(0, 0, 0), Coordinates.origin(3));
        assertFalse(Coordinates.of(0, 1).isOrigin());
    }

    @Test
    void measuresDistanceInRings() {
        assertEquals(0, Coordinates.origin(2).chebyshevNorm());
        assertEquals(1, Coordinates.of(1, -1).chebyshevNorm());
        assertEquals(3, Coordinates.of(1, -3, 2).chebyshevNorm());
    }

    @Test
    void comparesByValue() {
        assertEquals(Coordinates.of(1, 2), Coordinates.of(1, 2));
        assertEquals(Coordinates.of(1, 2).hashCode(), Coordinates.of(1, 2).hashCode());
        assertNotEquals(Coordinates.of(1, 2), Coordinates.of(2, 1));
        assertNotEquals(Coordinates.of(1), Coordinates.of(1, 0));
    }

    @Test
    void sortsLexicographically() {
        List<Coordinates> list = new ArrayList<>(
                List.of(Coordinates.of(1, 0), Coordinates.of(0, 1), Coordinates.of(-1, 5), Coordinates.of(0, -1)));
        Collections.sort(list);

        assertEquals(
                List.of(Coordinates.of(-1, 5), Coordinates.of(0, -1), Coordinates.of(0, 1), Coordinates.of(1, 0)),
                list);
    }

    @Test
    void printsAsATuple() {
        assertEquals("(1, -2, 0)", Coordinates.of(1, -2, 0).toString());
        assertEquals("(7)", Coordinates.of(7).toString());
    }
}
