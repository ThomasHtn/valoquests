package io.github.thomashtn.valoquests.shared.concurrency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.thomashtn.valoquests.shared.exception.ConflictException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link MatchHistoryLock}.
 */
class MatchHistoryLockTest {

    /**
     * Lock under test.
     */
    private final MatchHistoryLock lock = new MatchHistoryLock();

    @Test
    @DisplayName("Runs a job when no other guarded job holds the lock")
    void shouldRunAJobWhenFree() {
        AtomicBoolean ran = new AtomicBoolean();

        assertThat(lock.runIfFree(() -> ran.set(true))).isTrue();
        assertThat(ran).isTrue();
    }

    @Test
    @DisplayName("Skips a job while another one holds the lock, even from the same thread")
    void shouldSkipAJobWhileTheLockIsTaken() {
        AtomicBoolean nestedRan = new AtomicBoolean();

        lock.runIfFree(() -> lock.runIfFree(() -> nestedRan.set(true)));

        assertThat(nestedRan).isFalse();
    }

    @Test
    @DisplayName("Refuses a request with a conflict while another job holds the lock")
    void shouldRejectWhileTheLockIsTaken() {
        lock.acquireOrReject();

        assertThatThrownBy(() -> lock.runOrReject(() -> { }))
            .isInstanceOf(ConflictException.class);
        assertThatThrownBy(lock::acquireOrReject)
            .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Returns a job's result when free and refuses it while another job holds the lock")
    void shouldSupplyAResultOrReject() {
        assertThat(lock.supplyOrReject(() -> "done")).isEqualTo("done");
        assertThat(lock.runIfFree(() -> { })).isTrue();

        lock.acquireOrReject();
        assertThatThrownBy(() -> lock.supplyOrReject(() -> "too late"))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Frees the lock when a job fails, so the next one can run")
    void shouldReleaseTheLockWhenAJobFails() {
        assertThatThrownBy(() -> lock.runOrReject(() -> {
            throw new IllegalStateException("job failed");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(lock.runIfFree(() -> { })).isTrue();
    }

    @Test
    @DisplayName("Frees a lock taken on one thread once released by another")
    void shouldBeReleasableFromAnotherThread() throws InterruptedException {
        lock.acquireOrReject();

        Thread worker = new Thread(lock::release);
        worker.start();
        worker.join();

        assertThat(lock.runIfFree(() -> { })).isTrue();
    }

    @Test
    @DisplayName("Runs a waiting job once the running one releases the lock")
    void shouldRunAWaitingJobOnceTheLockIsReleased() throws InterruptedException {
        AtomicBoolean ran = new AtomicBoolean();
        lock.acquireOrReject();
        Thread runningJob = new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            lock.release();
        });
        runningJob.start();

        assertThat(lock.runWhenFree(() -> ran.set(true), Duration.ofSeconds(10))).isTrue();
        runningJob.join();

        assertThat(ran).isTrue();
        assertThat(lock.runIfFree(() -> { })).isTrue();
    }

    @Test
    @DisplayName("Gives up on a waiting job when the lock stays taken past the wait")
    void shouldGiveUpWhenTheWaitExpires() throws InterruptedException {
        AtomicBoolean ran = new AtomicBoolean();
        lock.acquireOrReject();

        assertThat(lock.runWhenFree(() -> ran.set(true), Duration.ofMillis(20))).isFalse();
        assertThat(ran).isFalse();
    }
}
