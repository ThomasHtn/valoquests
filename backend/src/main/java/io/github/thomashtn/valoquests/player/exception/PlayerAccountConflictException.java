package io.github.thomashtn.valoquests.player.exception;

/**
 * Indicates that a Riot account is already associated with another tracked
 * player.
 */
public class PlayerAccountConflictException extends RuntimeException {

    /**
     * Creates a Riot account conflict exception.
     *
     * @param message conflict description
     */
    public PlayerAccountConflictException(String message) {
        super(message);
    }
}
