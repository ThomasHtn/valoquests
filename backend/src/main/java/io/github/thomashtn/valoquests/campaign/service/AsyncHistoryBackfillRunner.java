package io.github.thomashtn.valoquests.campaign.service;

import io.github.thomashtn.valoquests.shared.config.AsyncConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Runs a history backfill on the administrative executor instead of the request thread.
 *
 * <p>Nine months of history for a whole squad is thousands of Henrik calls under a rate limit of a
 * few dozen a minute: the request is acknowledged and the walk is watched through the
 * synchronization history, exactly as an ordinary synchronization is.
 *
 * <p>Separate from its caller because {@code @Async} is applied by a proxy: a self-call would run
 * the walk inline and hold the connection open for the whole of it.
 */
@Service
public class AsyncHistoryBackfillRunner {

    /**
     * Application logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncHistoryBackfillRunner.class);

    /**
     * Service walking the history.
     */
    private final HistoryBackfillService backfillService;

    /**
     * Creates the asynchronous backfill runner.
     *
     * @param backfillService history backfill service
     */
    public AsyncHistoryBackfillRunner(HistoryBackfillService backfillService) {
        this.backfillService = backfillService;
    }

    /**
     * Walks the calibration window in the background.
     *
     * <p>Failures are logged rather than propagated: there is no caller left to receive them, and
     * the execution row already carries the failed status for the administration screen to read.
     */
    @Async(AsyncConfig.ADMIN_TASK_EXECUTOR)
    public void run() {
        try {
            backfillService.backfill();
        } catch (RuntimeException exception) {
            LOGGER.error("History backfill failed", exception);
        }
    }
}
