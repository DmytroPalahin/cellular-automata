package fr.uspn.cellula.ui.swing;

import static fr.uspn.cellula.ui.swing.SwingProbe.await;
import static fr.uspn.cellula.ui.swing.SwingProbe.find;
import static fr.uspn.cellula.ui.swing.SwingProbe.onEdt;
import static fr.uspn.cellula.ui.swing.SwingProbe.picture;
import static fr.uspn.cellula.ui.swing.SwingProbe.runOnEdt;

import fr.uspn.cellula.config.XmlDefinitionReader;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.UIManager;

/**
 * Regenerates the pictures of the user interface used by the documentation.
 *
 * <p>This is a developer tool, not a test: it builds the real windows, drives them to an
 * interesting state and saves what they would show, without ever putting them on screen.
 *
 * <p>Run it with {@code scripts/screenshots.sh}. Arguments: the directory of the example
 * definitions, then the output directory.
 */
public final class Screenshots {

    private Screenshots() {}

    /**
     * Runs the tool.
     *
     * @param args the examples directory and the output directory
     * @throws Exception if a window cannot be built or a picture cannot be saved
     */
    public static void main(String[] args) throws Exception {
        Path examples = Path.of(args.length > 0 ? args[0] : "examples");
        Path output = Path.of(args.length > 1 ? args[1] : "docs/images");
        Files.createDirectories(output);
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        JFrame launcher = onEdt(() -> new LauncherFrame(examples));
        runOnEdt(() -> find(launcher, JList.class, any -> true).setSelectedIndex(3));
        save(launcher, output.resolve("launcher.png"));

        simulation(examples.resolve("game-of-life.xml"), 60, output.resolve("window-game-of-life.png"));
        simulation(examples.resolve("sierpinski.xml"), 127, output.resolve("window-sierpinski.png"));
        simulation(examples.resolve("crystal-3d.xml"), 30, output.resolve("window-crystal-3d.png"));
        simulation(examples.resolve("brians-brain.xml"), 40, output.resolve("window-brians-brain.png"));
        System.exit(0);
    }

    private static void simulation(Path definition, int generations, Path file) throws Exception {
        JFrame frame = onEdt(() -> new SimulationFrame(XmlDefinitionReader.read(definition)));
        JButton step = find(frame, JButton.class, button -> button.getText().equals("Step"));
        for (int generation = 0; generation < generations; generation++) {
            runOnEdt(step::doClick);
        }
        await("generation " + generations, () -> find(frame, JLabel.class,
                        label -> label.getText().startsWith("Generation"))
                .getText()
                .startsWith("Generation " + generations + " "));
        save(frame, file);
    }

    private static void save(JFrame frame, Path file) throws Exception {
        ImageIO.write(picture(frame), "png", file.toFile());
        runOnEdt(frame::dispose);
        System.out.println("Wrote " + file);
    }
}
