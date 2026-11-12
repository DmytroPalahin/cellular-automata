package fr.uspn.cellula.automaton;

/**
 * What happened during one time step.
 *
 * @param generation   the generation reached by the step (the initial state is
 *                     generation 0)
 * @param changedCells number of cells whose state differs from the previous
 *                     generation
 * @param regime       the long-term behavior observed so far
 */
public record StepReport(long generation, int changedCells, Regime regime) {
}
