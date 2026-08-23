package io.github.thomashtn.valoquests.player.model;

/**
 * Describes what a deletion request actually did to a player.
 */
public enum PlayerDeletionOutcome {

    /**
     * The player row and its satellite data were removed for good.
     */
    DELETED,

    /**
     * The player was archived instead, because finalized weeks depend on it.
     */
    ARCHIVED
}
