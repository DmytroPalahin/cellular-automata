package fr.uspn.cellula.rule.parser;

import fr.uspn.cellula.neighborhood.Neighborhood;
import fr.uspn.cellula.rule.expression.Add;
import fr.uspn.cellula.rule.expression.And;
import fr.uspn.cellula.rule.expression.Conditional;
import fr.uspn.cellula.rule.expression.Count;
import fr.uspn.cellula.rule.expression.CurrentState;
import fr.uspn.cellula.rule.expression.Equal;
import fr.uspn.cellula.rule.expression.Expression;
import fr.uspn.cellula.rule.expression.GreaterOrEqual;
import fr.uspn.cellula.rule.expression.GreaterThan;
import fr.uspn.cellula.rule.expression.Multiply;
import fr.uspn.cellula.rule.expression.Not;
import fr.uspn.cellula.rule.expression.Or;
import fr.uspn.cellula.rule.expression.Subtract;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * The operators of the rule language: for each keyword, how many arguments it takes, of which
 * kind, and how to build the corresponding syntax-tree node.
 *
 * <p>The catalog is the single place where keywords are bound to classes. The parser contains no
 * knowledge of any particular operator, so adding one to the language takes a class and a line in
 * {@link #standard()}.
 *
 * <p>Keywords are case-insensitive. Each operator has the French keyword of the subject as its
 * canonical name and, where it differs, an English alias.
 */
public final class OperatorCatalog {

    /** Describes how to parse and build one operator. */
    public sealed interface Signature {

        /** Returns the canonical keyword of the operator. */
        String symbol();
    }

    /**
     * An operator applied to a fixed number of values.
     *
     * @param symbol canonical keyword
     * @param arity number of arguments
     * @param factory builds the node from exactly {@code arity} sub-expressions
     */
    public record ValueOperator(
            String symbol, int arity, Function<List<Expression>, Expression> factory)
            implements Signature {}

    /**
     * An operator applied to one neighborhood.
     *
     * @param symbol canonical keyword
     * @param factory builds the node from the neighborhood
     */
    public record NeighborhoodOperator(String symbol, Function<Neighborhood, Expression> factory)
            implements Signature {}

    private final Map<String, Signature> byKeyword = new LinkedHashMap<>();
    private final List<String> symbols = new ArrayList<>();

    /** Creates an empty catalog. */
    public OperatorCatalog() {}

    /** Returns a new catalog holding the operators of the subject and the documented extensions. */
    public static OperatorCatalog standard() {
        OperatorCatalog catalog = new OperatorCatalog();
        catalog.register(binary(And.SYMBOL, And::new), "AND");
        catalog.register(binary(Or.SYMBOL, Or::new), "OR");
        catalog.register(new ValueOperator(Not.SYMBOL, 1, args -> new Not(args.get(0))), "NOT");
        catalog.register(binary(GreaterThan.SYMBOL, GreaterThan::new), "GT");
        catalog.register(binary(GreaterOrEqual.SYMBOL, GreaterOrEqual::new), "GE");
        catalog.register(binary(Equal.SYMBOL, Equal::new));
        catalog.register(new NeighborhoodOperator(Count.SYMBOL, Count::new), "COUNT");
        catalog.register(binary(Add.SYMBOL, Add::new));
        catalog.register(binary(Subtract.SYMBOL, Subtract::new));
        catalog.register(binary(Multiply.SYMBOL, Multiply::new));
        catalog.register(
                new ValueOperator(
                        Conditional.SYMBOL,
                        3,
                        args -> new Conditional(args.get(0), args.get(1), args.get(2))),
                "IF");
        catalog.register(
                new ValueOperator(CurrentState.SYMBOL, 0, args -> new CurrentState()), "STATE");
        return catalog;
    }

    private static ValueOperator binary(
            String symbol, BiFunction<Expression, Expression, Expression> constructor) {
        return new ValueOperator(symbol, 2, args -> constructor.apply(args.get(0), args.get(1)));
    }

    /**
     * Adds an operator to the catalog.
     *
     * @param signature how to parse and build the operator
     * @param aliases additional keywords accepted for the same operator
     * @return this catalog, for chaining
     * @throws IllegalArgumentException if one of the keywords is already taken
     */
    public OperatorCatalog register(Signature signature, String... aliases) {
        Objects.requireNonNull(signature, "signature");
        bind(signature.symbol(), signature);
        for (String alias : aliases) {
            bind(alias, signature);
        }
        symbols.add(signature.symbol());
        return this;
    }

    private void bind(String keyword, Signature signature) {
        if (byKeyword.putIfAbsent(normalize(keyword), signature) != null) {
            throw new IllegalArgumentException("operator keyword " + keyword + " is already taken");
        }
    }

    /** Looks an operator up by keyword or alias, ignoring letter case. */
    public Optional<Signature> find(String keyword) {
        return Optional.ofNullable(byKeyword.get(normalize(keyword)));
    }

    /** Returns the canonical keywords, in registration order. */
    public List<String> symbols() {
        return List.copyOf(symbols);
    }

    private static String normalize(String keyword) {
        return keyword.toUpperCase(Locale.ROOT);
    }
}
