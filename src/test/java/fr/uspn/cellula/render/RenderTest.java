package fr.uspn.cellula.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.uspn.cellula.grid.Frame;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RenderTest {

    private static final Frame FRAME = new Frame(2, 3, new int[] {0, 1, 2, 1, 0, 7});

    private static int rgb(BufferedImage image, int x, int y) {
        return image.getRGB(x, y) & 0xFFFFFF;
    }

    @Test
    void theDefaultPaletteIsBlueOnWhiteLikeTheFiguresOfTheSubject() {
        Palette palette = Palette.standard();

        assertEquals(0xFFFFFF, palette.rgb(0));
        assertEquals(0x1D4ED8, palette.rgb(1));
        assertEquals(new Color(0x1D4ED8), palette.color(1));
    }

    @Test
    void everyStateHasItsOwnColor() {
        Palette palette = Palette.standard();
        Set<Integer> colors = new HashSet<>();
        for (int state = -5; state < 40; state++) {
            colors.add(palette.rgb(state));
        }

        assertEquals(45, colors.size());
    }

    @Test
    void colorsCanBeOverridden() {
        Palette palette = Palette.of(Map.of(0, 0x000000, 5, 0x123456));

        assertEquals(0x000000, palette.rgb(0));
        assertEquals(0x123456, palette.rgb(5));
        assertEquals(Palette.standard().rgb(1), palette.rgb(1));
        assertSame(Palette.standard(), Palette.of(Map.of()));
    }

    @Test
    void rendersOnePixelPerCell() {
        Palette palette = Palette.standard();

        BufferedImage image = FrameImages.render(FRAME, palette);

        assertEquals(3, image.getWidth());
        assertEquals(2, image.getHeight());
        assertEquals(palette.rgb(0), rgb(image, 0, 0));
        assertEquals(palette.rgb(1), rgb(image, 1, 0));
        assertEquals(palette.rgb(2), rgb(image, 2, 0));
        assertEquals(palette.rgb(1), rgb(image, 0, 1));
        assertEquals(palette.rgb(7), rgb(image, 2, 1));
    }

    @Test
    void magnifiesCellsIntoCrispSquares() {
        Palette palette = Palette.standard();

        BufferedImage image = FrameImages.render(FRAME, palette, 10, false);

        assertEquals(30, image.getWidth());
        assertEquals(20, image.getHeight());
        for (int offset : new int[] {0, 4, 9}) {
            assertEquals(palette.rgb(1), rgb(image, 10 + offset, offset));
            assertEquals(palette.rgb(7), rgb(image, 20 + offset, 10 + offset));
        }
    }

    @Test
    void gridLinesSeparateLargeCellsOnly() {
        Palette palette = Palette.standard();

        BufferedImage large = FrameImages.render(FRAME, palette, 10, true);
        BufferedImage small = FrameImages.render(FRAME, palette, 2, true);

        assertNotEquals(palette.rgb(1), rgb(large, 10, 5), "a line runs along the left edge of the cell");
        assertEquals(palette.rgb(1), rgb(large, 15, 5), "the inside of the cell is untouched");
        assertEquals(palette.rgb(1), rgb(small, 2, 0), "cells this small are not outlined");
    }

    @Test
    void rejectsANonPositiveCellSize() {
        assertThrows(IllegalArgumentException.class, () -> FrameImages.render(FRAME, Palette.standard(), 0, false));
    }

    @Test
    void savesPngFiles(@TempDir Path directory) throws IOException {
        Path file = directory.resolve("out/picture.png");

        FrameImages.writePng(FrameImages.render(FRAME, Palette.standard(), 4, false), file);

        BufferedImage reloaded = ImageIO.read(file.toFile());
        assertTrue(Files.size(file) > 0);
        assertEquals(12, reloaded.getWidth());
        assertEquals(Palette.standard().rgb(1), rgb(reloaded, 5, 1));
    }

    @Test
    void drawsFramesAsText() {
        assertEquals("·█2\n█·7\n", TextRenderer.unicode().render(FRAME));
        assertEquals(".#2\n#.7\n", TextRenderer.ascii().render(FRAME));
        assertEquals("", TextRenderer.ascii().render(new Frame(0, 0, new int[0])));
    }
}
