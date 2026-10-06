package io.github.thomashtn.valoquests.match.model;

/**
 * What importing one entry of a Henrik match-history page did, counted into a {@link MatchImportResult}.
 */
public enum MatchImportOutcome {

    /**
     * A new player-match association was inserted.
     */
    IMPORTED,

    /**
     * The valid entry was already stored for the player.
     */
    ALREADY_KNOWN,

    /**
     * The entry was malformed, incomplete or did not involve the player.
     */
    REJECTED,

    /**
     * The entry was valid but its game mode is not imported.
     */
    SKIPPED
}
