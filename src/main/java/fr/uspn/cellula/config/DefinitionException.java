package fr.uspn.cellula.config;

/**
 * Thrown when a definition file cannot be turned into an automaton: it is
 * unreadable, is not
 * well-formed XML, does not follow the schema, or describes something
 * inconsistent (a rule that
 * does not parse, an initial cell outside the grid, ...).
 *
 * <p>
 * This is a checked exception because a bad input file is an expected situation
 * that every
 * caller has to handle, typically by showing the message to the user.
 */
public final class DefinitionException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message what is wrong with the file, phrased for the end user
     */
    public DefinitionException(String message) {
        super(message);
    }

    /**
     * Creates the exception.
     *
     * @param message what is wrong with the file, phrased for the end user
     * @param cause   the underlying failure
     */
    public DefinitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
