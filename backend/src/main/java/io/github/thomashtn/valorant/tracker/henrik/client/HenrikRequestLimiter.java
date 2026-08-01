package io.github.thomashtn.valorant.tracker.henrik.client;

import io.github.thomashtn.valorant.tracker.henrik.config.HenrikApiProperties;
import java.time.Duration;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Component;

/**
 * Globally regulates calls sent through the Henrik API clients.
 *
 * <p>The Henrik API rate limit applies to the API key rather than to an
 * individual player or endpoint. Consequently, every account, MMR, season and
 * match-history request must share the same limiter.</p>
 *
 * <p>Requests are evenly distributed over time instead of allowing a burst of
 * thirty immediate requests followed by a long pause. This behaviour is safer
 * for scheduled synchronization jobs and avoids exhausting the quota at the
 * beginning of an execution.</p>
 */
@Component
public class HenrikRequestLimiter {

    /**
     * One-minute duration expressed in nanoseconds.
     */
    private static final long ONE_MINUTE_NANOS =
        Duration.ofMinutes(1).toNanos();

    /**
     * Fair lock preserving the order of waiting synchronization requests.
     */
    private final ReentrantLock lock = new ReentrantLock(true);

    /**
     * Minimum duration between two requests.
     */
    private final long requestIntervalNanos;

    /**
     * Earliest monotonic-clock instant at which the next request may start.
     */
    private long nextRequestAtNanos;

    /**
     * Creates the global Henrik request limiter.
     *
     * @param properties validated Henrik API configuration
     */
    public HenrikRequestLimiter(HenrikApiProperties properties) {
        long baseInterval =
            divideAndRoundUp(
                ONE_MINUTE_NANOS,
                properties.requestsPerMinute()
            );

        this.requestIntervalNanos =
            Math.addExact(
                baseInterval,
                properties.rateLimitSafetyMargin().toNanos()
            );
    }

    /**
     * Divides two positive numbers and rounds the result upward.
     *
     * @param dividend value to divide
     * @param divisor  positive divisor
     * @return rounded-up result
     */
    private static long divideAndRoundUp(
        long dividend,
        long divisor
    ) {
        return Math.addExact(dividend, divisor - 1L) / divisor;
    }

    /**
     * Waits until the next Henrik request is allowed to start.
     *
     * <p>This method is called before every physical HTTP attempt, including
     * retries generated after a temporary external failure.</p>
     *
     * @throws IllegalStateException when the waiting thread is interrupted
     */
    public void acquire() {
        lock.lock();

        try {
            waitUntilPermitIsAvailable();

            long now = System.nanoTime();

            nextRequestAtNanos =
                Math.max(now, nextRequestAtNanos)
                    + requestIntervalNanos;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Waits until the scheduled instant for the next request.
     */
    private void waitUntilPermitIsAvailable() {
        while (true) {
            long remainingNanos =
                nextRequestAtNanos - System.nanoTime();

            if (remainingNanos <= 0) {
                return;
            }

            LockSupport.parkNanos(remainingNanos);

            if (Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();

                throw new IllegalStateException(
                    "Interrupted while waiting for a Henrik API permit"
                );
            }
        }
    }
}
