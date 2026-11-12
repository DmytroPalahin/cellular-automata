package fr.uspn.cellula.cli;

import java.util.List;
import java.util.Set;

import fr.uspn.cellula.config.AutomatonDefinition;
import fr.uspn.cellula.config.DefinitionException;
import fr.uspn.cellula.config.XmlDefinitionReader;
import fr.uspn.cellula.render.TextRenderer;
import fr.uspn.cellula.ui.text.TextPlayer;

/**
 * {@code cellula print}: runs an automaton and draws the result with
 * characters.
 */
final class PrintCommand implements Command {

    static final int DEFAULT_STEPS = 32;
    static final int MAX_STEPS = 1_000_000;

    @Override
    public String name() {
        return "print";
    }

    @Override
    public String summary() {
        return "Run an automaton and print the result as text";
    }

    @Override
    public String usage() {
        return """
                Usage: cellula print FILE [--steps N] [--ascii]

                Runs the automaton described by FILE for N generations and prints the result on
                standard output. A one-dimensional automaton is shown as the juxtaposition of its
                generations 0 to N; any other as its displayed plane at generation N.

                Options:
                  -n, --steps N   number of generations to compute (default: 32)
                      --ascii     draw with '.' and '#' instead of Unicode blocks""";
    }

    @Override
    public int run(List<String> arguments, Console console)
            throws UsageException, DefinitionException {
        Arguments parsed = Arguments.parse(arguments, Set.of("--steps"), Set.of("--ascii"));
        AutomatonDefinition definition = XmlDefinitionReader.read(console.resolve(parsed.single("definition file")));
        int steps = parsed.intValue("--steps", DEFAULT_STEPS, 0, MAX_STEPS);
        TextRenderer renderer = parsed.has("--ascii") ? TextRenderer.ascii() : TextRenderer.unicode();
        TextPlayer.print(definition, steps, renderer, console.out());
        return Cli.SUCCESS;
    }
}
