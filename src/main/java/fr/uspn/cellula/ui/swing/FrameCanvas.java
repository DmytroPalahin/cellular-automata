package fr.uspn.cellula.ui.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import fr.uspn.cellula.grid.Frame;
import fr.uspn.cellula.render.FrameImages;
import fr.uspn.cellula.render.Palette;

/**
 * The component that draws a {@link Frame} as a grid of colored squares.
 *
 * <p>
 * It supports the two displays of the application:
 *
 * <ul>
 * <li><em>fitted</em>: the whole frame is scaled to the component and centered
 * (planes);
 * <li><em>scrolling</em>: the frame is fitted in width and anchored to the top,
 * and when it
 * grows taller than the component only its latest rows are shown, like a
 * terminal
 * (space-time diagrams).
 * </ul>
 *
 * <p>
 * The canvas also translates mouse gestures into "the user pointed at this
 * cell" events.
 */
final class FrameCanvas extends JComponent {

    /** Receives the cells the user clicks or drags over. */
    interface CellListener {

        /**
         * Called when the pointer is pressed on, or dragged onto, a cell.
         *
         * @param row          row of the cell in the displayed frame
         * @param column       column of the cell in the displayed frame
         * @param gestureStart {@code true} for the press that starts a gesture,
         *                     {@code false}
         *                     for the cells reached while dragging
         */
        void cellPointed(int row, int column, boolean gestureStart);
    }

    private static final int PADDING = 12;
    private static final int MAX_CELL_SIZE = 48;
    private static final int MAX_SCROLLING_CELL_SIZE = 12;
    private static final int MIN_CELL_SIZE_FOR_GRID = 6;
    private static final Color BACKGROUND = new Color(0xF1F5F9);
    private static final Color BORDER = new Color(0x94A3B8);

    private final Palette palette;
    private final boolean scrolling;
    private Frame frame;
    private BufferedImage cells;
    private boolean gridLines = true;
    private CellListener cellListener;

    /**
     * Creates an empty canvas.
     *
     * @param palette   the colors of the states
     * @param scrolling {@code true} for the scrolling display, {@code false} for
     *                  the fitted one
     */
    FrameCanvas(Palette palette, boolean scrolling) {
        this.palette = palette;
        this.scrolling = scrolling;
        setOpaque(true);
        setBackground(BACKGROUND);
        Pointer pointer = new Pointer();
        addMouseListener(pointer);
        addMouseMotionListener(pointer);
    }

    /** Displays a new frame. Must be called on the event dispatch thread. */
    void showFrame(Frame newFrame) {
        frame = newFrame;
        cells = FrameImages.render(newFrame, palette);
        repaint();
    }

    /**
     * Returns the frame being displayed, or {@code null} before the first one
     * arrived.
     */
    Frame frame() {
        return frame;
    }

    /**
     * Returns whether cells are separated by thin lines when they are large enough.
     */
    boolean gridLines() {
        return gridLines;
    }

    void setGridLines(boolean gridLines) {
        this.gridLines = gridLines;
        repaint();
    }

    void setCellListener(CellListener listener) {
        this.cellListener = listener;
    }

    /**
     * Returns a sensible initial size for a frame of the given dimensions.
     *
     * @param rows    number of rows the canvas should comfortably show
     * @param columns number of columns of the frames
     */
    static Dimension comfortableSize(int rows, int columns) {
        int cell = Math.max(2, Math.min(16, 760 / Math.max(1, Math.max(rows, columns))));
        int width = Math.max(720, Math.min(1100, columns * cell + 2 * PADDING));
        int height = Math.max(360, Math.min(780, rows * cell + 2 * PADDING));
        return new Dimension(width, height);
    }

    /** Where the frame goes inside the component. */
    private record Layout(int x, int y, double cellSize, int firstRow, int rows, int columns) {

        int width() {
            return (int) Math.round(columns * cellSize);
        }

