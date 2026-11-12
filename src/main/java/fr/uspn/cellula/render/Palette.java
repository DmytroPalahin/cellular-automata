package fr.uspn.cellula.render;

import java.awt.Color;
import java.util.Map;

/**
 * Maps cell states to display colors.
 *
 * <p>
 * The default palette follows the figures of the subject: white for a cell that
 * is off, blue
 * for a cell that is on. Further states get distinct colors, so multi-state
 * automata are legible
 * without any configuration, and a definition file may override any of them.
 *
 * <p>
 * Palettes are immutable.
 */
public final class Palette {

    private static final int[] DEFAULTS = {
            0xFFFFFF, // 0: off
            0x1D4ED8, // 1: on
            0xF59E0B,
            0x10B981,
            0xEF4444,
            0x8B5CF6,
            0x06B6D4,
            0xEC4899,
    };

    private static final Palette STANDARD = new Palette(Map.of());

    private final Map<Integer, Integer> overrides;

    private Palette(Map<Integer, Integer> overrides) {
        this.overrides = Map.copyOf(overrides);
    }

    /** Returns the default palette. */
    public static Palette standard() {
        return STANDARD;
    }

    /**
     * Returns the default palette with some colors replaced.
     *
     * @param overrides colors as {@code 0xRRGGBB}, by state
     */
    public static Palette of(Map<Integer, Integer> overrides) {
        return overrides.isEmpty() ? STANDARD : new Palette(overrides);
    }

    /**
     * Returns the color of a state as {@code 0xRRGGBB}.
     *
     * @param state any state, including negative or large values
     */
    public int rgb(int state) {
        Integer override = overrides.get(state);
        if (override != null) {
            return override & 0xFFFFFF;
        }
        if (state >= 0 && state < DEFAULTS.length) {
            return DEFAULTS[state];
        }
        // Unplanned states: spread hues with the golden ratio so neighbors in value
        // differ in color.
        float hue = (float) ((state * 0.6180339887498949) % 1.0);
        return Color.HSBtoRGB(hue < 0 ? hue + 1 : hue, 0.65f, 0.85f) & 0xFFFFFF;
    }

    /** Returns the color of a state. */
    public Color color(int state) {
        return new Color(rgb(state));
    }
}
