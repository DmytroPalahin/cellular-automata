package fr.uspn.cellula.ui.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Helpers to drive Swing windows from the outside without ever showing them: run code on the
 * event dispatch thread, find components, wait for a condition, take a picture.
 */
final class SwingProbe {

    private SwingProbe() {}

    /** Runs a computation on the event dispatch thread and returns its result. */
    static <T> T onEdt(Callable<T> computation) {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Exception> failure = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    result.set(computation.call());
                } catch (Exception e) {
                    failure.set(e);
                }
            });
        } catch (InterruptedException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
        if (failure.get() != null) {
            throw new IllegalStateException(failure.get());
        }
        return result.get();
    }

    /** Runs an action on the event dispatch thread. */
    static void runOnEdt(Runnable action) {
        onEdt(() -> {
            action.run();
            return null;
        });
    }

    /** Returns every component of the given type below a container, depth first. */
    static <T extends Component> List<T> all(Container root, Class<T> type) {
        List<T> found = new ArrayList<>();
        collect(root, type, found);
        return found;
    }

    private static <T extends Component> void collect(Component component, Class<T> type, List<T> found) {
        if (type.isInstance(component)) {
            found.add(type.cast(component));
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                collect(child, type, found);
            }
        }
    }

    /** Returns the only component of the given type that satisfies a condition. */
    static <T extends Component> T find(Container root, Class<T> type, Predicate<T> condition) {
        List<T> matches = all(root, type).stream().filter(condition).toList();
        if (matches.size() != 1) {
            throw new IllegalStateException(
                    "expected exactly one matching " + type.getSimpleName() + " but found " + matches.size());
        }
        return matches.get(0);
    }

    /** Polls a condition on the event dispatch thread until it holds or five seconds have passed. */
    static void await(String what, BooleanSupplier condition) {
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (!onEdt(condition::getAsBoolean)) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("timed out waiting for " + what);
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
    }

    /** Paints the content of a window into an image, at twice the resolution of the layout. */
    static BufferedImage picture(JFrame frame) {
        return onEdt(() -> {
            Container content = frame.getContentPane();
            content.validate();
            BufferedImage image = new BufferedImage(
                    content.getWidth() * 2, content.getHeight() * 2, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            graphics.scale(2, 2);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            content.printAll(graphics);
            graphics.dispose();
            return image;
        });
    }
}
