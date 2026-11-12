package fr.uspn.cellula.cli;

import java.util.List;
import java.util.Set;

import fr.uspn.cellula.config.AutomatonDefinition;
import fr.uspn.cellula.config.DefinitionException;
import fr.uspn.cellula.config.XmlDefinitionReader;
import fr.uspn.cellula.ui.swing.SwingApp;

/** {@code cellula run}: opens the simulation window of one definition file. */
final class RunCommand implements Command {

    @Override
    public String name() {
        return "run";
    }

    @Override
    public String summary() {
        return "Open the simulation window of one file";
    }

    @Override
    public String usage() {
        return """
                Usage: cellula run FILE

                Opens the simulation window of the automaton described by FILE, skipping the menu.

                In the window: Space plays or pauses, Right arrow advances by one generation,
                R resets, and clicking or dragging on the grid switches cells on and off.""";
    }

    @Override
    public int run(List<String> arguments, Console console)
            throws UsageException, DefinitionException {
        Arguments parsed = Arguments.parse(arguments, Set.of(), Set.of());
        AutomatonDefinition definition = XmlDefinitionReader.read(console.resolve(parsed.single("definition file")));
        if (!SwingApp.isAvailable()) {
            console.err().println(
                    "cellula: no display available; use 'cellula print' or 'cellula render' instead");
            return Cli.FAILURE;
        }
        SwingApp.showSimulation(definition);
        return Cli.SUCCESS;
    }
}
