package fr.uspn.cellula.config;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The automaton definition files available in a directory.
 *
 * <p>
 * This is the model behind the start-up menu required by the subject, which
 * lists every input
 * file of the current directory. Files that claim to be definitions but are
 * invalid are not
 * silently dropped: they are listed as {@link Entry.Broken broken}, with the
 * reason, so that the
 * author of a file with a typo is told about it instead of wondering where the
 * file went.
 */
public final class DefinitionLibrary {

    private DefinitionLibrary() {
    }

    /** A definition file found in a directory. */
    public sealed interface Entry {

        /** Returns the path of the file. */
        Path file();

        /** Returns the name of the file, without its directory. */
        default String fileName() {
            return file().getFileName().toString();
        }

        /**
         * A file that was read successfully.
         *
         * @param file       path of the file
         * @param definition the definition it holds
         */
        record Loaded(Path file, AutomatonDefinition definition) implements Entry {
        }

        /**
         * A file that looks like a definition but could not be used.
         *
         * @param file    path of the file
         * @param problem why it was rejected
         */
        record Broken(Path file, String problem) implements Entry {
        }
    }

    /**
     * Lists the definition files of a directory, sorted by file name.
     *
     * <p>
     * Only the {@code *.xml} files directly in the directory whose root element is
     * {@code <automaton>} are considered.
     *
     * @param directory the directory to scan
     * @return one entry per definition file
     * @throws IOException if the directory cannot be listed
     */
    public static List<Entry> scan(Path directory) throws IOException {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.xml")) {
            for (Path file : stream) {
                if (Files.isRegularFile(file) && XmlDefinitionReader.looksLikeDefinition(file)) {
                    files.add(file);
                }
            }
        }
        files.sort(Comparator.comparing(file -> file.getFileName().toString()));

        List<Entry> entries = new ArrayList<>(files.size());
        for (Path file : files) {
            entries.add(load(file));
        }
        return entries;
    }

    /** Reads one file, turning a failure into a {@link Entry.Broken} entry. */
    public static Entry load(Path file) {
        try {
            return new Entry.Loaded(file, XmlDefinitionReader.read(file));
        } catch (DefinitionException e) {
            String prefix = file.getFileName() + ": ";
            String message = e.getMessage();
            return new Entry.Broken(
                    file, message.startsWith(prefix) ? message.substring(prefix.length()) : message);
        }
    }
}
