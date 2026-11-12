package fr.uspn.cellula.rule.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import fr.uspn.cellula.grid.Coordinates;
import fr.uspn.cellula.neighborhood.NeighborhoodCatalog;
import fr.uspn.cellula.neighborhood.Neighborhoods;
import fr.uspn.cellula.rule.expression.Add;
import fr.uspn.cellula.rule.expression.Conditional;
import fr.uspn.cellula.rule.expression.Constant;
import fr.uspn.cellula.rule.expression.Count;
import fr.uspn.cellula.rule.expression.CurrentState;
import fr.uspn.cellula.rule.expression.Equal;
import fr.uspn.cellula.rule.expression.Expression;
import fr.uspn.cellula.rule.expression.GreaterOrEqual;
import fr.uspn.cellula.rule.expression.Not;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class RuleParserTest {

    private final RuleParser parser = new RuleParser(new NeighborhoodCatalog(2));

    /** The complete formula given at the end of section 2.3 of the subject. */
    private static final String SUBJECT_FORMULA = """
            SI(EQ(COMPTER(G0), 1),
               SI(SUPEQ(COMPTER(G8), 4), 0, 1),
               SI(EQ(COMPTER(G8), 2), 1, 0))""";

    @Test
    void parsesNumbers() {
        assertEquals(new Constant(42), parser.parse("42"));
        assertEquals(new Constant(-7), parser.parse(" -7 "));
        assertEquals(new Constant(0), parser.parse("0"));
    }

    @Test
    void parsesNestedOperators() {
        Expression expected = new Add(new Constant(1), new Not(new Constant(0)));

        assertEquals(expected, parser.parse("ADD(1, NON(0))"));
    }

    @Test
    void resolvesNeighborhoodsWhileParsing() {
        Count count = assertInstanceOf(Count.class, parser.parse("COMPTER(G8*)"));

        assertEquals(Neighborhoods.punctured(Neighborhoods.moore(2)), count.neighborhood());
    }

    @Test
    void parsesTheFormulaOfTheSubject() {
        Expression g0 = new Count(Neighborhoods.self(2));
        Expression g8 = new Count(Neighborhoods.moore(2));
        Expression expected = new Conditional(
                new Equal(g0, new Constant(1)),
                new Conditional(new GreaterOrEqual(g8, new Constant(4)), new Constant(0), new Constant(1)),
                new Conditional(new Equal(g8, new Constant(2)), new Constant(1), new Constant(0)));

        assertEquals(expected, parser.parse(SUBJECT_FORMULA));
    }

    @Test
    void printsRulesInCanonicalForm() {
        assertEquals(
                "SI(EQ(COMPTER(G0), 1), SI(SUPEQ(COMPTER(G8), 4), 0, 1), SI(EQ(COMPTER(G8), 2), 1, 0))",
                parser.parse(SUBJECT_FORMULA).toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "ET(OU(1, 0), NON(SUP(3, 2)))",
        "MUL(SUB(ADD(1, 2), -3), SUPEQ(COMPTER(G4*), EQ(1, 1)))",
        "SI(ETAT(), COMPTER(G4), COMPTER(G0))",
    })
    void printingThenParsingGivesTheSameTree(String source) {
        Expression tree = parser.parse(source);

        assertEquals(source, tree.toString());
        assertEquals(tree, parser.parse(tree.toString()));
    }

    @Test
    void keywordsAreCaseInsensitiveAndHaveEnglishAliases() {
        assertEquals(
                parser.parse("SI(ET(OU(1, 0), NON(0)), SUP(2, 1), SUPEQ(COMPTER(G4), ETAT()))"),
                parser.parse("if(and(or(1, 0), not(0)), gt(2, 1), ge(count(G4), state()))"));
        assertEquals(new CurrentState(), parser.parse("Etat()"));
    }

    @Test
    void usesUserDefinedNeighborhoods() {
        NeighborhoodCatalog catalog = new NeighborhoodCatalog(1);
        catalog.define("G11", List.of(Coordinates.of(-1)));

        Count count = assertInstanceOf(Count.class, new RuleParser(catalog).parse("COMPTER(G11)"));

        assertEquals(1, count.neighborhood().size());
    }

    static Stream<Arguments> invalidRules() {
        return Stream.of(
                arguments("", 0, "the rule is empty"),
                arguments("ADD(1, 2) 3", 10, "unexpected '3' after the end of the rule"),
                arguments("ADD(1, 2))", 9, "unexpected ')' after the end of the rule"),
                arguments("ADD(1, 2", 8, "expected ')' to close ADD( but found the end of the rule"),
                arguments("ADD 1, 2)", 4, "expected '(' after ADD but found '1'"),
                arguments("ADD(1)", 0, "ADD expects 2 argument(s) but got 1"),
                arguments("SI(1, 2)", 0, "SI expects 3 argument(s) but got 2"),
                arguments("NON(1, 2)", 0, "NON expects 1 argument(s) but got 2"),
                arguments("ETAT(1)", 0, "ETAT expects 0 argument(s) but got 1"),
                arguments("ADD(1, )", 7, "expected a number or an operator but found ')'"),
                arguments("ADD(, 1)", 4, "expected a number or an operator but found ','"),
                arguments("FOO(1)", 0, "unknown operator 'FOO'"),
                arguments("ADD(1, G8)", 7, "neighborhood G8 cannot be used as a value"),
                arguments("COMPTER(3)", 8, "COMPTER expects the name of a neighborhood"),
                arguments("COMPTER(G8, G4)", 10, "expected ')' to close COMPTER( but found ','"),
                arguments("COMPTER(G26)", 8, "unknown neighborhood 'G26' for a 2-dimensional grid"),
                arguments("COMPTER(ADD(1, 2))", 8, "unknown neighborhood 'ADD'"),
                arguments("(1)", 0, "expected a number or an operator but found '('"));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidRules")
    void reportsWhatIsWrongAndWhere(String source, int position, String reason) {
        RuleSyntaxException error = assertThrows(RuleSyntaxException.class, () -> parser.parse(source));

        assertTrue(error.reason().startsWith(reason), error.reason());
        assertEquals(position, error.position());
    }

    @Test
    void rejectsAbsurdlyDeepNesting() {
        String deep = "NON(".repeat(2000) + "1" + ")".repeat(2000);

        RuleSyntaxException error = assertThrows(RuleSyntaxException.class, () -> parser.parse(deep));

        assertEquals("the rule is nested too deeply", error.reason());
    }

    @Test
    void theErrorMessagePointsAtTheCulprit() {
        RuleSyntaxException error = assertThrows(
                RuleSyntaxException.class,
                () -> parser.parse("SI(EQ(COMPTER(G0), 1),\n   COMPTER(G9),\n   0)"));

        assertEquals(
                """
                unknown neighborhood 'G9' for a 2-dimensional grid (available: G0, G4, G4*, G8, G8*) (line 2, column 12)
                       COMPTER(G9),
                               ^""",
                error.getMessage());
        assertEquals("SI(EQ(COMPTER(G0), 1),\n   COMPTER(G9),\n   0)", error.source());
    }
}
