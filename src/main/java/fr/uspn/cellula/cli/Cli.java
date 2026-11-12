package fr.uspn.cellula.cli;

import fr.uspn.cellula.config.DefinitionException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * The command-line interface: finds the sub-command, runs it, and turns failures into messages
 * and exit statuses.
 *
 * <p>Exit statuses follow the usual convention: 0 for success, 1 when the command could not do
 * its job (invalid definition, unreadable file), 2 when the command line itself is wrong.
 */
public final class Cli {

    /** Exit status of a successful command. */
    public static final int SUCCESS = 0;

    /** Exit status of a command that failed on its input. */
    public static final int FAILURE = 1;

    /** Exit status of a malformed command line. */
    public static final int USAGE = 2;

    private static final String DEFAULT_COMMAND = "menu";

    private final Console console;
    private final List<Command> commands = List.of(
            new MenuCommand(),
            new RunCommand(),
            new ListCommand(),
            new ValidateCommand(),
            new PrintCommand(),
            new RenderCommand(),
            new ExploreCommand());

    /**
     * Creates the interface.
     *
     * @param console where commands read from and print to
     */
    public Cli(Console console) {
        this.console = console;
    }

    /** Returns the version of the application, or {@code "dev"} when not run from a release jar. */
    public static String version() {
        String version = Cli.class.getPackage().getImplementationVersion();
        return version != null ? version : "dev";
    }

    /**
     * Executes a command line.
     *
     * @param args the arguments of the program
     * @return the exit status
     */
    public int run(String... args) {
        List<String> words = Arrays.asList(args);
        String name = DEFAULT_COMMAND;
        List<String> rest = words;
        if (!words.isEmpty() && !words.get(0).startsWith("-")) {
            name = words.get(0);
            rest = words.subList(1, words.size());
        } else if (!words.isEmpty() && isHelp(words.get(0))) {
            name = "help";
            rest = words.subList(1, words.size());
        } else if (!words.isEmpty() && words.get(0).equals("--version")) {
            name = "version";
        }

        switch (name) {
            case "help":
                return help(rest);
            case "version":
                console.out().println("Cellula " + version());
                return SUCCESS;
            default:
                break;
        }

        Optional<Command> command = find(name);
        if (command.isEmpty()) {
            console.err().println("cellula: unknown command '" + name + "'");
            console.err().println("Run 'cellula help' for the list of commands.");
            return USAGE;
        }
        if (rest.stream().anyMatch(Cli::isHelp)) {
            console.out().println(command.get().usage());
            return SUCCESS;
        }
        try {
            return command.get().run(rest, console);
        } catch (UsageException e) {
            console.err().println("cellula " + name + ": " + e.getMessage());
            console.err().println("Run 'cellula help " + name + "' for details.");
            return USAGE;
        } catch (DefinitionException | IOException e) {
            console.err().println("cellula: " + e.getMessage());
            return FAILURE;
        }
    }

    private static boolean isHelp(String word) {
        return word.equals("--help") || word.equals("-h");
    }

    private Optional<Command> find(String name) {
        return commands.stream().filter(command -> command.name().equals(name)).findFirst();
    }

    private int help(List<String> topics) {
        if (!topics.isEmpty()) {
            Optional<Command> command = find(topics.get(0));
            if (command.isEmpty()) {
                console.err().println("cellula: unknown command '" + topics.get(0) + "'");
                return USAGE;
            }
            console.out().println(command.get().usage());
            return SUCCESS;
        }
        StringBuilder text = new StringBuilder();
        text.append("Cellula ").append(version())
                .append(" - a d-dimensional cellular automaton engine\n\n");
        text.append("Usage: cellula [command] [options]\n\n");
        text.append("Commands:\n");
        for (Command command : commands) {
            text.append(String.format("  %-10s %s%n", command.name(), command.summary()));
        }
        text.append(String.format("  %-10s %s%n", "help", "Show this help, or the help of a command"));
        text.append(String.format("  %-10s %s%n", "version", "Print the version"));
        text.append("\nWithout a command, 'menu' is run on the current directory.\n");
        text.append("Run 'cellula help <command>' for the options of a command.");
        console.out().println(text);
        return SUCCESS;
    }
}
