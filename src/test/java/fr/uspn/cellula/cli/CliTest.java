package fr.uspn.cellula.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.uspn.cellula.config.AutomatonDefinition;
import fr.uspn.cellula.config.XmlDefinitionReader;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;

/**
 * Drives the application through its command line, end to end, in a temporary directory.
 *
 * <p>The tests of the menu and of {@code run} rely on the JVM being headless, which makes the
 * application fall back to its text form. They are skipped when a display is available, where
 * they would open real windows.
 */
class CliTest {

    private static final String GROWTH = """
            <automaton name="Growth">
              <dimension>1</dimension>
              <size>9</size>
              <rule>SUPEQ(COMPTER(G2), 1)</rule>
              <initial>(4)</initial>
            </automaton>""";

    private static final String VOLUME = """
            <automaton name="Volume">
              <dimension>3</dimension>
              <size>3, 4, 5</size>
              <slice>(1, :, :)</slice>
              <rule>ETAT()</rule>
              <initial>(1, 0, 0), (1, 3, 4), (0, 2, 2)</initial>
            </automaton>""";

    private static final String BROKEN = """
            <automaton name="Broken">
              <dimension>1</dimension>
              <size>9</size>
              <rule>SI(COMPTER(G2), 1)</rule>
              <initial>(4)</initial>
            </automaton>""";

    @TempDir
    Path directory;

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    @BeforeEach
    void writeDefinitions() throws IOException {
        Files.writeString(directory.resolve("growth.xml"), GROWTH);
        Files.writeString(directory.resolve("volume.xml"), VOLUME);
    }

    private int run(String... args) {
        return runWithInput("", args);
    }

