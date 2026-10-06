package io.github.thomashtn.valoquests.synchronization.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import io.github.thomashtn.valoquests.shared.concurrency.MatchHistoryLock;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationTrigger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link AsyncSynchronizationRunner}.
 */
@ExtendWith(MockitoExtension.class)
class AsyncSynchronizationRunnerTest {

    /**
     * Mocked synchronization command service.
     */
    @Mock
    private SynchronizationCommandService commandService;

    /**
     * Lock the caller took before dispatching, released by the runner.
     */
    private MatchHistoryLock lock;

    /**
     * Runner under test.
     */
    private AsyncSynchronizationRunner runner;

    /**
     * Creates the runner under test before each test.
     */
    @BeforeEach
    void setUp() {
        lock = new MatchHistoryLock();
        lock.acquireOrReject();
        runner = new AsyncSynchronizationRunner(commandService, lock);
    }

    /**
     * Verifies that a background batch run is recorded as manually triggered.
     */
    @Test
    void shouldRunABatchSynchronizationWithTheManualTrigger() {
        runner.runAllPlayers();

        verify(commandService).synchronizeAllPlayers(SynchronizationTrigger.MANUAL);
    }

    /**
     * Verifies that a background batch failure never escapes the runner.
     *
     * <p>Nothing is waiting on this thread: an exception thrown here would only be reported by the
     * executor's default handler, while the failed execution row already carries the diagnosis.
     */
    @Test
    void shouldSwallowABatchFailure() {
        doThrow(new IllegalStateException("Henrik unreachable"))
            .when(commandService).synchronizeAllPlayers(SynchronizationTrigger.MANUAL);

        assertThatCode(() -> runner.runAllPlayers()).doesNotThrowAnyException();
    }

    /**
     * Verifies that a background single-player run reaches the command service.
     */
    @Test
    void shouldRunASinglePlayerSynchronization() {
        runner.runPlayer(3L);

        verify(commandService).synchronizePlayer(3L);
    }

    /**
     * Verifies that a background single-player failure never escapes the runner.
     */
    @Test
    void shouldSwallowASinglePlayerFailure() {
        doThrow(new IllegalStateException("Henrik unreachable"))
            .when(commandService).synchronizePlayer(3L);

        assertThatCode(() -> runner.runPlayer(3L)).doesNotThrowAnyException();
    }

    /**
     * Verifies that the lock taken by the caller is given back even when the run fails.
     */
    @Test
    @DisplayName("Releases the match history lock once a failed background run ends")
    void shouldReleaseTheLockAfterAFailedRun() {
        doThrow(new IllegalStateException("Henrik unreachable"))
            .when(commandService).synchronizePlayer(3L);

        runner.runPlayer(3L);

        assertThat(lock.runIfFree(() -> { })).isTrue();
    }

    /**
     * Verifies that a successful batch run gives the lock back too.
     */
    @Test
    @DisplayName("Releases the match history lock once a background batch run ends")
    void shouldReleaseTheLockAfterABatchRun() {
        runner.runAllPlayers();

        assertThat(lock.runIfFree(() -> { })).isTrue();
    }
}
