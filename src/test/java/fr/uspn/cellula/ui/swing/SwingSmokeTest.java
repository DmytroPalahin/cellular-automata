package fr.uspn.cellula.ui.swing;

import static fr.uspn.cellula.ui.swing.SwingProbe.await;
import static fr.uspn.cellula.ui.swing.SwingProbe.find;
import static fr.uspn.cellula.ui.swing.SwingProbe.onEdt;
import static fr.uspn.cellula.ui.swing.SwingProbe.runOnEdt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.uspn.cellula.config.AutomatonDefinition;
import fr.uspn.cellula.config.DefinitionLibrary.Entry;
import fr.uspn.cellula.config.InitialState;
import fr.uspn.cellula.grid.Coordinates;
import fr.uspn.cellula.grid.Slice;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.DisabledIf;
import org.junit.jupiter.api.io.TempDir;

/**
 * Exercises the real windows, without showing them. These tests need a display, which a build
 * server does not have: they are skipped when the JVM is headless, which is how the Maven build
 * runs the suite by default. Run them with {@code ./mvnw verify -Dtest.headless=false}.
 */
@DisabledIf(value = "java.awt.GraphicsEnvironment#isHeadless", disabledReason = "needs a display")
@Timeout(30)
class SwingSmokeTest {

    private JFrame frame;

    @AfterEach
    void disposeWindow() {
        if (frame != null) {
            runOnEdt(frame::dispose);
        }
    }

    private SimulationFrame open(AutomatonDefinition definition) {
        SimulationFrame window = onEdt(() -> new SimulationFrame(definition));
        frame = window;
        await("the first snapshot", () -> status().startsWith("Generation 0"));
        return window;
    }

    private String status() {
        return find(frame, JLabel.class, label -> label.getText().startsWith("Generation")
                        || label.getText().isBlank())
                .getText();
    }

    private void click(String button) {
        runOnEdt(() -> find(frame, JButton.class, candidate -> candidate.getText().equals(button)).doClick());
    }

    private static AutomatonDefinition.Builder blinker() {
        return AutomatonDefinition.builder()
                .name("Blinker")
                .size(5, 5)
                .rule("SI(COMPTER(G0), OU(EQ(COMPTER(G8*), 2), EQ(COMPTER(G8*), 3)), EQ(COMPTER(G8*), 3))")
                .initialState(new InitialState.ExplicitCells(
                        List.of(Coordinates.of(2, 1), Coordinates.of(2, 2), Coordinates.of(2, 3))));
    }

    @Test
    void theButtonsDriveTheSimulation() {
        open(blinker().build());
        assertTrue(status().contains("Population 3 (12.0 %)"), status());
        assertTrue(status().endsWith("evolving, paused"), status());

        click("Step");
        await("generation 1", () -> status().startsWith("Generation 1 "));
        click("Step");
        await("the cycle to be noticed", () -> status().contains("cycle of period 2"));

        click("Reset");
        await("generation 0", () -> status().startsWith("Generation 0 "));

        click("Play");
        await("the simulation to run", () -> status().endsWith("running"));
        click("Pause");
        await("the simulation to pause", () -> status().endsWith("paused"));
        assertFalse(status().startsWith("Generation 0 "), status());
    }

    @Test
    void cellsCanBeDrawnWithTheMouse() {
        open(blinker().build());
        click("Clear");
        await("an empty grid", () -> status().contains("Population 0 "));

        FrameCanvas canvas = find(frame, FrameCanvas.class, any -> true);
        runOnEdt(() -> {
            int x = canvas.getWidth() / 2;
            int y = canvas.getHeight() / 2;
            canvas.dispatchEvent(new MouseEvent(
                    canvas, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
                    MouseEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1));
        });

        await("the drawn cell", () -> status().contains("Population 1 "));
        assertEquals(1, onEdt(() -> canvas.frame().stateAt(2, 2)));
    }

    @Test
    void theSliceOfAVolumeCanBeMoved() {
        open(AutomatonDefinition.builder()
                .name("Volume")
                .size(6, 6, 6)
                .slice(Slice.parse("(0, :, :)"))
                .rule("ETAT()")
                .initialState(new InitialState.ExplicitCells(List.of(Coordinates.of(4, 1, 1))))
                .build());
        FrameCanvas canvas = find(frame, FrameCanvas.class, any -> true);
        assertEquals(0, onEdt(() -> canvas.frame().stateAt(1, 1)));

        runOnEdt(() -> find(frame, JSpinner.class, any -> true).setValue(4));

        await("the new slice", () -> find(frame, JLabel.class, label -> label.getText().startsWith("("))
                .getText()
                .equals("(4, :, :)"));
        await("the cell of the new slice", () -> canvas.frame().stateAt(1, 1) == 1);
    }

    @Test
    void oneDimensionalAutomataScrollDownTheWindow() {
        open(AutomatonDefinition.builder()
                .name("Line")
                .size(21)
                .rule("SUPEQ(COMPTER(G2), 1)")
                .initialState(new InitialState.ExplicitCells(List.of(Coordinates.of(10))))
                .build());
        FrameCanvas canvas = find(frame, FrameCanvas.class, any -> true);
        assertEquals(1, onEdt(() -> canvas.frame().rows()));

        click("Step");
        click("Step");

        await("three generations on screen", () -> canvas.frame().rows() == 3);
        assertEquals(1, onEdt(() -> canvas.frame().stateAt(2, 8)));
    }

    @Test
    void theLauncherListsTheDefinitionsOfAFolder(@TempDir Path directory) throws IOException {
        Files.writeString(
                directory.resolve("good.xml"),
                "<automaton name=\"Good one\"><dimension>1</dimension><size>9</size>"
                        + "<rule>COMPTER(G2)</rule><initial>(4)</initial></automaton>");
        Files.writeString(
                directory.resolve("bad.xml"),
                "<automaton name=\"Bad one\"><dimension>1</dimension><size>9</size>"
                        + "<rule>COMPTER(G8)</rule><initial>(4)</initial></automaton>");

        frame = onEdt(() -> new LauncherFrame(directory));

        @SuppressWarnings("unchecked")
        JList<Entry> list = find(frame, JList.class, any -> true);
        JTextArea details = find(frame, JTextArea.class, any -> true);
        JButton run = find(frame, JButton.class, button -> button.getText().equals("Run simulation"));

        assertEquals(2, onEdt(() -> list.getModel().getSize()));
        // The first entry, in file name order, is the broken one.
        assertTrue(onEdt(details::getText).contains("unknown neighborhood 'G8'"));
        assertFalse(onEdt(run::isEnabled));

        runOnEdt(() -> list.setSelectedIndex(1));
        assertTrue(onEdt(details::getText).startsWith("Good one\n1D · 9 · fixed boundary"));
        assertTrue(onEdt(run::isEnabled));
    }
}
