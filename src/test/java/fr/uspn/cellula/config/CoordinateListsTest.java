package fr.uspn.cellula.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.uspn.cellula.grid.Coordinates;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CoordinateListsTest {

    @Test
    void parsesTheSetNotationOfTheSubject() {
        assertEquals(
                List.of(Coordinates.of(0, 0, 0), Coordinates.of(1, 1, 0), Coordinates.of(-1, 0, 0)),
                CoordinateLists.parse("{(0, 0, 0), (1, 1, 0), (-1, 0, 0)}"));
    }

    @Test
    void bracesAndSpacingAreOptional() {
        assertEquals(
                List.of(Coordinates.of(-1, 0), Coordinates.of(1, 0)),
                CoordinateLists.parse(" (-1,0) ,\n\t( 1 , 0 ) "));
    }

    @Test
    void oneDimensionalCoordinatesMayOmitTheirParentheses() {
        List<Coordinates> expected =
                List.of(Coordinates.of(-2), Coordinates.of(-1), Coordinates.of(1), Coordinates.of(2));

        assertEquals(expected, CoordinateLists.parse("{(-2), (-1), (1), (2)}"));
        assertEquals(expected, CoordinateLists.parse("-2, -1, 1, 2"));
        assertEquals(List.of(Coordinates.of(128)), CoordinateLists.parse("128"));
        assertEquals(List.of(Coordinates.of(3)), CoordinateLists.parse("+3"));
    }

    @Test
    void anEmptyTextIsAnEmptyList() {
        assertTrue(CoordinateLists.parse("").isEmpty());
        assertTrue(CoordinateLists.parse("  \n ").isEmpty());
        assertTrue(CoordinateLists.parse("{}").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"(1, 2", "{(1, 2)", "(1, 2))", "(1,, 2)", "(a, b)", "(1, 2) (3, 4)", "()", "(1.5)", "1 2", ","})
    void rejectsMalformedLists(String text) {
        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> CoordinateLists.parse(text));

        assertTrue(error.getMessage().startsWith("invalid coordinate list: "), error.getMessage());
    }
}
