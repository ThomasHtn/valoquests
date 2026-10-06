package io.github.thomashtn.valoquests.synchronization.service;

import io.github.thomashtn.valoquests.shared.concurrency.MatchHistoryLock;
import io.github.thomashtn.valoquests.shared.config.AsyncConfig;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Runs a synchronization on the administrative executor instead of the request thread.
 *
 * <p>Separate from {@link SynchronizationLaunchService} on purpose: {@code @Async} is applied by a
 * proxy, so a self-call inside a single class would run inline and defeat the whole point. The
 * caller keeps the guard, this class keeps the dispatch.
 *
 * <p>Each run releases the {@link MatchHistoryLock} its caller took before dispatching it.
 */
@Service
public class AsyncSynchronizationRunner {

    /**
     * Application logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncSynchronizationRunner.class);

    /**
     * Service executing the synchronization itself.
     */
    private final SynchronizationCommandService synchronizationCommandService;

    /**
     * Lock taken by the caller before dispatching, released once the run ends.
     */
    private final MatchHistoryLock matchHistoryLock;

    /**
     * Creates the asynchronous synchronization runner.
     *
     * @param synchronizationCommandService synchronization command service
     * @param matchHistoryLock              lock shared by every job writing the match history
     */
    public AsyncSynchronizationRunner(
        SynchronizationCommandService synchronizationCommandService,
        MatchHistoryLock matchHistoryLock
    ) {
        this.synchronizationCommandService = synchronizationCommandService;
        this.matchHistoryLock = matchHistoryLock;
    }

    /**
     * Synchronizes every tracked player in the background.
     *
     * <p>Failures are logged rather than propagated: there is no caller left to receive them, and
     * the execution row already carries the failed status and its message for the administration
     * screen to read.
     */
    @Async(AsyncConfig.ADMIN_TASK_EXECUTOR)
    public void runAllPlayers() {
        try {
            synchronizationCommandService.synchronizeAllPlayers(SynchronizationTrigger.MANUAL);
        } catch (RuntimeException exception) {
            LOGGER.error("Manual synchronization of every player failed", exception);
        } finally {
            matchHistoryLock.release();
        }
    }

    /**
     * Synchronizes one tracked player in the background.
     *
     * @param playerId tracked player identifier
     */
    @Async(AsyncConfig.ADMIN_TASK_EXECUTOR)
    public void runPlayer(long playerId) {
        try {
            synchronizationCommandService.synchronizePlayer(playerId);
        } catch (RuntimeException exception) {
            LOGGER.error("Manual synchronization of player {} failed", playerId, exception);
        } finally {
            matchHistoryLock.release();
        }
    }
}
