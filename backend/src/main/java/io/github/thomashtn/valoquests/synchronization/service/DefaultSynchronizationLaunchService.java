package io.github.thomashtn.valoquests.synchronization.service;

import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.shared.concurrency.MatchHistoryLock;
import io.github.thomashtn.valoquests.shared.exception.ConflictException;
import io.github.thomashtn.valoquests.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Hands synchronization requests to the background executor.
 *
 * <p>A synchronization walks the Henrik match history under a rate limit of a few dozen requests
 * per minute, so a full run over the tracked squad routinely outlives an HTTP request. Rather than
 * hold the caller's connection open for minutes and leave it unable to tell a slow run from a
 * dropped one, the request is acknowledged immediately and the run is observed through the
 * synchronization history.
 *
 * <p>The {@link MatchHistoryLock} is taken here, on the request thread, so a concurrent request is
 * refused with a 409 before anything is dispatched; the background run releases it.
 */
@Service
public class DefaultSynchronizationLaunchService implements SynchronizationLaunchService {

    /**
     * Runner dispatching the synchronization to the administrative executor.
     */
    private final AsyncSynchronizationRunner runner;

    /**
     * Lock keeping this run from overlapping another synchronization, rollover or reset.
     */
    private final MatchHistoryLock matchHistoryLock;

    /**
     * Repository used to reject a request targeting an unknown player.
     */
    private final PlayerRepository playerRepository;

    /**
     * Creates the synchronization launch service.
     *
     * @param runner           asynchronous synchronization runner
     * @param matchHistoryLock lock shared by every job writing the match history
     * @param playerRepository tracked player repository
     */
    public DefaultSynchronizationLaunchService(
        AsyncSynchronizationRunner runner,
        MatchHistoryLock matchHistoryLock,
        PlayerRepository playerRepository
    ) {
        this.runner = runner;
        this.matchHistoryLock = matchHistoryLock;
        this.playerRepository = playerRepository;
    }

    @Override
    public void launchAllPlayers() {
        dispatchHoldingTheLock(runner::runAllPlayers);
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
     * @throws ConflictException         when another guarded job is running
     */
    @Override
    public void launchPlayer(long playerId) {
        if (!playerRepository.existsById(playerId)) {
            throw new ResourceNotFoundException("No tracked player exists with id " + playerId);
        }

        dispatchHoldingTheLock(() -> runner.runPlayer(playerId));
    }

    /**
     * Takes the lock, then hands the run to the background executor, which releases it.
     *
     * @throws ConflictException when another guarded job is running
     */
    private void dispatchHoldingTheLock(Runnable dispatch) {
        matchHistoryLock.acquireOrReject();
        boolean dispatched = false;
        try {
            dispatch.run();
            dispatched = true;
        } finally {
            if (!dispatched) {
                // The run never started, so nothing else will release the lock.
                matchHistoryLock.release();
            }
        }
    }
}
