package io.github.thomashtn.valoquests.player.exception;

import io.github.thomashtn.valoquests.shared.exception.ConflictException;

/**
 * Indicates that a Riot account is already associated with another tracked
 * player.
 */
public class PlayerAccountConflictException extends ConflictException {

    /**
     * Creates a Riot account conflict exception.
     *
     * @param message conflict description
     */
    public PlayerAccountConflictException(String message) {
        super(message);
    }
}
