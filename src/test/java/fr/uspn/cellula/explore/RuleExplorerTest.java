package fr.uspn.cellula.explore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.uspn.cellula.explore.RuleExplorer.Candidate;
import fr.uspn.cellula.explore.RuleExplorer.Family;
import fr.uspn.cellula.explore.RuleExplorer.Verdict;
import fr.uspn.cellula.grid.Frame;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuleExplorerTest {

    private final RuleExplorer explorer = new RuleExplorer(255);

    @Test
    void aDiagramRecordsEveryGeneration() {
        SpaceTimeDiagram diagram = SpaceTimeDiagram.fromSingleCell(TableRule.of(1, 254), 3);

        assertEquals(4, diagram.generations());
        assertEquals(7, diagram.width());
        assertEquals(1, diagram.population(0));
        assertEquals(7, diagram.population(3));
        assertEquals(1, diagram.cell(1, 2));
        assertEquals(0, diagram.cell(1, 1));

        Frame frame = diagram.toFrame();
        assertEquals(4, frame.rows());
        assertEquals(1, frame.stateAt(0, 3));
        assertEquals(diagram, SpaceTimeDiagram.of(TableRule.of(1, 254), SpaceTimeDiagram.singleCell(7), 3));
    }

    @Test
    void theSierpinskiTriangleHasDimensionLog3OverLog2() {
        double dimension = SpaceTimeDiagram.fromSingleCell(TableRule.of(1, 18), 255).boxDimension();

        assertEquals(Math.log(3) / Math.log(2), dimension, 0.01);
    }

    @Test
    void simpleShapesHaveIntegerDimensions() {
        // Rule 4 keeps a lone cell forever (a vertical line); rule 254 fills a triangle.
        assertEquals(1.0, SpaceTimeDiagram.fromSingleCell(TableRule.of(1, 4), 255).boxDimension(), 0.02);
        assertEquals(2.0, SpaceTimeDiagram.fromSingleCell(TableRule.of(1, 254), 255).boxDimension(), 0.06);
        assertEquals(0.0, SpaceTimeDiagram.fromSingleCell(TableRule.of(1, 0), 255).boxDimension(), 1e-9);
    }

    @Test
    void disorderResistsCompression() {
        double chaotic = SpaceTimeDiagram.fromSingleCell(TableRule.of(1, 30), 255).compressionRatio();
        double nested = SpaceTimeDiagram.fromSingleCell(TableRule.of(1, 18), 255).compressionRatio();
        double uniform = SpaceTimeDiagram.fromSingleCell(TableRule.of(1, 254), 255).compressionRatio();

        assertTrue(chaotic > 3 * nested, chaotic + " vs " + nested);
        assertTrue(chaotic > 3 * uniform, chaotic + " vs " + uniform);
    }

    @Test
    void classifiesTheClassicElementaryRules() {
        assertEquals(Verdict.EXTINCT, explorer.assess(TableRule.of(1, 0)).verdict());
        assertEquals(Verdict.REGULAR, explorer.assess(TableRule.of(1, 4)).verdict());
        assertEquals(Verdict.REGULAR, explorer.assess(TableRule.of(1, 254)).verdict());
        assertEquals(Verdict.FRACTAL, explorer.assess(TableRule.of(1, 18)).verdict());
        assertEquals(Verdict.FRACTAL, explorer.assess(TableRule.of(1, 90)).verdict());
        assertEquals(Verdict.FRACTAL, explorer.assess(TableRule.of(1, 150)).verdict());
        assertEquals(Verdict.CHAOTIC, explorer.assess(TableRule.of(1, 30)).verdict());
        assertEquals(Verdict.CHAOTIC, explorer.assess(TableRule.of(1, 45)).verdict());
    }

    @Test
    void findsFractalAndChaoticRulesAmongTheTotalisticOnes() {
        List<Candidate> found = explorer.search(Family.TOTALISTIC, 0, 0);

        List<Candidate> fractal = found.stream().filter(c -> c.verdict() == Verdict.FRACTAL).toList();
        List<Candidate> chaotic = found.stream().filter(c -> c.verdict() == Verdict.CHAOTIC).toList();
        assertFalse(fractal.isEmpty());
        assertFalse(chaotic.isEmpty());
        assertEquals(found.size(), fractal.size() + chaotic.size());
        // Fractal rules come first, by increasing dimension; chaotic ones by decreasing disorder.
        assertEquals(fractal, found.subList(0, fractal.size()));
        for (int index = 1; index < fractal.size(); index++) {
            assertTrue(fractal.get(index - 1).boxDimension() <= fractal.get(index).boxDimension());
        }
        for (int index = 1; index < chaotic.size(); index++) {
            assertTrue(chaotic.get(index - 1).compressionRatio() >= chaotic.get(index).compressionRatio());
        }
        // The parity of the five cells generalizes the Sierpiński rule and must be among them.
        assertTrue(fractal.stream().anyMatch(c -> c.rule().equals(TableRule.totalistic(2, 0b101010))));
    }

    @Test
    void everyReportedRuleGenuinelyNeedsRadiusTwo() {
        for (Candidate candidate : explorer.search(Family.TOTALISTIC, 0, 0)) {
            assertFalse(candidate.rule().fitsRadius(1), candidate.toString());
            assertFalse(explorer.reproducibleWithRadiusOne(candidate.rule()), candidate.toString());
            assertEquals(0, candidate.rule().output(0), "an empty row stays empty");
        }
    }

    @Test
    void detectsPicturesThatRadiusOneCanDraw() {
        // Rule 90 lifted to radius 2 draws, of course, the picture of rule 90.
        TableRule rule90 = TableRule.of(1, 90);
        long lifted = 0;
        for (int neighborhood = 0; neighborhood < 32; neighborhood++) {
            if (rule90.output(neighborhood >> 1 & 0b111) == 1) {
                lifted |= 1L << neighborhood;
            }
        }

        assertTrue(explorer.reproducibleWithRadiusOne(TableRule.of(2, lifted)));
        assertFalse(explorer.reproducibleWithRadiusOne(TableRule.totalistic(2, 0b101010)));
    }

    @Test
    void sampledSearchesAreReproducible() {
        RuleExplorer quick = new RuleExplorer(63);

        List<Candidate> first = quick.search(Family.SYMMETRIC, 300, 7);
        List<Candidate> second = quick.search(Family.SYMMETRIC, 300, 7);
        List<Candidate> general = quick.search(Family.GENERAL, 300, 7);

        assertEquals(first, second);
        assertFalse(first.isEmpty());
        assertFalse(general.isEmpty());
        for (Candidate candidate : first) {
            long code = candidate.rule().code();
            for (int neighborhood = 0; neighborhood < 32; neighborhood++) {
                int mirror = Integer.reverse(neighborhood) >>> 27;
                assertEquals(code >>> neighborhood & 1, code >>> mirror & 1, "rule " + code + " is not symmetric");
            }
        }
    }

    @Test
    void rejectsSearchesThatCannotWork() {
        assertThrows(IllegalArgumentException.class, () -> new RuleExplorer(8));
        assertThrows(IllegalArgumentException.class, () -> explorer.search(Family.GENERAL, 0, 1));
        assertEquals(255, explorer.steps());
    }
}
