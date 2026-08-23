package io.github.thomashtn.valoquests.player.model;

/**
 * Defines the supported player status values.
 */
public enum PlayerStatus {

    /**
     * Player takes part in the competition in full.
     */
    ACTIVE,

    /**
     * Player is still tracked and synchronized, and still completes challenges individually, but
     * never contributes boss damage and never consumes a ranking slot.
     */
    INACTIVE,

    /**
     * Player was removed from the roster while keeping the history it took part in.
     *
     * <p>Not synchronized, and absent from every public listing, but still resolvable by
     * identifier: a finalized week may credit it with the kill that ended a boss, and a stored
     * ranking may hold its position. Deleting the row outright would leave those references
     * pointing at nothing, so an archived player is what a deletion becomes once the player has
     * fought a boss. The status is reversible, which is what makes archiving acceptable in the
     * first place.
     */
    ARCHIVED
}
