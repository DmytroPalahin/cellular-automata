package fr.uspn.cellula.cli;

import fr.uspn.cellula.config.AutomatonDefinition;
import fr.uspn.cellula.config.InitialState;
import fr.uspn.cellula.config.XmlDefinitionWriter;
import fr.uspn.cellula.explore.RuleExplorer;
import fr.uspn.cellula.explore.RuleExplorer.Candidate;
import fr.uspn.cellula.explore.RuleExplorer.Family;
import fr.uspn.cellula.explore.RuleExplorer.Verdict;
import fr.uspn.cellula.explore.SpaceTimeDiagram;
import fr.uspn.cellula.explore.TableRule;
import fr.uspn.cellula.grid.Coordinates;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * {@code cellula explore}: searches the one-dimensional rules of radius 2 for fractal or chaotic
 * ones, which is the bonus question of the subject.
 */
final class ExploreCommand implements Command {

    @Override
    public String name() {
        return "explore";
    }

    @Override
    public String summary() {
        return "Search radius-2 one-dimensional rules for fractal or chaotic ones";
    }

    @Override
    public String usage() {
        return """
                Usage: cellula explore [--family FAMILY] [--samples N] [--seed S] [--steps T]
                                       [--top K] [--write DIR]

                Runs many one-dimensional rules whose neighborhood is the two cells on the left,
                the cell itself and the two cells on the right, each from a single live cell, and
                reports those whose space-time picture is fractal or chaotic. Rules that could be
                expressed, or whose picture could be obtained, with the left/center/right
                neighborhood alone are discarded.

                Options:
                      --family FAMILY   totalistic (the 64 rules depending only on the number of
                                        live cells; default), symmetric or general (random
                                        samples of the 2^20 mirror-symmetric rules, or of all
                                        2^32 rules)
                      --samples N       number of rules drawn for sampled families (default: 20000)
                      --seed S          seed of the sampling, for reproducible searches (default: 2024)
                  -n, --steps T         generations each rule is run for (default: 255)
                      --top K           number of rules listed per category (default: 5)
                      --write DIR       save the best fractal and the best chaotic rule as
                                        definition files in DIR""";
    }

    @Override
    public int run(List<String> arguments, Console console) throws UsageException, IOException {
        Arguments parsed = Arguments.parse(
                arguments, Set.of("--family", "--samples", "--seed", "--steps", "--top", "--write"), Set.of());
        parsed.requireNoPositional();
        Family family = family(parsed.value("--family").orElse("totalistic"));
        int samples = parsed.intValue("--samples", 20_000, 1, 5_000_000);
        long seed = parsed.longValue("--seed", 2024, Long.MIN_VALUE, Long.MAX_VALUE);
        int steps = parsed.intValue("--steps", 255, 32, 4095);
        int top = parsed.intValue("--top", 5, 1, 1000);

        RuleExplorer explorer = new RuleExplorer(steps);
        List<Candidate> found = explorer.search(family, samples, seed);
        List<Candidate> fractal = found.stream().filter(c -> c.verdict() == Verdict.FRACTAL).toList();
        List<Candidate> chaotic = found.stream().filter(c -> c.verdict() == Verdict.CHAOTIC).toList();

        console.out().printf(Locale.ROOT,
                "Explored %s radius-2 rules for %d generations each, from a single live cell.%n"
                        + "Genuinely radius-2 rules found: %d fractal, %d chaotic.%n",
                family == Family.TOTALISTIC ? "the 64 totalistic" : samples + " " + family.name().toLowerCase(Locale.ROOT),
                steps, fractal.size(), chaotic.size());
        table(console, "Fractal pictures, by increasing box-counting dimension", fractal, top);
        table(console, "Chaotic pictures, by decreasing compression ratio", chaotic, top);

        if (parsed.value("--write").isPresent()) {
            Path directory = console.resolve(parsed.value("--write").get());
            if (!fractal.isEmpty()) {
                write(console, directory, "fractal", fractal.get(0), steps);
            }
            if (!chaotic.isEmpty()) {
                write(console, directory, "chaotic", chaotic.get(0), steps);
            }
        }
        return Cli.SUCCESS;
    }

    private static Family family(String name) throws UsageException {
        try {
            return Family.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new UsageException(
                    "unknown family '" + name + "' (expected totalistic, symmetric or general)");
        }
    }

    private static void table(Console console, String title, List<Candidate> candidates, int top) {
        console.out().println();
        console.out().println(title);
        if (candidates.isEmpty()) {
            console.out().println("  (none)");
            return;
        }
        console.out().println("  rule code    totalistic  dimension  compression");
        candidates.stream().limit(top).forEach(candidate -> console.out().printf(Locale.ROOT,
                "  %-12d %-11s %-10.3f %.3f%n",
                candidate.rule().code(),
                candidate.rule().totalisticCode().isPresent()
                        ? "code " + candidate.rule().totalisticCode().getAsInt()
                        : "-",
                candidate.boxDimension(),
                candidate.compressionRatio()));
    }

    private static void write(Console console, Path directory, String kind, Candidate candidate, int steps)
            throws IOException {
        TableRule rule = candidate.rule();
        int width = SpaceTimeDiagram.widthFor(rule.radius(), steps);
        String description = String.format(Locale.ROOT,
                "One-dimensional rule of radius 2 (Wolfram code %d%s) found by 'cellula explore'. "
                        + "From a single live cell it draws a %s picture: box-counting dimension %.3f, "
                        + "compression ratio %.3f. It cannot be obtained with the left/center/right "
                        + "neighborhood alone.",
                rule.code(),
                rule.totalisticCode().isPresent() ? ", totalistic code " + rule.totalisticCode().getAsInt() : "",
                kind, candidate.boxDimension(), candidate.compressionRatio());
        AutomatonDefinition definition = rule.toDefinition(
                "Radius-2 " + kind + " rule " + rule.code(),
                description,
                width,
                new InitialState.ExplicitCells(List.of(Coordinates.of(width / 2))));
        Path file = directory.resolve("radius2-" + kind + ".xml");
        XmlDefinitionWriter.write(definition, file);
        console.out().println();
        console.out().println("Wrote " + file);
    }
}
