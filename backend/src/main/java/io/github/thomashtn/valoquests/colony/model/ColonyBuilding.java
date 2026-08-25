package io.github.thomashtn.valoquests.colony.model;

/**
 * The structures a colony erects, from the one it starts with to the one it may never reach.
 *
 * <p>Identity and order only. Every number attached to them — the materials they cost and the capacity
 * they open — lives in {@link io.github.thomashtn.valoquests.colony.ColonyRuleset}, so rebalancing is one
 * class to edit rather than a constant hidden in an enum.
 *
 * <p>Three buildings beyond the starting camp, one effect each. An earlier design carried six, across
 * three families of effects, which came to eighteen numbers to calibrate before a single run had been
 * played.
 */
public enum ColonyBuilding {

    /**
     * What a colony starts with, at no cost.
     */
    CAMP,

    /**
     * First milestone, reachable on a mediocre run.
     */
    BARRACKS,

    /**
     * Second milestone, the mark of a run that was actually held.
     */
    RESIDENTIAL_QUARTER,

    /**
     * Prestige tier, calibrated to fall in week nine at the earliest.
     */
    CITADEL
}
