package fr.uspn.cellula.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ArgumentsTest {

    private static Arguments parse(String... words) throws UsageException {
        return Arguments.parse(List.of(words), Set.of("--steps", "--output"), Set.of("--ascii"));
    }

    @Test
    void separatesPositionalsOptionsAndFlags() throws UsageException {
        Arguments arguments = parse("life.xml", "--steps", "12", "--ascii", "extra");

        assertEquals(List.of("life.xml", "extra"), arguments.positionals());
        assertEquals(Optional.of("12"), arguments.value("--steps"));
        assertEquals(Optional.empty(), arguments.value("--output"));
        assertTrue(arguments.has("--ascii"));
        assertFalse(arguments.has("--help"));
    }

    @Test
    void acceptsInlineValuesAndShortAliases() throws UsageException {
        Arguments arguments = parse("--steps=7", "-o", "out.png");

        assertEquals(7, arguments.intValue("--steps", 1, 0, 100));
        assertEquals(Optional.of("out.png"), arguments.value("--output"));
        assertEquals(3, parse("-n", "3").intValue("--steps", 1, 0, 100));
    }

    @Test
    void aDoubleDashEndsTheOptions() throws UsageException {
        assertEquals(List.of("--ascii", "-x"), parse("--", "--ascii", "-x").positionals());
        assertEquals(List.of("-5", "-"), parse("-5", "-").positionals());
    }

    @Test
    void fallsBackOnDefaults() throws UsageException {
        assertEquals(32, parse().intValue("--steps", 32, 0, 100));
        assertEquals(5L, parse().longValue("--steps", 5, 0, 100));
    }

    @Test
    void rejectsMalformedCommandLines() {
        assertEquals("unknown option --nope", assertThrows(UsageException.class, () -> parse("--nope")).getMessage());
        assertEquals("option --steps needs a value", assertThrows(UsageException.class, () -> parse("--steps")).getMessage());
        assertEquals(
                "option --steps is given twice",
                assertThrows(UsageException.class, () -> parse("--steps", "1", "-n", "2")).getMessage());
        assertEquals(
                "option --ascii does not take a value",
                assertThrows(UsageException.class, () -> parse("--ascii=yes")).getMessage());
    }

    @Test
    void validatesNumbers() {
        assertThrows(UsageException.class, () -> parse("--steps", "many").intValue("--steps", 1, 0, 100));
        assertThrows(UsageException.class, () -> parse("--steps", "101").intValue("--steps", 1, 0, 100));
        assertThrows(UsageException.class, () -> parse("--steps", "-1").intValue("--steps", 1, 0, 100));
    }

    @Test
    void checksTheNumberOfPositionals() throws UsageException {
        assertEquals("a.xml", parse("a.xml").single("file"));
        assertThrows(UsageException.class, () -> parse().single("file"));
        assertThrows(UsageException.class, () -> parse("a.xml", "b.xml").single("file"));
        assertThrows(UsageException.class, () -> parse("a.xml").requireNoPositional());
        parse("--ascii").requireNoPositional();
    }
}
