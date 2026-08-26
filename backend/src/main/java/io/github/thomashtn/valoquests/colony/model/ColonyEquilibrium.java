package io.github.thomashtn.valoquests.colony.model;

/**
 * The state a colony settles on when one day's gains repeat indefinitely.
 *
 * <p>Carries both gauges and not only the population, because the level a gauge stops falling at is
 * what makes its bar readable. The limiting gauge settles far below the health it produces — a squad
 * holding 57% of capacity leaves that gauge sitting around 33 — so the raw figure reads as a famine
 * unless the level it is supposed to sit at is shown beside it.
 *
 * @param food       level the Food gauge stops falling at
 * @param energy     level the Energy gauge stops falling at
 * @param population population the colony plateaus at
 */
public record ColonyEquilibrium(double food, double energy, double population) {
}
