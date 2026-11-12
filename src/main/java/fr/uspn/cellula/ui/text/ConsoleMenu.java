package fr.uspn.cellula.ui.text;

import fr.uspn.cellula.config.AutomatonDefinition;
import fr.uspn.cellula.config.DefinitionLibrary;
import fr.uspn.cellula.config.DefinitionLibrary.Entry;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The start-up menu in a terminal: lists the automaton definition files of a directory and
 * launches the simulation of the one the user picks, again and again until the user quits.
 */
public final class ConsoleMenu {

    /** What "launching a simulation" means; decided by the caller (a window, or text output). */
    @FunctionalInterface
    public interface Launcher {

        /**
         * Runs the simulation of an automaton and returns once the user is done with it.
         *
         * @param definition the automaton chosen by the user
         */
        void launch(AutomatonDefinition definition);
    }

    private final Path directory;
    private final BufferedReader in;
    private final PrintStream out;
    private final Launcher launcher;

    /**
     * Creates the menu.
     *
     * @param directory the directory whose definition files are listed
     * @param in where the choices of the user are read from
     * @param out where the menu is printed
     * @param launcher what to do with the chosen automaton
     */
    public ConsoleMenu(Path directory, BufferedReader in, PrintStream out, Launcher launcher) {
        this.directory = directory.toAbsolutePath().normalize();
        this.in = in;
        this.out = out;
        this.launcher = launcher;
    }

    /**
     * Shows the menu until the user quits or the input ends.
     *
     * @throws IOException if the directory cannot be listed or the input cannot be read
     */
    public void run() throws IOException {
        while (true) {
            List<Entry.Loaded> choices = show(DefinitionLibrary.scan(directory));
            out.print(choices.isEmpty()
                    ? "[r] rescan, [q] quit > "
                    : "Number of the automaton to run, [r] rescan, [q] quit > ");
            out.flush();
            String answer = in.readLine();
            if (answer == null) {
                out.println();
                return;
            }
            answer = answer.strip().toLowerCase(Locale.ROOT);
            if (answer.equals("q") || answer.equals("quit") || answer.equals("exit")) {
                return;
            }
            if (answer.isEmpty() || answer.equals("r")) {
                continue;
            }
            try {
                int choice = Integer.parseInt(answer);
                if (choice >= 1 && choice <= choices.size()) {
                    launcher.launch(choices.get(choice - 1).definition());
                    continue;
                }
            } catch (NumberFormatException e) {
                // Reported below, like an out-of-range number.
            }
            out.println("'" + answer + "' is not one of the choices.");
        }
    }

    /** Prints the list and returns the entries that can be chosen, in menu order. */
    private List<Entry.Loaded> show(List<Entry> entries) {
        out.println();
        out.println("Automaton definitions in " + directory);
        out.println();
        List<Entry.Loaded> choices = new ArrayList<>();
        if (entries.isEmpty()) {
            out.println("  (none: a definition is an XML file whose root element is <automaton>)");
        }
        for (Entry entry : entries) {
            switch (entry) {
                case Entry.Loaded loaded -> {
                    choices.add(loaded);
                    AutomatonDefinition definition = loaded.definition();
                    out.printf(Locale.ROOT, "  [%d] %-28s %-26s %dD  %s%n",
                            choices.size(),
                            loaded.fileName(),
                            definition.name(),
                            definition.dimension(),
                            definition.shape());
                }
                case Entry.Broken broken -> {
                    out.printf(Locale.ROOT, "  [!] %-28s invalid: %s%n",
                            broken.fileName(), firstLine(broken.problem()));
                }
            }
        }
        out.println();
        return choices;
    }

    private static String firstLine(String text) {
        int end = text.indexOf('\n');
        return end < 0 ? text : text.substring(0, end);
    }
}
