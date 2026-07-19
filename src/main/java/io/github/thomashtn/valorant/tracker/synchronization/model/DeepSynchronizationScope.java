package io.github.thomashtn.valorant.tracker.synchronization.model;

/**
 * Defines how far a deep synchronization must browse through a player's
 * match history.
 */
public enum DeepSynchronizationScope {

    /**
     * Imports only matches belonging to the current competitive season.
     */
    CURRENT_SEASON,

    /**
     * Imports every match available through the Henrik API.
     */
    ALL_HISTORY
}
