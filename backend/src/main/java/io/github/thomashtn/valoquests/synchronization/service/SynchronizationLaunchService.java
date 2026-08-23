package io.github.thomashtn.valoquests.synchronization.service;

import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.shared.exception.ConflictException;
import io.github.thomashtn.valoquests.shared.exception.ResourceNotFoundException;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationStatus;
import io.github.thomashtn.valoquests.synchronization.repository.SynchronizationRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Accepts synchronization requests and hands them to the background executor.
 *
 * <p>A synchronization walks the Henrik match history under a rate limit of a few dozen requests
 * per minute, so a full run over the tracked squad routinely outlives an HTTP request. Rather than
 * hold the caller's connection open for minutes and leave it unable to tell a slow run from a
 * dropped one, the request is acknowledged immediately and the run is observed through the
 * synchronization history.
 */
@Service
public class SynchronizationLaunchService {

    /**
     * Statuses meaning an execution still owns the Henrik rate-limit budget.
     */
    private static final List<SynchronizationStatus> IN_PROGRESS_STATUSES =
        List.of(SynchronizationStatus.PENDING, SynchronizationStatus.RUNNING);

    /**
     * Runner dispatching the synchronization to the administrative executor.
     */
    private final AsyncSynchronizationRunner runner;

    /**
     * Repository used to detect an execution already in progress.
     */
    private final SynchronizationRepository synchronizationRepository;

    /**
     * Repository used to reject a request targeting an unknown player.
     */
    private final PlayerRepository playerRepository;

    /**
     * Creates the synchronization launch service.
     *
     * @param runner                    asynchronous synchronization runner
     * @param synchronizationRepository synchronization repository
     * @param playerRepository          tracked player repository
     */
    public SynchronizationLaunchService(
        AsyncSynchronizationRunner runner,
        SynchronizationRepository synchronizationRepository,
        PlayerRepository playerRepository
    ) {
        this.runner = runner;
        this.synchronizationRepository = synchronizationRepository;
        this.playerRepository = playerRepository;
    }

    /**
     * Accepts a synchronization of every tracked player.
     *
     * @throws ConflictException when an execution is already in progress
     */
    public void launchAllPlayers() {
        rejectWhenAlreadyRunning();

        runner.runAllPlayers();
    }

    /**
     * Accepts a synchronization of one tracked player.
     *
     * <p>The player is resolved before the request is acknowledged. Accepting a run for an unknown
     * identifier would answer 202 and report the mistake only as a failed execution, several
     * minutes later, in a history the caller has no reason to open.
     *
     * @param playerId tracked player identifier
     * @throws ResourceNotFoundException when no tracked player owns the identifier
     * @throws ConflictException         when an execution is already in progress
     */
    public void launchPlayer(long playerId) {
        if (!playerRepository.existsById(playerId)) {
            throw new ResourceNotFoundException("No tracked player exists with id " + playerId);
        }

        rejectWhenAlreadyRunning();

        runner.runPlayer(playerId);
    }

    /**
     * Determines whether an execution is currently in progress.
     *
     * @return {@code true} when a synchronization is pending or running
     */
    public boolean isSynchronizationInProgress() {
        return synchronizationRepository.existsByStatusIn(IN_PROGRESS_STATUSES);
    }

    /**
     * Refuses to start a second concurrent execution.
     *
     * @throws ConflictException when an execution is already in progress
     */
    private void rejectWhenAlreadyRunning() {
        if (isSynchronizationInProgress()) {
            throw new ConflictException(
                "A synchronization is already in progress. Wait for it to finish before starting "
                    + "another one."
            );
        }
    }
}
