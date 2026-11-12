package fr.uspn.cellula.cli;

import fr.uspn.cellula.config.AutomatonDefinition;
import fr.uspn.cellula.config.DefinitionException;
import fr.uspn.cellula.config.XmlDefinitionReader;
import fr.uspn.cellula.grid.Frame;
import fr.uspn.cellula.render.FrameImages;
import fr.uspn.cellula.render.Palette;
import fr.uspn.cellula.ui.text.TextPlayer;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/** {@code cellula render}: runs an automaton and saves the result as a PNG image. */
final class RenderCommand implements Command {

    /** Width or height, in pixels, the automatic cell size aims for. */
    private static final int TARGET_PIXELS = 1024;

    private static final int MAX_CELL_SIZE = 32;

    @Override
    public String name() {
        return "render";
    }

    @Override
    public String summary() {
        return "Run an automaton and save the result as a PNG image";
    }

    @Override
    public String usage() {
        return """
                Usage: cellula render FILE --output IMAGE.png [--steps N] [--cell-size PX] [--no-grid]

                Runs the automaton described by FILE for N generations, without opening any
                window, and saves what the window would show. A one-dimensional automaton gives
                the juxtaposition of its generations 0 to N; any other its displayed plane at
                generation N.

                Options:
                  -o, --output IMAGE   the PNG file to write (required)
                  -n, --steps N        number of generations to compute (default: 32)
                      --cell-size PX   side of a cell in pixels (default: chosen so that the
                                       image is about 1000 pixels across)
                      --no-grid        do not draw lines between cells""";
    }

    @Override
    public int run(List<String> arguments, Console console)
            throws UsageException, DefinitionException, IOException {
        Arguments parsed = Arguments.parse(
                arguments, Set.of("--output", "--steps", "--cell-size"), Set.of("--no-grid"));
        AutomatonDefinition definition =
                XmlDefinitionReader.read(console.resolve(parsed.single("definition file")));
        Path output = console.resolve(parsed.value("--output")
                .orElseThrow(() -> new UsageException("option --output is required")));
        int steps = parsed.intValue("--steps", PrintCommand.DEFAULT_STEPS, 0, PrintCommand.MAX_STEPS);

        Frame frame = TextPlayer.play(definition, steps);
        int automatic = Math.max(1, Math.min(
                MAX_CELL_SIZE, TARGET_PIXELS / Math.max(frame.rows(), frame.columns())));
        int cellSize = parsed.intValue("--cell-size", automatic, 1, 256);
        FrameImages.writePng(
                FrameImages.render(frame, Palette.of(definition.colors()), cellSize, !parsed.has("--no-grid")),
                output);
        console.out().println("Wrote " + output + " (" + frame.columns() * cellSize + " x "
                + frame.rows() * cellSize + " pixels, generation " + steps + ")");
        return Cli.SUCCESS;
    }
}
