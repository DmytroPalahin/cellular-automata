package fr.uspn.cellula.cli;

import fr.uspn.cellula.config.AutomatonDefinition;
import fr.uspn.cellula.config.DefinitionException;
import fr.uspn.cellula.config.XmlDefinitionReader;
import java.util.List;
import java.util.Set;

/** {@code cellula validate}: checks definition files and explains what is wrong with them. */
final class ValidateCommand implements Command {

    @Override
    public String name() {
        return "validate";
    }

    @Override
    public String summary() {
        return "Check definition files";
    }

    @Override
    public String usage() {
        return """
                Usage: cellula validate FILE...

                Checks each definition file: XML structure, consistency of the values, syntax of
                the rule. Problems are explained on standard error. The exit status is 0 if every
                file is valid and 1 otherwise, so the command can guard a commit or a build.""";
    }

    @Override
    public int run(List<String> arguments, Console console) throws UsageException {
        Arguments parsed = Arguments.parse(arguments, Set.of(), Set.of());
        if (parsed.positionals().isEmpty()) {
            throw new UsageException("expected at least one definition file");
        }
        int invalid = 0;
        for (String file : parsed.positionals()) {
            try {
                AutomatonDefinition definition = XmlDefinitionReader.read(console.resolve(file));
                console.out().println("ok    " + file + " - " + definition.name()
                        + " (" + definition.summary() + ")");
            } catch (DefinitionException e) {
                invalid++;
                console.out().println("FAIL  " + file);
                console.err().println(e.getMessage());
            }
        }
        return invalid == 0 ? Cli.SUCCESS : Cli.FAILURE;
    }
}
