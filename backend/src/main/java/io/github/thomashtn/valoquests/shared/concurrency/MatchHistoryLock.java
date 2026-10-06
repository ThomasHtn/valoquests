package io.github.thomashtn.valoquests.shared.concurrency;

import io.github.thomashtn.valoquests.shared.exception.ConflictException;
import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * Lets one job at a time write the match history or the data derived from it.
 *
 * <p>In-process is enough for a single instance. Entry points take it outside any transaction so it is
 * released only after commit; a semaphore because a manual sync is released by another thread.
 */
@Component
public class MatchHistoryLock {

    /**
     * Message returned to an administrator whose request arrives while another job runs.
     */
    private static final String BUSY_MESSAGE =
        "A synchronization, weekly rollover, daily tick or other match history maintenance is already "
            + "running. Wait for it to finish before starting another one.";

    /**
     * Single permit held by the running job.
     */
    private final Semaphore permit = new Semaphore(1);

    /**
     * Runs the job now, unless another guarded job is running.
     *
     * @param job work to run while holding the lock
     * @return {@code false} when the job was not run because the lock was taken
     */
    public boolean runIfFree(Runnable job) {
        if (!permit.tryAcquire()) {
            return false;
        }
        runHoldingPermit(job);
        return true;
    }

    /**
     * Runs the job once the running guarded job, if any, has finished.
     *
     * @param job     work to run while holding the lock
     * @param maxWait longest time to wait for the lock
     * @return {@code false} when the lock was still taken after {@code maxWait}
     * @throws InterruptedException when the thread is interrupted while waiting
     */
    public boolean runWhenFree(Runnable job, Duration maxWait) throws InterruptedException {
        if (!permit.tryAcquire(maxWait.toMillis(), TimeUnit.MILLISECONDS)) {
            return false;
        }
        runHoldingPermit(job);
        return true;
    }

    /**
     * Runs the job now, or refuses it when another guarded job is running.
     *
     * @param job work to run while holding the lock
     * @throws ConflictException when the lock is taken
     */
    public void runOrReject(Runnable job) {
        if (!runIfFree(job)) {
            throw new ConflictException(BUSY_MESSAGE);
        }
    }

    /**
     * Runs a job returning a result now, or refuses it when another guarded job is running.
     *
     * @param job work to run while holding the lock
     * @param <T> type of the job's result
     * @return the job's result
     * @throws ConflictException when the lock is taken
     */
    public <T> T supplyOrReject(Supplier<T> job) {
        acquireOrReject();
        try {
            return job.get();
        } finally {
            permit.release();
        }
    }

    /**
     * Takes the lock for a job running on another thread, which must call {@link #release()}.
     *
     * @throws ConflictException when the lock is taken
     */
    public void acquireOrReject() {
        if (!permit.tryAcquire()) {
            throw new ConflictException(BUSY_MESSAGE);
        }
    }

    /**
     * Releases the lock taken by {@link #acquireOrReject()}.
     */
    public void release() {
        permit.release();
    }

    /**
     * Runs the job, then releases the permit the caller has just acquired.
     *
     * @param job work to run
     */
    private void runHoldingPermit(Runnable job) {
        try {
            job.run();
        } finally {
            permit.release();
        }
    }
}
