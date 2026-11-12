package fr.uspn.cellula.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.uspn.cellula.config.DefinitionLibrary.Entry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DefinitionLibraryTest {

    private static String definition(String name, String rule) {
        return "<automaton name=\"" + name + "\"><dimension>1</dimension><size>5</size><rule>" + rule
                + "</rule><initial>(2)</initial></automaton>";
    }

    @Test
    void listsTheDefinitionsOfADirectorySortedByFileName(@TempDir Path directory) throws IOException {
        Files.writeString(directory.resolve("zebra.xml"), definition("Zebra", "COMPTER(G2)"));
        Files.writeString(directory.resolve("alpha.xml"), definition("Alpha", "COMPTER(G0)"));
        Files.writeString(directory.resolve("pom.xml"), "<project/>");
        Files.writeString(directory.resolve("readme.txt"), definition("Not XML by name", "1"));
        Files.createDirectory(directory.resolve("nested.xml"));
        Files.createDirectory(directory.resolve("sub"));
        Files.writeString(directory.resolve("sub/hidden.xml"), definition("Hidden", "1"));

        List<Entry> entries = DefinitionLibrary.scan(directory);

        assertEquals(List.of("alpha.xml", "zebra.xml"), entries.stream().map(Entry::fileName).toList());
        Entry.Loaded alpha = assertInstanceOf(Entry.Loaded.class, entries.get(0));
        assertEquals("Alpha", alpha.definition().name());
    }

    @Test
    void invalidDefinitionsAreListedWithTheReason(@TempDir Path directory) throws IOException {
        Files.writeString(directory.resolve("broken.xml"), definition("Broken", "COMPTER(G8)"));

        List<Entry> entries = DefinitionLibrary.scan(directory);

        Entry.Broken broken = assertInstanceOf(Entry.Broken.class, entries.get(0));
        assertEquals("broken.xml", broken.fileName());
        assertTrue(broken.problem().startsWith("unknown neighborhood 'G8' for a 1-dimensional grid"), broken.problem());
    }

    @Test
    void anEmptyDirectoryHasNoEntry(@TempDir Path directory) throws IOException {
        assertTrue(DefinitionLibrary.scan(directory).isEmpty());
    }

    @Test
    void aMissingDirectoryIsAnError(@TempDir Path directory) {
        assertThrows(IOException.class, () -> DefinitionLibrary.scan(directory.resolve("nowhere")));
    }
}
