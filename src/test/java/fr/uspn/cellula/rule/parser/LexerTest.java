package fr.uspn.cellula.rule.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class LexerTest {

    private static List<String> texts(String source) {
        return new Lexer(source).tokenize().stream().map(Token::text).toList();
    }

    private static List<Token.Type> types(String source) {
        return new Lexer(source).tokenize().stream().map(Token::type).toList();
    }

    @Test
    void splitsARuleIntoTokens() {
        assertEquals(
                List.of("SI", "(", "EQ", "(", "COMPTER", "(", "G8*", ")", ",", "2", ")", ",", "1", ",", "0", ")", ""),
                texts("SI(EQ(COMPTER(G8*), 2), 1, 0)"));
    }

    @Test
    void classifiesTokens() {
        assertEquals(
                List.of(
                        Token.Type.NAME, Token.Type.LEFT_PAREN, Token.Type.NUMBER, Token.Type.COMMA,
                        Token.Type.NAME, Token.Type.RIGHT_PAREN, Token.Type.END),
                types("ADD(-12, G0)"));
    }

    @Test
    void theStarBelongsToTheName() {
        assertEquals(List.of("G26*", ""), texts("G26*"));
        assertEquals(List.of("G4", ""), texts("G4"));
    }

    @Test
    void whiteSpaceAndLineBreaksAreInsignificant() {
        assertEquals(texts("SI(1,2,3)"), texts("  SI (\n\t1 ,\r\n 2 , 3\n)  "));
    }

    @Test
    void recordsWhereEachTokenStarts() {
        List<Token> tokens = new Lexer("EQ( 10,G2)").tokenize();

        assertEquals(List.of(0, 2, 4, 6, 7, 9, 10), tokens.stream().map(Token::position).toList());
    }

    @Test
    void anEmptySourceIsJustTheEndMarker() {
        assertEquals(List.of(Token.Type.END), types(""));
        assertEquals(List.of(Token.Type.END), types("  \n "));
    }

    @Test
    void rejectsCharactersOutsideTheLanguage() {
        RuleSyntaxException error =
                assertThrows(RuleSyntaxException.class, () -> new Lexer("ADD(1 + 2)").tokenize());

        assertEquals("unexpected character '+'", error.reason());
        assertEquals(6, error.position());
        assertThrows(RuleSyntaxException.class, () -> new Lexer("SUB(1, -)").tokenize());
        assertThrows(RuleSyntaxException.class, () -> new Lexer("*").tokenize());
    }

    @Test
    void rejectsNumbersThatDoNotFitInAnInt() {
        RuleSyntaxException error =
                assertThrows(RuleSyntaxException.class, () -> new Lexer("99999999999").tokenize());

        assertEquals(0, error.position());
    }
}
