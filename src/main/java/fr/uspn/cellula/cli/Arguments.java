package fr.uspn.cellula.cli;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The parsed command line of one command: positional arguments, options with a value
 * ({@code --steps 100} or {@code --steps=100}) and flags ({@code --ascii}).
 */
final class Arguments {

    /** Short spellings of the most common options. */
    private static final Map<String, String> ALIASES =
            Map.of("-d", "--dir", "-n", "--steps", "-o", "--output", "-h", "--help");

    private final List<String> positionals = new ArrayList<>();
    private final Map<String, String> values = new HashMap<>();
    private final Set<String> flags = new HashSet<>();

    private Arguments() {}

    /**
     * Parses the arguments of a command.
     *
     * @param arguments the words following the command name
     * @param valueOptions the options that take a value, e.g. {@code --steps}
     * @param flagOptions the options that do not, e.g. {@code --ascii}
     * @throws UsageException if an option is unknown, repeated or lacks its value
     */
    static Arguments parse(List<String> arguments, Set<String> valueOptions, Set<String> flagOptions)
            throws UsageException {
        Arguments parsed = new Arguments();
        boolean optionsEnded = false;
        for (int index = 0; index < arguments.size(); index++) {
            String word = arguments.get(index);
            if (optionsEnded || !word.startsWith("-") || word.equals("-") || isNegativeNumber(word)) {
                parsed.positionals.add(word);
                continue;
            }
            if (word.equals("--")) {
                optionsEnded = true;
                continue;
            }
            String name = word;
            String inlineValue = null;
            int equals = word.indexOf('=');
            if (word.startsWith("--") && equals > 0) {
                name = word.substring(0, equals);
                inlineValue = word.substring(equals + 1);
            }
            name = ALIASES.getOrDefault(name, name);

            if (flagOptions.contains(name) || name.equals("--help")) {
                if (inlineValue != null) {
                    throw new UsageException("option " + name + " does not take a value");
                }
                parsed.flags.add(name);
            } else if (valueOptions.contains(name)) {
                String value = inlineValue;
                if (value == null) {
                    if (++index >= arguments.size()) {
                        throw new UsageException("option " + name + " needs a value");
                    }
                    value = arguments.get(index);
                }
                if (parsed.values.put(name, value) != null) {
                    throw new UsageException("option " + name + " is given twice");
                }
            } else {
                throw new UsageException("unknown option " + word);
            }
        }
        return parsed;
    }

    private static boolean isNegativeNumber(String word) {
        return word.matches("-\\d+");
    }

    /** Returns the arguments that are not options, in order. */
    List<String> positionals() {
        return positionals;
    }

    /** Returns the value of an option, if it was given. */
    Optional<String> value(String name) {
        return Optional.ofNullable(values.get(name));
    }

    /** Tells whether a flag was given. */
    boolean has(String flag) {
        return flags.contains(flag);
    }

    /**
     * Returns the integer value of an option.
     *
     * @param name the option
     * @param fallback the value when the option is absent
     * @param min smallest accepted value
     * @param max largest accepted value
     * @throws UsageException if the value is not an integer within the bounds
     */
    int intValue(String name, int fallback, int min, int max) throws UsageException {
        return (int) longValue(name, fallback, min, max);
    }

    /** Same as {@link #intValue} for values that may not fit in an {@code int}. */
    long longValue(String name, long fallback, long min, long max) throws UsageException {
        String text = values.get(name);
        if (text == null) {
            return fallback;
        }
        try {
            long value = Long.parseLong(text.strip());
            if (value >= min && value <= max) {
                return value;
            }
        } catch (NumberFormatException e) {
            // Reported below, like an out-of-range value.
        }
        throw new UsageException(
                "option " + name + " expects an integer between " + min + " and " + max
                        + ", got '" + text + "'");
    }

    /**
     * Returns the only positional argument.
     *
     * @param what what the argument is, for the error message
     * @throws UsageException if there is not exactly one positional argument
     */
    String single(String what) throws UsageException {
        if (positionals.size() != 1) {
            throw new UsageException("expected exactly one " + what);
        }
        return positionals.get(0);
    }

    /**
     * Checks that no positional argument was given.
     *
     * @throws UsageException otherwise
     */
    void requireNoPositional() throws UsageException {
        if (!positionals.isEmpty()) {
            throw new UsageException("unexpected argument '" + positionals.get(0) + "'");
        }
    }
}
