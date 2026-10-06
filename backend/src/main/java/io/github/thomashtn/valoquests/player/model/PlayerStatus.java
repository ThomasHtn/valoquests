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
     * Player is still tracked and synchronized, and still completes challenges individually, but
     * never consumes a ranking slot and is left out of a newly opened campaign's roster.
     */
    INACTIVE,

    /**
     * Player was removed from the roster while keeping the history it took part in.
     *
     * <p>Not synchronized, and absent from every public listing, but still resolvable by
     * identifier: a campaign roster may count it and a stored ranking may hold its position.
     * Deleting the row outright would leave those references pointing at nothing, so an archived
     * player is what a deletion becomes once the player was on a campaign roster. The status is
     * reversible, which is what makes archiving acceptable in the first place.
     */
    ARCHIVED
}
