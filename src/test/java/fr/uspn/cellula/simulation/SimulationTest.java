package fr.uspn.cellula.simulation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.uspn.cellula.automaton.Regime;
import fr.uspn.cellula.config.AutomatonDefinition;
import fr.uspn.cellula.config.InitialState;
import fr.uspn.cellula.grid.Coordinates;
import fr.uspn.cellula.grid.Slice;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(20)
class SimulationTest {

    private static final String LIFE = """
            SI(EQ(COMPTER(G0), 1),
               OU(EQ(COMPTER(G8*), 2), EQ(COMPTER(G8*), 3)),
               EQ(COMPTER(G8*), 3))""";

    private Simulation simulation;

    @AfterEach
    void closeSimulation() {
        if (simulation != null) {
            simulation.close();
        }
    }

    private Simulation blinker() {
        simulation = new Simulation(AutomatonDefinition.builder()
                .name("Blinker")
                .size(5, 5)
                .rule(LIFE)
                .initialState(new InitialState.ExplicitCells(
                        List.of(Coordinates.of(2, 1), Coordinates.of(2, 2), Coordinates.of(2, 3))))
                .build());
        return simulation;
    }

    /** Waits for every command issued so far, then returns the resulting state. */
    private Snapshot settle() throws Exception {
        return simulation.state().get(10, TimeUnit.SECONDS);
    }

    @Test
    void startsPausedAtGenerationZero() throws Exception {
        Snapshot snapshot = blinker().state().get(10, TimeUnit.SECONDS);

        assertEquals(0, snapshot.generation());
        assertFalse(snapshot.running());
        assertEquals(3, snapshot.population());
        assertEquals(25, snapshot.cellCount());
        assertEquals(0.12, snapshot.density(), 1e-9);
        assertEquals(Slice.parse("(:, :)"), snapshot.slice().orElseThrow());
        assertEquals(1, snapshot.frame().stateAt(2, 2));
    }

    @Test
    void stepsOneGenerationAtATime() throws Exception {
        blinker().step();
        Snapshot first = settle();
        simulation.step();
        Snapshot second = settle();

        assertEquals(1, first.generation());
        assertEquals(4, first.changedCells());
        assertEquals(1, first.frame().stateAt(1, 2));
        assertEquals(0, first.frame().stateAt(2, 1));
        assertEquals(2, second.generation());
        assertEquals(Regime.cycle(2), second.regime());
        assertFalse(second.running());
    }

    @Test
    void notifiesItsListenersOfEveryChange() throws Exception {
        BlockingQueue<Snapshot> received = new LinkedBlockingQueue<>();
        blinker().addListener(received::add);

        assertEquals(0, received.poll(10, TimeUnit.SECONDS).generation());
        simulation.step();
        assertEquals(1, received.poll(10, TimeUnit.SECONDS).generation());

        simulation.removeListener(received::add); // a different lambda instance: no effect
        simulation.step();
        assertEquals(2, received.poll(10, TimeUnit.SECONDS).generation());
    }

    @Test
    void playsOnItsOwnUntilPaused() throws Exception {
        BlockingQueue<Snapshot> received = new LinkedBlockingQueue<>();
        blinker().setInterval(1);
        simulation.addListener(received::add);

        simulation.play();
        Snapshot snapshot;
        do {
            snapshot = received.poll(10, TimeUnit.SECONDS);
        } while (snapshot.generation() < 5);
        assertTrue(snapshot.running());

        simulation.pause();
        Snapshot paused = settle();
        Thread.sleep(50);

        assertFalse(paused.running());
        assertEquals(paused.generation(), settle().generation(), "a paused simulation does not advance");
    }

