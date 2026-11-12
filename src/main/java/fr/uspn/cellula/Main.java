package fr.uspn.cellula;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Path;

import fr.uspn.cellula.cli.Cli;
import fr.uspn.cellula.cli.Console;

/**
 * Entry point of the application.
 *
 * <p>
 * Without arguments the program shows the menu of the automaton definition
 * files found in the
 * current directory, as the subject requires. See {@code cellula help} for
 * everything else.
 */
public final class Main {

    private Main() {
    }

    /**
     * Runs the command line.
     *
     * @param args the arguments of the program
     */
    public static void main(String[] args) {
        Charset charset = System.console() != null ? System.console().charset() : Charset.defaultCharset();
        Console console = new Console(
                System.out,
                System.err,
                new BufferedReader(new InputStreamReader(System.in, charset)),
                Path.of("").toAbsolutePath());
        int status = new Cli(console).run(args);
        if (status != Cli.SUCCESS) {
            System.exit(status);
        }
        // On success the JVM ends by itself: at once for batch commands, and when the
        // last window
        // is closed for graphical ones.
    }
}
