package io.github.thomashtn.valorant.tracker.player.exception;

import io.github.thomashtn.valorant.tracker.shared.exception.ResourceNotFoundException;

/**
 * Indicates that a requested tracked player does not exist.
 */
public class PlayerNotFoundException extends ResourceNotFoundException {

    /**
     * Creates an exception for the requested player identifier.
     *
     * @param playerId missing player identifier
     */
    public PlayerNotFoundException(Long playerId) {
        super("Player not found with id: " + playerId);
    }
}
