package fr.uspn.cellula.ui.swing;

import fr.uspn.cellula.config.AutomatonDefinition;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * Entry points of the graphical user interface.
 *
 * <p>These methods may be called from any thread: they take care of creating the windows on the
 * event dispatch thread.
 */
public final class SwingApp {

    private static boolean prepared;

    private SwingApp() {}

    /** Tells whether windows can be opened, i.e. whether the JVM has access to a display. */
    public static boolean isAvailable() {
        return !GraphicsEnvironment.isHeadless();
    }

    /**
     * Opens the start-up menu.
     *
     * @param directory the directory whose definition files are listed
     */
    public static void showLauncher(Path directory) {
        prepare();
        SwingUtilities.invokeLater(() -> new LauncherFrame(directory).setVisible(true));
    }

    /**
     * Opens the window of one automaton.
     *
     * @param definition the automaton to show
     * @return a future completed once the window has been closed; it fails if the window could
     *     not be created
     */
    public static CompletableFuture<Void> showSimulation(AutomatonDefinition definition) {
        prepare();
        CompletableFuture<Void> closed = new CompletableFuture<>();
        SwingUtilities.invokeLater(() -> {
            try {
                SimulationFrame frame = new SimulationFrame(definition);
                frame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent event) {
                        closed.complete(null);
                    }
                });
                frame.setVisible(true);
            } catch (RuntimeException e) {
                closed.completeExceptionally(e);
            }
        });
        return closed;
    }

    /** Applies the platform look and feel, once. */
    private static synchronized void prepare() {
        if (prepared) {
            return;
        }
        prepared = true;
        System.setProperty("apple.awt.application.name", "Cellula");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | UnsupportedLookAndFeelException e) {
            // The cross-platform look and feel is a perfectly good fallback.
        }
    }
}
