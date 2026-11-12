package fr.uspn.cellula.automaton;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CycleDetectorTest {

    @Test
    void aNewConfigurationIsNotARepetition() {
        CycleDetector detector = new CycleDetector();

        assertEquals(0, detector.observe(11L, 0));
        assertEquals(0, detector.observe(22L, 1));
    }

    @Test
    void reportsTheDistanceToThePreviousSighting() {
        CycleDetector detector = new CycleDetector();
        detector.observe(11L, 0);
        detector.observe(22L, 1);
        detector.observe(33L, 2);

        assertEquals(3, detector.observe(11L, 3));
        assertEquals(3, detector.observe(22L, 4));
        assertEquals(3, detector.observe(33L, 5));
        assertEquals(3, detector.observe(11L, 6));
    }

    @Test
    void forgetsOnReset() {
        CycleDetector detector = new CycleDetector();
        detector.observe(11L, 0);

        detector.reset();

        assertEquals(0, detector.observe(11L, 1));
    }

    @Test
    void onlyRemembersABoundedWindow() {
        CycleDetector detector = new CycleDetector();
        detector.observe(-1L, 0);
        for (int generation = 1; generation <= CycleDetector.WINDOW; generation++) {
            detector.observe(generation, generation);
        }

        // The very first configuration has been pushed out of the window...
        assertEquals(0, detector.observe(-1L, CycleDetector.WINDOW + 1));
        // ...but recent ones are still known.
        assertEquals(2, detector.observe(CycleDetector.WINDOW, CycleDetector.WINDOW + 2));
    }
}
