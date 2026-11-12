package fr.uspn.cellula.render;

import fr.uspn.cellula.grid.Frame;

/**
 * Draws frames with characters, for terminals and for tests.
 *
 * <p>A cell that is off is shown as a dot, a cell that is on as a block, and any other state as
 * its last decimal digit.
 */
public final class TextRenderer {

    private final char off;
    private final char on;

    private TextRenderer(char off, char on) {
        this.off = off;
        this.on = on;
    }

    /** Returns a renderer using Unicode block characters: {@code ·} and {@code █}. */
    public static TextRenderer unicode() {
        return new TextRenderer('·', '█');
    }

    /** Returns a renderer restricted to ASCII: {@code .} and {@code #}. */
    public static TextRenderer ascii() {
        return new TextRenderer('.', '#');
    }

    /**
     * Renders a frame.
     *
     * @param frame the states to draw
     * @return one line of text per row, each terminated by a line feed
     */
    public String render(Frame frame) {
        StringBuilder text = new StringBuilder(frame.rows() * (frame.columns() + 1));
        for (int row = 0; row < frame.rows(); row++) {
            for (int column = 0; column < frame.columns(); column++) {
                text.append(symbol(frame.stateAt(row, column)));
            }
            text.append('\n');
        }
        return text.toString();
    }

    private char symbol(int state) {
        return switch (state) {
            case 0 -> off;
            case 1 -> on;
            default -> (char) ('0' + Math.floorMod(state, 10));
        };
    }
}
