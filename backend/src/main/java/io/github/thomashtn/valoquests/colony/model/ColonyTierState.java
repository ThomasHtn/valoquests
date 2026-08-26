package io.github.thomashtn.valoquests.colony.model;

/**
 * Where one step of the ladder stands relative to the town.
 */
public enum ColonyTierState {

    /**
     * Already crossed.
     */
    REACHED,

    /**
     * The step the town currently sits in.
     */
    CURRENT,

    /**
     * Still ahead.
     */
    LOCKED
}
