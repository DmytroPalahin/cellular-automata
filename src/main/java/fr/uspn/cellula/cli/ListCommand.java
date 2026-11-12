package fr.uspn.cellula.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import fr.uspn.cellula.config.DefinitionLibrary;
import fr.uspn.cellula.config.DefinitionLibrary.Entry;

/**
 * {@code cellula list}: prints the definition files of a directory, without
 * interaction.
 */
final class ListCommand implements Command {

    @Override
    public String name() {
        return "list";
    }

    @Override
    public String summary() {
        return "Print the automaton files of a directory";
    }

    @Override
    public String usage() {
        return """
                Usage: cellula list [--dir DIR]

                Prints one line per automaton definition file found in a directory.

                Options:
                  -d, --dir DIR   directory to list (default: the current directory)""";
    }

    @Override
    public int run(List<String> arguments, Console console) throws UsageException, IOException {
        Arguments parsed = Arguments.parse(arguments, Set.of("--dir"), Set.of());
        parsed.requireNoPositional();
        Path directory = parsed.value("--dir").map(console::resolve).orElse(console.workingDirectory());
        if (!Files.isDirectory(directory)) {
            throw new UsageException(directory + " is not a directory");
        }
        for (Entry entry : DefinitionLibrary.scan(directory)) {
            switch (entry) {
                case Entry.Loaded loaded -> console.out().printf(Locale.ROOT, "%-28s %-26s %dD  %s%n",
                        loaded.fileName(),
                        loaded.definition().name(),
                        loaded.definition().dimension(),
                        loaded.definition().shape());
                case Entry.Broken broken -> console.out().printf(Locale.ROOT, "%-28s INVALID%n",
                        broken.fileName());
            }
        }
        return Cli.SUCCESS;
    }
}
