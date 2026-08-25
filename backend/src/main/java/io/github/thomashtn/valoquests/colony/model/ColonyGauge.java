package io.github.thomashtn.valoquests.colony.model;

/**
 * The two gauges a colony's health is the geometric mean of.
 *
 * <p>Strictly independent behaviours: playing every day, and playing together. Whichever is fed less
 * alone sets the equilibrium population, while the other saturates and loses its surplus.
 */
public enum ColonyGauge {

    /**
     * Fed by the day's match damage: playing every day.
     */
    FOOD,

    /**
     * Fed by the day's turnout: playing together.
     */
    ENERGY
}
