package fr.uspn.cellula.ui.text;

import fr.uspn.cellula.automaton.CellularAutomaton;
import fr.uspn.cellula.config.AutomatonDefinition;
import fr.uspn.cellula.config.AutomatonFactory;
import fr.uspn.cellula.grid.Frame;
import fr.uspn.cellula.render.TextRenderer;
import fr.uspn.cellula.simulation.Projection;
import java.io.PrintStream;

/**
 * Runs an automaton without any window and produces its picture: the display of last resort on a
 * machine without a screen, and the workhorse of the {@code print} and {@code render} commands.
 */
public final class TextPlayer {

    private TextPlayer() {}

    /**
     * Runs an automaton for a number of generations and returns what a window would show at the
     * end: the selected plane for a grid of two dimensions or more, the whole space-time diagram
     * for a one-dimensional grid.
     *
     * @param definition the automaton to run
     * @param steps number of generations to compute
     * @return the final picture
     */
    public static Frame play(AutomatonDefinition definition, int steps) {
        CellularAutomaton automaton = AutomatonFactory.create(definition);
        Projection projection = Projection.of(definition);
        projection.restarted(automaton.grid());
        for (int step = 0; step < steps; step++) {
            automaton.step();
            projection.advanced(automaton.grid());
        }
        return projection.frame(automaton.grid());
    }

    /**
     * Runs an automaton and prints the result as text.
     *
     * @param definition the automaton to run
     * @param steps number of generations to compute
     * @param renderer how to draw cells
     * @param out where to print
     */
    public static void print(
            AutomatonDefinition definition, int steps, TextRenderer renderer, PrintStream out) {
        out.println(definition.name() + " (" + definition.summary() + ")");
        if (definition.dimension() == 1) {
            out.println("Generations 0 to " + steps + ", one per line:");
        } else if (definition.dimension() == 2) {
            out.println("Generation " + steps + ":");
        } else {
            out.println("Generation " + steps + ", slice " + definition.slice() + ":");
        }
        out.print(renderer.render(play(definition, steps)));
    }
}
