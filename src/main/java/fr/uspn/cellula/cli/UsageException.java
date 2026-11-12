package fr.uspn.cellula.cli;

/**
 * Thrown when the command line itself is wrong: unknown option, missing
 * argument, bad value.
 */
final class UsageException extends Exception {

    private static final long serialVersionUID = 1L;

    UsageException(String message) {
        super(message);
    }
}