    @Test
    void stopsByItselfOnceTheAutomatonHasConverged() throws Exception {
        simulation = new Simulation(AutomatonDefinition.builder()
                .name("Growth")
                .size(7)
                .rule("SUPEQ(COMPTER(G2), 1)")
                .initialState(new InitialState.ExplicitCells(List.of(Coordinates.of(3))))
                .build());
        BlockingQueue<Snapshot> received = new LinkedBlockingQueue<>();
        simulation.setInterval(0);
        simulation.addListener(received::add);

        simulation.play();
        Snapshot snapshot;
        do {
            snapshot = received.poll(10, TimeUnit.SECONDS);
        } while (snapshot.running() || snapshot.generation() == 0);

        assertEquals(Regime.FIXED_POINT, snapshot.regime());
        assertEquals(4, snapshot.generation());
        assertEquals(7, snapshot.population());
        // One-dimensional automata are published as space-time diagrams: one row per generation.
        assertEquals(5, snapshot.frame().rows());
        assertTrue(snapshot.slice().isEmpty());
    }

    @Test
    void resetGoesBackToTheInitialState() throws Exception {
        blinker().step();
        simulation.step();
        simulation.step();

        simulation.reset();
        Snapshot snapshot = settle();

        assertEquals(0, snapshot.generation());
        assertEquals(0, snapshot.changedCells());
        assertEquals(Regime.TRANSIENT, snapshot.regime());
        assertArrayEquals(new int[] {0, 1, 1, 1, 0}, Arrays.copyOfRange(snapshot.frame().states(), 10, 15));
    }

    @Test
    void resetRestoresARandomInitialStateExactlyAndReseedDrawsANewOne() throws Exception {
        simulation = new Simulation(AutomatonDefinition.builder()
                .name("Soup")
                .size(40, 40)
                .rule(LIFE)
                .initialState(new InitialState.RandomFill(50, OptionalLong.empty()))
                .build());
        int[] initial = settle().frame().states();

        simulation.step();
        simulation.reset();
        assertArrayEquals(initial, settle().frame().states());

        simulation.reseed();
        Snapshot reseeded = settle();
        assertEquals(0, reseeded.generation());
        assertFalse(Arrays.equals(initial, reseeded.frame().states()));
    }

    @Test
    void cellsCanBeDrawnByHand() throws Exception {
        blinker().clear();
        assertEquals(0, settle().population());

        simulation.setCell(0, 0, 1);
        simulation.setCell(0, 1, 1);
        simulation.setCell(0, 1, 1); // already on: no effect
        simulation.setCell(99, 99, 1); // outside the picture: ignored
        Snapshot drawn = settle();

        assertEquals(2, drawn.population());
        assertEquals(1, drawn.frame().stateAt(0, 1));

        // Drawing at generation 0 redefines the state "reset" goes back to.
        simulation.step();
        simulation.reset();
        assertEquals(2, settle().population());
    }

    @Test
    void theDisplayedSliceCanBeMoved() throws Exception {
        simulation = new Simulation(AutomatonDefinition.builder()
                .name("Volume")
                .size(4, 4, 4)
                .slice(Slice.parse("(0, :, :)"))
                .rule("ETAT()")
                .initialState(new InitialState.ExplicitCells(List.of(Coordinates.of(3, 1, 2))))
                .build());
        assertEquals(0, settle().frame().stateAt(1, 2));

        simulation.setSlice(Slice.parse("(3, :, :)"));
        Snapshot moved = settle();

        assertEquals(Slice.parse("(3, :, :)"), moved.slice().orElseThrow());
        assertEquals(1, moved.frame().stateAt(1, 2));
        assertEquals(1, moved.population(), "the population is that of the whole grid, not of the slice");

        simulation.setSlice(Slice.parse("(9, :, :)")); // does not fit: ignored
        assertEquals(Slice.parse("(3, :, :)"), settle().slice().orElseThrow());
    }

    @Test
    void commandsIssuedAfterClosingAreIgnored() {
        blinker().close();

        simulation.step();
        simulation.play();

        assertTrue(simulation.state().isCompletedExceptionally());
        assertThrows(IllegalArgumentException.class, () -> simulation.setInterval(-1));
    }
}
