package fr.uspn.cellula.cli;

import fr.uspn.cellula.config.AutomatonDefinition;
import fr.uspn.cellula.render.TextRenderer;
import fr.uspn.cellula.ui.swing.SwingApp;
import fr.uspn.cellula.ui.text.ConsoleMenu;
import fr.uspn.cellula.ui.text.TextPlayer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionException;

/** {@code cellula menu}: the start-up menu of the definition files of a directory. */
final class MenuCommand implements Command {

    /** Number of generations shown when a simulation has to be played as text. */
    private static final int TEXT_STEPS = 32;

    @Override
    public String name() {
        return "menu";
    }

    @Override
    public String summary() {
        return "List the automaton files of a directory and launch one (default)";
    }

    @Override
    public String usage() {
        return """
                Usage: cellula [menu] [--dir DIR] [--text]

                Lists every automaton definition file of a directory and lets you pick the one to
                simulate. The menu is a window; on a machine without a display, or with --text, it
                is shown in the terminal instead.

                Options:
                  -d, --dir DIR   directory to list (default: the current directory)
                      --text      show the menu in the terminal""";
    }

    @Override
    public int run(List<String> arguments, Console console) throws UsageException, IOException {
        Arguments parsed = Arguments.parse(arguments, Set.of("--dir"), Set.of("--text"));
        parsed.requireNoPositional();
        Path directory = parsed.value("--dir").map(console::resolve).orElse(console.workingDirectory());
        if (!Files.isDirectory(directory)) {
            throw new UsageException(directory + " is not a directory");
        }
        if (SwingApp.isAvailable() && !parsed.has("--text")) {
            SwingApp.showLauncher(directory);
            return Cli.SUCCESS;
        }
        new ConsoleMenu(directory, console.in(), console.out(), definition -> launch(definition, console))
                .run();
        return Cli.SUCCESS;
    }

    /** Opens a window and waits for it to be closed, or falls back to text without a display. */
    private static void launch(AutomatonDefinition definition, Console console) {
        if (SwingApp.isAvailable()) {
            console.out().println("Simulating " + definition.name() + " - close its window to come back here.");
            try {
                SwingApp.showSimulation(definition).join();
            } catch (CompletionException e) {
                console.err().println("cellula: " + e.getCause().getMessage());
            }
        } else {
            TextPlayer.print(definition, TEXT_STEPS, TextRenderer.unicode(), console.out());
        }
    }
}
