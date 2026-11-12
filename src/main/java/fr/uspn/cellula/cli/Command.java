package fr.uspn.cellula.cli;

import java.io.IOException;
import java.util.List;

import fr.uspn.cellula.config.DefinitionException;

/**
 * One sub-command of the command-line interface, such as {@code render} or
 * {@code explore}.
 */
interface Command {

    /** Returns the word that selects this command. */
    String name();

    /** Returns a one-line description, shown in the list of commands. */
    String summary();

    /** Returns the detailed usage of the command: synopsis and options. */
    String usage();

    /**
     * Runs the command.
     *
     * @param arguments the words following the command name
     * @param console   where to read from and print to
     * @return the process exit status: 0 for success
     * @throws UsageException      if the arguments are wrong
     * @throws DefinitionException if a definition file is invalid
     * @throws IOException         if a file cannot be read or written
     */
    int run(List<String> arguments, Console console)
            throws UsageException, DefinitionException, IOException;
}
