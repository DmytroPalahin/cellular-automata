package fr.uspn.cellula.config;

import fr.uspn.cellula.grid.Coordinates;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser for the set-of-tuples notation used by definition files, as in
 * {@code {(0, 0, 0), (1, 1, 0), (-1, 0, 0)}}.
 *
 * <pre>
 * list  := '{' items '}' | items
 * items := [ tuple { ',' tuple } ]
 * tuple := '(' INT { ',' INT } ')' | INT
 * </pre>
 *
 * <p>The surrounding braces are optional, and so are the parentheses around a one-dimensional
 * coordinate: {@code -2, -1, 1, 2} reads as four positions on a line.
 */
final class CoordinateLists {

    private final String text;
    private int position;

    private CoordinateLists(String text) {
        this.text = text;
    }

    /**
     * Parses a list of coordinates.
     *
     * @param text the textual list; may be blank, meaning the empty list
     * @return the coordinates, in source order
     * @throws IllegalArgumentException if the text is malformed
     */
    static List<Coordinates> parse(String text) {
        return new CoordinateLists(text).list();
    }

    private List<Coordinates> list() {
        List<Coordinates> coordinates = new ArrayList<>();
        boolean braces = accept('{');
        if (!atEnd() && peek() != '}') {
            coordinates.add(tuple());
            while (accept(',')) {
                coordinates.add(tuple());
            }
        }
        if (braces) {
            expect('}');
        }
        skipWhitespace();
        if (!atEnd()) {
            throw error("unexpected '" + text.charAt(position) + "'");
        }
        return coordinates;
    }

    private Coordinates tuple() {
        if (!accept('(')) {
            return Coordinates.of(integer());
        }
        List<Integer> components = new ArrayList<>();
        components.add(integer());
        while (accept(',')) {
            components.add(integer());
        }
        expect(')');
        return Coordinates.of(components.stream().mapToInt(Integer::intValue).toArray());
    }

    private int integer() {
        skipWhitespace();
        int start = position;
        if (position < text.length() && (text.charAt(position) == '-' || text.charAt(position) == '+')) {
            position++;
        }
        while (position < text.length() && Character.isDigit(text.charAt(position))) {
            position++;
        }
        try {
            return Integer.parseInt(text.substring(start, position));
        } catch (NumberFormatException e) {
            position = start;
            throw error("expected an integer");
        }
    }

    private boolean accept(char expected) {
        skipWhitespace();
        if (position < text.length() && text.charAt(position) == expected) {
            position++;
            return true;
        }
        return false;
    }

    private void expect(char expected) {
        if (!accept(expected)) {
            throw error("expected '" + expected + "'");
        }
    }

    private char peek() {
        skipWhitespace();
        return position < text.length() ? text.charAt(position) : '\0';
    }

    private boolean atEnd() {
        skipWhitespace();
        return position >= text.length();
    }

    private void skipWhitespace() {
        while (position < text.length() && Character.isWhitespace(text.charAt(position))) {
            position++;
        }
    }

    private IllegalArgumentException error(String problem) {
        String where = position >= text.length()
                ? "at the end"
                : "at '" + abbreviate(text.substring(position)) + "'";
        return new IllegalArgumentException(
                "invalid coordinate list: " + problem + " " + where);
    }

    private static String abbreviate(String remainder) {
        String flat = remainder.strip().replaceAll("\\s+", " ");
        return flat.length() <= 20 ? flat : flat.substring(0, 20) + "…";
    }
}
