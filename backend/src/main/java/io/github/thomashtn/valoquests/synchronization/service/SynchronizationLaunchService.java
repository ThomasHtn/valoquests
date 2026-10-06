package io.github.thomashtn.valoquests.synchronization.service;

import io.github.thomashtn.valoquests.shared.exception.ConflictException;
import io.github.thomashtn.valoquests.shared.exception.ResourceNotFoundException;

/**
 * Accepts synchronization requests and runs them in the background.
 *
 * <p>Acknowledged immediately; the run is followed through the synchronization history.
 */
public interface SynchronizationLaunchService {

    /**
     * Accepts a synchronization of every tracked player.
     *
     * @throws ConflictException when a synchronization, rollover or reset is running
     */
    void launchAllPlayers();

    /**
     * Accepts a synchronization of one tracked player.
     *
     * @param playerId tracked player identifier
     * @throws ResourceNotFoundException when no tracked player owns the identifier
     * @throws ConflictException         when a synchronization, rollover or reset is running
     */
    void launchPlayer(long playerId);
}
