package fr.uspn.cellula.rule.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import fr.uspn.cellula.neighborhood.Neighborhood;
import fr.uspn.cellula.neighborhood.NeighborhoodCatalog;
import fr.uspn.cellula.neighborhood.UnknownNeighborhoodException;
import fr.uspn.cellula.rule.expression.Constant;
import fr.uspn.cellula.rule.expression.Expression;
import fr.uspn.cellula.rule.parser.OperatorCatalog.NeighborhoodOperator;
import fr.uspn.cellula.rule.parser.OperatorCatalog.Signature;
import fr.uspn.cellula.rule.parser.OperatorCatalog.ValueOperator;

/**
 * Turns the text of a rule into a syntax tree.
 *
 * <p>
 * The language is the prefix notation of the subject:
 *
 * <pre>
 * rule         := expression END
 * expression   := NUMBER | call
 * call         := NAME '(' arguments ')'
 * arguments    := neighborhood                      -- for COMPTER
 *               | [ expression { ',' expression } ] -- for every other operator
 * neighborhood := NAME                              -- G0, G8*, ...
 * </pre>
 *
 * <p>
 * It is parsed by recursive descent, one method per production. Which
 * alternative of
 * {@code arguments} applies, and how many expressions are expected, is not
 * hard-wired: the parser
 * asks its {@link OperatorCatalog}. Neighborhood names are resolved against a
 * {@link NeighborhoodCatalog} while parsing, so a tree that parses is
 * guaranteed to be evaluable
 * on any grid of the catalog's dimension.
 *
 * <p>
 * A parser holds no per-parse state and can be reused.
 */
public final class RuleParser {

    /**
     * Nesting depth beyond which a rule is rejected rather than risking a stack
     * overflow.
     */
    private static final int MAX_DEPTH = 512;

    private final NeighborhoodCatalog neighborhoods;
    private final OperatorCatalog operators;

    /**
     * Creates a parser for the standard operators.
     *
     * @param neighborhoods the neighborhoods that rules may refer to
     */
    public RuleParser(NeighborhoodCatalog neighborhoods) {
        this(neighborhoods, OperatorCatalog.standard());
    }

    /**
     * Creates a parser.
     *
     * @param neighborhoods the neighborhoods that rules may refer to
     * @param operators     the operators of the language
     */
    public RuleParser(NeighborhoodCatalog neighborhoods, OperatorCatalog operators) {
        this.neighborhoods = Objects.requireNonNull(neighborhoods, "neighborhoods");
        this.operators = Objects.requireNonNull(operators, "operators");
    }

    /**
     * Parses a rule.
     *
     * @param source the text of the rule
     * @return the syntax tree
     * @throws RuleSyntaxException if the text is not a valid rule
     */
    public Expression parse(String source) {
        Objects.requireNonNull(source, "source");
        return new Run(source).rule();
    }

    /** The state of one parse: the tokens and a cursor. */
    private final class Run {

        private final String source;
        private final List<Token> tokens;
        private int cursor;
        private int depth;

        Run(String source) {
            this.source = source;
            this.tokens = new Lexer(source).tokenize();
        }

        Expression rule() {
            if (peek().type() == Token.Type.END) {
                throw error(peek(), "the rule is empty");
            }
            Expression expression = expression();
            if (peek().type() != Token.Type.END) {
                throw error(peek(), "unexpected " + peek().describe() + " after the end of the rule");
            }
            return expression;
        }

        private Expression expression() {
            Token token = peek();
            return switch (token.type()) {
                case NUMBER -> {
                    cursor++;
                    yield new Constant(Integer.parseInt(token.text()));
                }
                case NAME -> call();
                default -> throw error(
                        token, "expected a number or an operator but found " + token.describe());
            };
        }

        private Expression call() {
            Token name = tokens.get(cursor++);
            Signature signature = operators.find(name.text()).orElseThrow(() -> unknown(name));
            if (++depth > MAX_DEPTH) {
                throw error(name, "the rule is nested too deeply");
            }
            expect(Token.Type.LEFT_PAREN, "after " + name.text());
            Expression node = switch (signature) {
                case NeighborhoodOperator operator -> operator.factory().apply(neighborhood(operator));
                case ValueOperator operator -> {
                    List<Expression> arguments = arguments();
                    if (arguments.size() != operator.arity()) {
                        throw error(name, operator.symbol() + " expects " + operator.arity()
                                + " argument(s) but got " + arguments.size());
                    }
                    yield operator.factory().apply(arguments);
                }
            };
            expect(Token.Type.RIGHT_PAREN, "to close " + name.text() + "(");
            depth--;
            return node;
        }

        private List<Expression> arguments() {
            List<Expression> arguments = new ArrayList<>();
            if (peek().type() == Token.Type.RIGHT_PAREN) {
                return arguments;
            }
            arguments.add(expression());
            while (peek().type() == Token.Type.COMMA) {
                cursor++;
                arguments.add(expression());
            }
            return arguments;
        }

        private Neighborhood neighborhood(NeighborhoodOperator operator) {
            Token name = peek();
            if (name.type() != Token.Type.NAME) {
                throw error(name, operator.symbol() + " expects the name of a neighborhood, such as "
                        + neighborhoods.names().get(0) + ", but found " + name.describe());
            }
            cursor++;
            try {
                return neighborhoods.resolve(name.text());
            } catch (UnknownNeighborhoodException e) {
                throw error(name, e.getMessage());
            }
        }

        private RuleSyntaxException unknown(Token name) {
            if (NeighborhoodCatalog.isValidName(name.text())) {
                return error(name, "neighborhood " + name.text()
                        + " cannot be used as a value; count its cells with COMPTER("
                        + name.text() + ")");
            }
            return error(name, "unknown operator '" + name.text() + "' (known operators: "
                    + String.join(", ", operators.symbols()) + ")");
        }

        private void expect(Token.Type type, String context) {
            Token token = peek();
            if (token.type() != type) {
                throw error(token, "expected " + type.description() + " " + context
                        + " but found " + token.describe());
            }
            cursor++;
        }

        private Token peek() {
            return tokens.get(cursor);
        }

        private RuleSyntaxException error(Token token, String reason) {
            return new RuleSyntaxException(reason, source, token.position());
        }
    }
}
