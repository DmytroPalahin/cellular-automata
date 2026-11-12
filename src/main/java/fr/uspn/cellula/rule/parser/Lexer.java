package fr.uspn.cellula.rule.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits the source text of a rule into tokens.
 *
 * <p>
 * The lexical grammar is tiny:
 *
 * <pre>
 * NUMBER := '-'? DIGIT+
 * NAME   := LETTER (LETTER | DIGIT | '_')* '*'?
 * </pre>
 *
 * plus parentheses and commas. White space, including line breaks, separates
 * tokens and is
 * otherwise ignored, so long rules can be laid out over several lines. The
 * optional trailing star
 * belongs to the name, which is what makes {@code G8*} a single token.
 */
final class Lexer {

    private final String source;
    private int position;

    Lexer(String source) {
        this.source = source;
    }

    /**
     * Scans the whole source.
     *
     * @return the tokens, always terminated by a token of type
     *         {@link Token.Type#END}
     * @throws RuleSyntaxException if a character cannot start any token
     */
    List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (true) {
            skipWhitespace();
            if (position >= source.length()) {
                tokens.add(new Token(Token.Type.END, "", source.length()));
                return tokens;
            }
            tokens.add(next());
        }
    }

    private void skipWhitespace() {
        while (position < source.length() && Character.isWhitespace(source.charAt(position))) {
            position++;
        }
    }

    private Token next() {
        int start = position;
        char current = source.charAt(position);
        switch (current) {
            case '(':
                position++;
                return new Token(Token.Type.LEFT_PAREN, "(", start);
            case ')':
                position++;
                return new Token(Token.Type.RIGHT_PAREN, ")", start);
            case ',':
                position++;
                return new Token(Token.Type.COMMA, ",", start);
            default:
                break;
        }
        if (isDigit(current) || (current == '-' && isDigit(peek(1)))) {
            return number(start);
        }
        if (isLetter(current)) {
            return name(start);
        }
        throw new RuleSyntaxException("unexpected character '" + current + "'", source, start);
    }

    private Token number(int start) {
        position++; // first digit, or the minus sign
        while (position < source.length() && isDigit(source.charAt(position))) {
            position++;
        }
        String text = source.substring(start, position);
        try {
            Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new RuleSyntaxException(
                    "number " + text + " does not fit in a 32-bit integer", source, start);
        }
        return new Token(Token.Type.NUMBER, text, start);
    }

    private Token name(int start) {
        while (position < source.length() && isNamePart(source.charAt(position))) {
            position++;
        }
        if (position < source.length() && source.charAt(position) == '*') {
            position++;
        }
        return new Token(Token.Type.NAME, source.substring(start, position), start);
    }

    private char peek(int offset) {
        int index = position + offset;
        return index < source.length() ? source.charAt(index) : '\0';
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isLetter(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    private static boolean isNamePart(char c) {
        return isLetter(c) || isDigit(c) || c == '_';
    }
}
