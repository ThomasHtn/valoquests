package io.github.thomashtn.valoquests.shared.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Throttles repeated failed {@code X-Admin-Key} attempts per remote address.
 *
 * <p>The admin key is a single shared secret compared in constant time by {@link AdminApiKeyFilter},
 * which defeats timing attacks but not a caller simply guessing values fast enough. This tracks
 * invalid-key failures per remote address in memory and locks an address out for a short duration
 * once it crosses a small budget — enough to make brute-forcing impractical without penalizing an admin
 * who mistypes the key once or twice.
 */
@Component
public class AdminAuthRateLimiter {

    /**
     * Failed attempts allowed for one remote address before it is locked out.
     */
    private final int maxFailures;

    /**
     * Duration a remote address stays locked out from the failure that crosses {@link #maxFailures},
     * and the duration after which a failure count below the budget resets.
     */
    private final Duration lockoutDuration;

    /**
     * Clock used to time attempt windows.
     */
    private final Clock clock;

    /**
     * Number of tracked addresses beyond which expired windows are swept before a new one is added.
     *
     * <p>Far above what this application's handful of admins can produce, so a legitimate
     * mistyped key never triggers the sweep.</p>
     */
    private static final int SWEEP_THRESHOLD = 1_000;

    /**
     * Tracked failure windows, keyed by remote address.
     */
    private final ConcurrentHashMap<String, AttemptWindow> windowsByRemoteAddress =
        new ConcurrentHashMap<>();

    /**
     * Creates the administrative authentication rate limiter.
     *
     * @param maxFailures     failed attempts allowed before a remote address is locked out
     * @param lockoutDuration duration a remote address stays locked out, and the window a failure
     *                        count resets after
     * @param clock           application clock
     */
    public AdminAuthRateLimiter(
        @Value("${app.admin-rate-limit.max-failures}") int maxFailures,
        @Value("${app.admin-rate-limit.lockout-duration}") Duration lockoutDuration,
        Clock clock
    ) {
        this.maxFailures = maxFailures;
        this.lockoutDuration = lockoutDuration;
        this.clock = clock;
    }

    /**
     * Determines whether a remote address is currently locked out.
     *
     * @param remoteAddress caller's remote address
     * @return {@code true} when the address crossed the failure budget less than a lockout ago
     */
    public boolean isLockedOut(String remoteAddress) {
        AttemptWindow window = windowsByRemoteAddress.get(remoteAddress);

        if (window == null) {
            return false;
        }

        if (window.isExpired(clock.instant(), lockoutDuration)) {
            windowsByRemoteAddress.remove(remoteAddress, window);
            return false;
        }

        return window.lockedUntil() != null;
    }

    /**
     * Records an invalid-key attempt from a remote address.
     *
     * @param remoteAddress caller's remote address
     */
    public void recordFailure(String remoteAddress) {
        sweepExpiredWindows();

        Instant now = clock.instant();

        windowsByRemoteAddress.compute(
            remoteAddress,
            (key, existing) -> existing == null || existing.isExpired(now, lockoutDuration)
                ? new AttemptWindow(0, now, null).withFailureAt(now, maxFailures, lockoutDuration)
                : existing.withFailureAt(now, maxFailures, lockoutDuration)
        );
    }

    /**
     * Clears any tracked failures for a remote address, following a valid-key request.
     *
     * @param remoteAddress caller's remote address
     */
    public void recordSuccess(String remoteAddress) {
        windowsByRemoteAddress.remove(remoteAddress);
    }

    /**
     * Number of remote addresses currently tracked.
     *
     * <p>Package-private and used only by tests: sweeping reclaims memory, which no response or
     * lockout decision reveals, so this is the only way to assert it actually happens.</p>
     *
     * @return count of tracked failure windows
     */
    int trackedAddressCount() {
        return windowsByRemoteAddress.size();
    }

    /**
     * Drops windows that have outlived their lockout, once enough addresses are tracked.
     *
     * <p>A window is otherwise only discarded when its own address is seen again, so addresses that
     * fail once and never return stay in the map for good. The keys come from unauthenticated
     * callers, which makes that a slow leak an attacker can drive by rotating source addresses.
     * Sweeping keeps the map proportional to the addresses currently failing rather than to every
     * address that ever failed.</p>
     */
    private void sweepExpiredWindows() {
        if (windowsByRemoteAddress.size() < SWEEP_THRESHOLD) {
            return;
        }

        Instant now = clock.instant();

        windowsByRemoteAddress.values()
            .removeIf(window -> window.isExpired(now, lockoutDuration));
    }

    /**
     * Failure count accumulated by a remote address since its first recent failure.
     *
     * @param failureCount   number of invalid-key attempts recorded in this window
     * @param firstFailureAt instant the window started at
     * @param lockedUntil    end of the lockout, once the failure budget has been crossed
     */
    private record AttemptWindow(
        int failureCount,
        Instant firstFailureAt,
        @Nullable Instant lockedUntil
    ) {

        /**
         * Returns a window with one additional failure, starting the lockout when it crosses the budget.
         *
         * @param now     instant of the failure
         * @param budget  failed attempts allowed before a lockout
         * @param lockout duration of the lockout
         * @return updated attempt window
         */
        AttemptWindow withFailureAt(Instant now, int budget, Duration lockout) {
            int failures = failureCount + 1;
            Instant lockEnd = lockedUntil == null && failures >= budget
                ? now.plus(lockout)
                : lockedUntil;

            return new AttemptWindow(failures, firstFailureAt, lockEnd);
        }

        /**
         * Determines whether this window no longer applies: its lockout is over, or, below the
         * budget, its first failure is older than the lockout duration.
         *
         * @param now     current instant
         * @param lockout configured lockout duration
         * @return {@code true} when the window can be forgotten
         */
        boolean isExpired(Instant now, Duration lockout) {
            if (lockedUntil != null) {
                return !now.isBefore(lockedUntil);
            }
            return now.isAfter(firstFailureAt.plus(lockout));
        }
    }
}