        int height() {
            return (int) Math.round(rows * cellSize);
        }
    }

    private Layout computeLayout() {
        int availableWidth = Math.max(1, getWidth() - 2 * PADDING);
        int availableHeight = Math.max(1, getHeight() - 2 * PADDING);
        int columns = frame.columns();
        int rows = frame.rows();

        double cellSize;
        int firstRow = 0;
        int shownRows = rows;
        if (scrolling) {
            cellSize = snap(Math.min(MAX_SCROLLING_CELL_SIZE, (double) availableWidth / columns));
            int visibleRows = Math.max(1, (int) (availableHeight / cellSize));
            shownRows = Math.min(rows, visibleRows);
            firstRow = rows - shownRows;
        } else {
            cellSize = snap(Math.min(
                    MAX_CELL_SIZE,
                    Math.min((double) availableWidth / columns, (double) availableHeight / rows)));
        }
        int width = (int) Math.round(columns * cellSize);
        int height = (int) Math.round(shownRows * cellSize);
        int x = PADDING + (availableWidth - width) / 2;
        int y = scrolling ? PADDING : PADDING + (availableHeight - height) / 2;
        return new Layout(x, y, cellSize, firstRow, shownRows, columns);
    }

    /**
     * Whole pixels per cell whenever a cell is at least one pixel, to keep squares
     * crisp.
     */
    private static double snap(double cellSize) {
        return cellSize >= 1 ? Math.floor(cellSize) : cellSize;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            if (frame == null || frame.rows() == 0 || frame.columns() == 0) {
                return;
            }
            Layout layout = computeLayout();
            int width = layout.width();
            int height = layout.height();

            g.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    layout.cellSize() >= 1
                            ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
                            : RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(
                    cells,
                    layout.x(), layout.y(), layout.x() + width, layout.y() + height,
                    0, layout.firstRow(), layout.columns(), layout.firstRow() + layout.rows(),
                    null);

            if (gridLines && layout.cellSize() >= MIN_CELL_SIZE_FOR_GRID) {
                int cell = (int) layout.cellSize();
                g.setColor(new Color(0xE2E8F0));
                for (int column = 1; column < layout.columns(); column++) {
                    int x = layout.x() + column * cell;
                    g.drawLine(x, layout.y(), x, layout.y() + height - 1);
                }
                for (int row = 1; row < layout.rows(); row++) {
                    int y = layout.y() + row * cell;
                    g.drawLine(layout.x(), y, layout.x() + width - 1, y);
                }
            }
            g.setColor(BORDER);
            g.drawRect(layout.x() - 1, layout.y() - 1, width + 1, height + 1);
        } finally {
            g.dispose();
        }
    }

    /** Turns presses and drags into {@link CellListener} notifications. */
    private final class Pointer extends MouseAdapter {

        private int lastRow = -1;
        private int lastColumn = -1;

        @Override
        public void mousePressed(MouseEvent event) {
            if (SwingUtilities.isLeftMouseButton(event)) {
                lastRow = -1;
                lastColumn = -1;
                point(event, true);
            }
        }

        @Override
        public void mouseDragged(MouseEvent event) {
            if (SwingUtilities.isLeftMouseButton(event)) {
                point(event, false);
            }
        }

        private void point(MouseEvent event, boolean gestureStart) {
            if (cellListener == null || frame == null || frame.rows() == 0) {
                return;
            }
            Layout layout = computeLayout();
            int column = (int) Math.floor((event.getX() - layout.x()) / layout.cellSize());
            int row = (int) Math.floor((event.getY() - layout.y()) / layout.cellSize());
            if (column < 0 || column >= layout.columns() || row < 0 || row >= layout.rows()) {
                return;
            }
            row += layout.firstRow();
            if (!gestureStart && row == lastRow && column == lastColumn) {
                return; // still on the same cell
            }
            lastRow = row;
            lastColumn = column;
            cellListener.cellPointed(row, column, gestureStart);
        }
    }
}
