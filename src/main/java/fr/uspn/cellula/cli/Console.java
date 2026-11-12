package fr.uspn.cellula.cli;

import java.io.BufferedReader;
import java.io.PrintStream;
import java.nio.file.Path;

/**
 * The outside world as seen by a command: where to print, where to read answers
 * from, and which
 * directory relative paths refer to.
 *
 * <p>
 * Commands never touch {@code System.out} or the current directory directly,
 * which lets tests
 * run them against in-memory streams and temporary directories.
 *
 * @param out              standard output
 * @param err              standard error
 * @param in               standard input
 * @param workingDirectory the directory relative paths are resolved against
 */
public record Console(PrintStream out, PrintStream err, BufferedReader in, Path workingDirectory) {

    /** Resolves a path given on the command line. */
    Path resolve(String path) {
        return workingDirectory.resolve(path).normalize();
    }
}