    private int runWithInput(String input, String... args) {
        Console console = new Console(
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8),
                new BufferedReader(new StringReader(input)),
                directory);
        return new Cli(console).run(args);
    }

    private String out() {
        return out.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private String err() {
        return err.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    @Test
    void helpListsTheCommands() {
        assertEquals(Cli.SUCCESS, run("help"));

        for (String command : new String[] {"menu", "run", "list", "validate", "print", "render", "explore"}) {
            assertTrue(out().contains("\n  " + command + " "), command);
        }
    }

    @Test
    void helpIsAvailableInSeveralForms() {
        assertEquals(Cli.SUCCESS, run("--help"));
        assertEquals(Cli.SUCCESS, run("help", "render"));
        assertEquals(Cli.SUCCESS, run("print", "-h"));

        assertTrue(out().contains("Usage: cellula [command] [options]"));
        assertTrue(out().contains("Usage: cellula render FILE --output IMAGE.png"));
        assertTrue(out().contains("Usage: cellula print FILE"));
    }

    @Test
    void printsItsVersion() {
        assertEquals(Cli.SUCCESS, run("version"));
        assertEquals(Cli.SUCCESS, run("--version"));

        assertTrue(out().startsWith("Cellula "), out());
    }

    @Test
    void misuseIsReportedWithStatusTwo() {
        assertEquals(Cli.USAGE, run("frobnicate"));
        assertEquals(Cli.USAGE, run("help", "frobnicate"));
        assertEquals(Cli.USAGE, run("print"));
        assertEquals(Cli.USAGE, run("print", "growth.xml", "--steps", "lots"));
        assertEquals(Cli.USAGE, run("render", "growth.xml"));
        assertEquals(Cli.USAGE, run("list", "--dir", "no-such-directory"));
        assertEquals(Cli.USAGE, run("explore", "--family", "odd"));

        assertTrue(err().contains("unknown command 'frobnicate'"), err());
        assertTrue(err().contains("cellula print: expected exactly one definition file"), err());
        assertTrue(err().contains("option --steps expects an integer"), err());
        assertTrue(err().contains("option --output is required"), err());
        assertTrue(err().contains("is not a directory"), err());
        assertTrue(err().contains("unknown family 'odd'"), err());
        assertEquals("", out());
    }

    @Test
    void printShowsAOneDimensionalAutomatonAsASpaceTimeDiagram() {
        assertEquals(Cli.SUCCESS, run("print", "growth.xml", "--steps", "3", "--ascii"));

        assertEquals(
                """
                Growth (1D · 9 · fixed boundary)
                Generations 0 to 3, one per line:
                ....#....
                ...###...
                ..#####..
                .#######.
                """,
                out());
    }

    @Test
    void printShowsTheSelectedSliceOfAVolume() {
        assertEquals(Cli.SUCCESS, run("print", "volume.xml", "-n", "2"));

        assertEquals(
                """
                Volume (3D · 3 × 4 × 5 · fixed boundary)
                Generation 2, slice (1, :, :):
                █····
                ·····
                ·····
                ····█
                """,
                out());
    }

    @Test
    void renderSavesAPicture() throws IOException {
        assertEquals(Cli.SUCCESS, run("render", "growth.xml", "-o", "pictures/growth.png", "--steps", "4", "--cell-size", "8"));

        BufferedImage image = ImageIO.read(directory.resolve("pictures/growth.png").toFile());
        assertEquals(9 * 8, image.getWidth());
        assertEquals(5 * 8, image.getHeight());
        assertEquals(0x1D4ED8, image.getRGB(4 * 8 + 4, 4) & 0xFFFFFF, "the seed is blue");
        assertEquals(0xFFFFFF, image.getRGB(4, 4) & 0xFFFFFF, "the corner is white");
        assertTrue(out().contains("72 x 40 pixels"), out());
    }

    @Test
    void renderPicksAReasonableCellSizeByDefault() throws IOException {
        assertEquals(Cli.SUCCESS, run("render", "volume.xml", "--output", "volume.png", "--no-grid"));

        BufferedImage image = ImageIO.read(directory.resolve("volume.png").toFile());
        assertEquals(5 * 32, image.getWidth());
        assertEquals(4 * 32, image.getHeight());
    }

    @Test
    void listPrintsTheDefinitionsOfADirectory() throws IOException {
        Files.writeString(directory.resolve("broken.xml"), BROKEN);
        Files.writeString(directory.resolve("pom.xml"), "<project/>");

        assertEquals(Cli.SUCCESS, run("list"));

        String[] lines = out().split("\n");
        assertEquals(3, lines.length);
        assertTrue(lines[0].startsWith("broken.xml") && lines[0].endsWith("INVALID"), lines[0]);
        assertTrue(lines[1].startsWith("growth.xml") && lines[1].contains("Growth") && lines[1].endsWith("1D  9"), lines[1]);
        assertTrue(lines[2].startsWith("volume.xml") && lines[2].endsWith("3D  3 × 4 × 5"), lines[2]);
    }

    @Test
    void validateAcceptsValidFiles() {
        assertEquals(Cli.SUCCESS, run("validate", "growth.xml", "volume.xml"));

        assertTrue(out().contains("ok    growth.xml - Growth (1D · 9 · fixed boundary)"), out());
        assertTrue(out().contains("ok    volume.xml"), out());
        assertEquals("", err());
    }

    @Test
    void validateExplainsWhatIsWrong() throws IOException {
        Files.writeString(directory.resolve("broken.xml"), BROKEN);

        assertEquals(Cli.FAILURE, run("validate", "growth.xml", "broken.xml", "missing.xml"));

        assertTrue(out().contains("ok    growth.xml"), out());
        assertTrue(out().contains("FAIL  broken.xml"), out());
        assertTrue(out().contains("FAIL  missing.xml"), out());
        assertTrue(err().contains("broken.xml: SI expects 3 argument(s) but got 2 (line 1, column 1)"), err());
        assertTrue(err().contains("    SI(COMPTER(G2), 1)\n    ^"), err());
        assertTrue(err().contains("missing.xml: cannot read the file"), err());
    }

    @Test
    void anInvalidFileStopsACommandWithStatusOne() throws IOException {
        Files.writeString(directory.resolve("broken.xml"), BROKEN);

        assertEquals(Cli.FAILURE, run("print", "broken.xml"));

        assertTrue(err().startsWith("cellula: broken.xml: SI expects 3 argument(s)"), err());
    }

    @Test
    @EnabledIf(value = "java.awt.GraphicsEnvironment#isHeadless", disabledReason = "would open real windows")
    void runNeedsADisplay() {
        assertEquals(Cli.FAILURE, run("run", "growth.xml"));

        assertTrue(err().contains("no display available"), err());
    }

    @Test
    @EnabledIf(value = "java.awt.GraphicsEnvironment#isHeadless", disabledReason = "would open real windows")
    void theMenuListsTheFilesAndLaunchesTheChosenOne() throws IOException {
        Files.writeString(directory.resolve("broken.xml"), BROKEN);

        assertEquals(Cli.SUCCESS, runWithInput("7\nhello\n1\nq\n"));

        String text = out();
        assertTrue(text.contains("Automaton definitions in " + directory.toAbsolutePath().normalize()), text);
        assertTrue(text.contains("[!] broken.xml"), text);
        assertTrue(text.contains("invalid: SI expects 3 argument(s) but got 2"), text);
        assertTrue(text.contains("[1] growth.xml"), text);
        assertTrue(text.contains("[2] volume.xml"), text);
        assertTrue(text.contains("'7' is not one of the choices."), text);
        assertTrue(text.contains("'hello' is not one of the choices."), text);
        // Without a display the chosen simulation is played as text.
        assertTrue(text.contains("Growth (1D · 9 · fixed boundary)\nGenerations 0 to 32, one per line:\n····█····\n"), text);
    }

    @Test
    @EnabledIf(value = "java.awt.GraphicsEnvironment#isHeadless", disabledReason = "would open real windows")
    void theMenuEndsWithItsInput() {
        assertEquals(Cli.SUCCESS, runWithInput("", "menu", "--text"));
        assertEquals(Cli.SUCCESS, runWithInput("r\n", "--dir", "."));

        assertTrue(out().contains("[1] growth.xml"), out());
    }

    @Test
    @EnabledIf(value = "java.awt.GraphicsEnvironment#isHeadless", disabledReason = "would open real windows")
    void theMenuCopesWithAnEmptyDirectory() throws IOException {
        Path empty = Files.createDirectory(directory.resolve("empty"));

        assertEquals(Cli.SUCCESS, runWithInput("q\n", "menu", "--dir", empty.toString()));

        assertTrue(out().contains("(none: a definition is an XML file whose root element is <automaton>)"), out());
    }

    @Test
    void exploreReportsAndSavesTheRulesItFinds() throws Exception {
        assertEquals(Cli.SUCCESS, run("explore", "--steps", "127", "--top", "3", "--write", "found"));

        String text = out();
        assertTrue(text.contains("Explored the 64 totalistic radius-2 rules for 127 generations each"), text);
        assertTrue(text.contains("Fractal pictures, by increasing box-counting dimension"), text);
        assertTrue(text.contains("Chaotic pictures, by decreasing compression ratio"), text);

        // What the explorer writes is an ordinary definition, readable like any other.
        for (String kind : new String[] {"fractal", "chaotic"}) {
            AutomatonDefinition found = XmlDefinitionReader.read(directory.resolve("found/radius2-" + kind + ".xml"));
            assertEquals(1, found.dimension());
            assertEquals(4 * 127 + 1, found.shape().size(0));
            assertFalse(found.neighborhoods().isEmpty());
        }
    }

    @Test
    void exploreCanSampleLargerFamilies() {
        assertEquals(Cli.SUCCESS, run("explore", "--family", "symmetric", "--samples", "200", "--steps", "63", "--seed", "1"));

        assertTrue(out().contains("Explored 200 symmetric radius-2 rules for 63 generations each"), out());
    }
}
