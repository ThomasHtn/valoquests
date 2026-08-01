package io.github.thomashtn.valorant.tracker.match.exception;

import io.github.thomashtn.valorant.tracker.shared.exception.ResourceNotFoundException;

/**
 * Indicates that a requested match does not exist.
 */
public class MatchNotFoundException extends ResourceNotFoundException {

    /**
     * Creates an exception for the requested match identifier.
     *
     * @param matchId missing match identifier
     */
    public MatchNotFoundException(Long matchId) {
        super("Match not found with id: " + matchId);
    }
}
