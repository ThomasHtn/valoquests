package io.github.thomashtn.valoquests.player.model;

/**
 * Defines the supported player status values.
 */
public enum PlayerStatus {

    /**
     * Player takes part in full, and joins the roster of the next campaign opened.
     */
    ACTIVE,

    /**
     * Player is still synchronized and completes challenges, but never ranks nor joins a new campaign roster.
     */
    INACTIVE,

    /**
     * Player removed from the roster, neither synchronized nor listed, but kept for past rosters and rankings.
     */
    ARCHIVED
}
