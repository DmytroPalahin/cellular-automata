package fr.uspn.cellula.render;

import fr.uspn.cellula.grid.Frame;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/**
 * Turns frames into raster images.
 *
 * <p>The same code paints the simulation window and produces exported PNG files, so what is saved
 * is what was on screen. It only relies on the part of AWT that works without a display.
 */
public final class FrameImages {

    /** Color of the lines separating cells when a grid is drawn. */
    private static final Color GRID_LINE = new Color(0xE2E8F0);

    /** Cells smaller than this many pixels are drawn without separating lines. */
    private static final int MIN_CELL_SIZE_FOR_GRID = 4;

    private FrameImages() {}

    /**
     * Renders a frame with one pixel per cell.
     *
     * @param frame the states to draw
     * @param palette the colors of the states
     * @return an image of {@code frame.columns()} by {@code frame.rows()} pixels
     */
    public static BufferedImage render(Frame frame, Palette palette) {
        int[] states = frame.states();
        int[] pixels = new int[states.length];
        // Most automata use a handful of small states: resolve those colors once.
        int[] cache = new int[16];
        for (int state = 0; state < cache.length; state++) {
            cache[state] = palette.rgb(state);
        }
        for (int index = 0; index < states.length; index++) {
            int state = states[index];
            pixels[index] = state >= 0 && state < cache.length ? cache[state] : palette.rgb(state);
        }
        BufferedImage image = new BufferedImage(
                Math.max(1, frame.columns()), Math.max(1, frame.rows()), BufferedImage.TYPE_INT_RGB);
        if (pixels.length > 0) {
            image.setRGB(0, 0, frame.columns(), frame.rows(), pixels, 0, frame.columns());
        }
        return image;
    }

    /**
     * Renders a frame with square cells of a given size.
     *
     * @param frame the states to draw
     * @param palette the colors of the states
     * @param cellSize side of a cell, in pixels; at least one
     * @param gridLines whether to separate cells with thin lines (ignored for very small cells)
     * @return an image of {@code columns * cellSize} by {@code rows * cellSize} pixels
     */
    public static BufferedImage render(Frame frame, Palette palette, int cellSize, boolean gridLines) {
        if (cellSize < 1) {
            throw new IllegalArgumentException("cell size must be positive, got " + cellSize);
        }
        BufferedImage cells = render(frame, palette);
        int width = cells.getWidth() * cellSize;
        int height = cells.getHeight() * cellSize;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            paint(graphics, cells, 0, 0, cellSize, gridLines);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    /**
     * Paints a one-pixel-per-cell image, magnified, onto a graphics context.
     *
     * @param graphics where to paint
     * @param cells an image produced by {@link #render(Frame, Palette)}
     * @param x left edge of the painted area
     * @param y top edge of the painted area
     * @param cellSize side of a cell, in pixels
     * @param gridLines whether to separate cells with thin lines (ignored for very small cells)
     */
    public static void paint(
            Graphics2D graphics, BufferedImage cells, int x, int y, int cellSize, boolean gridLines) {
        int width = cells.getWidth() * cellSize;
        int height = cells.getHeight() * cellSize;
        graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.drawImage(cells, x, y, width, height, null);
        if (gridLines && cellSize >= MIN_CELL_SIZE_FOR_GRID) {
            graphics.setColor(GRID_LINE);
            for (int column = 0; column <= cells.getWidth(); column++) {
                int lineX = Math.min(x + column * cellSize, x + width - 1);
                graphics.drawLine(lineX, y, lineX, y + height - 1);
            }
            for (int row = 0; row <= cells.getHeight(); row++) {
                int lineY = Math.min(y + row * cellSize, y + height - 1);
                graphics.drawLine(x, lineY, x + width - 1, lineY);
            }
        }
    }

    /**
     * Saves an image as a PNG file, creating the parent directories if needed.
     *
     * @param image the image to save
     * @param file the destination
     * @throws IOException if the file cannot be written
     */
    public static void writePng(BufferedImage image, Path file) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (!ImageIO.write(image, "png", file.toFile())) {
            throw new IOException("no PNG encoder available to write " + file);
        }
    }
}
