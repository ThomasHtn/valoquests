package io.github.thomashtn.valoquests.synchronization.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.shared.concurrency.MatchHistoryLock;
import io.github.thomashtn.valoquests.shared.exception.ConflictException;
import io.github.thomashtn.valoquests.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;

/**
 * Unit tests for {@link DefaultSynchronizationLaunchService}.
 */
@ExtendWith(MockitoExtension.class)
class DefaultSynchronizationLaunchServiceTest {

    /**
     * Mocked asynchronous runner, which never releases the lock here.
     */
    @Mock
    private AsyncSynchronizationRunner runner;

    /**
     * Mocked player repository.
     */
    @Mock
    private PlayerRepository playerRepository;

    /**
     * Real lock, so the guard is exercised rather than stubbed.
     */
    private MatchHistoryLock lock;

    /**
     * Service under test.
     */
    private SynchronizationLaunchService service;

    /**
     * Creates the service under test before each test.
     */
    @BeforeEach
    void setUp() {
        lock = new MatchHistoryLock();
        service = new DefaultSynchronizationLaunchService(runner, lock, playerRepository);
    }

    /**
     * Verifies that an idle application dispatches the batch run.
     */
    @Test
    @DisplayName("Dispatches a batch run when no guarded job is running")
    void shouldDispatchABatchRunWhenNothingIsInProgress() {
        service.launchAllPlayers();

        verify(runner).runAllPlayers();
    }

    /**
     * Verifies that two quick requests cannot both be accepted.
     *
     * <p>The lock is taken on the request thread, so the second request is refused even though the
     * first run has not started yet.
     */
    @Test
    @DisplayName("Refuses a second batch request while the first one still holds the lock")
    void shouldRefuseASecondRequestBeforeTheFirstRunStarted() {
        service.launchAllPlayers();

        assertThatThrownBy(() -> service.launchAllPlayers())
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("already running");

        verify(runner).runAllPlayers();
    }

    /**
     * Verifies that a request is refused while a scheduled job holds the lock.
     */
    @Test
    @DisplayName("Refuses a batch request while another guarded job runs")
    void shouldRefuseABatchRunWhileAnotherJobRuns() {
        lock.acquireOrReject();

        assertThatThrownBy(() -> service.launchAllPlayers())
            .isInstanceOf(ConflictException.class);

        verifyNoInteractions(runner);
    }

    /**
     * Verifies that a single-player run is dispatched for a known player.
     */
    @Test
    @DisplayName("Dispatches a single-player run for a known player")
    void shouldDispatchASinglePlayerRun() {
        when(playerRepository.existsById(3L)).thenReturn(true);

        service.launchPlayer(3L);

        verify(runner).runPlayer(3L);
    }

    /**
     * Verifies that an unknown player is reported now rather than as a failed execution later.
     */
    @Test
    @DisplayName("Rejects an unknown player before taking the lock")
    void shouldRejectAnUnknownPlayerBeforeAccepting() {
        when(playerRepository.existsById(404L)).thenReturn(false);

        assertThatThrownBy(() -> service.launchPlayer(404L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("404");

        verifyNoInteractions(runner);
        assertThat(lock.runIfFree(() -> { })).isTrue();
    }

    /**
     * Verifies that a concurrent single-player request is refused too.
     */
    @Test
    @DisplayName("Refuses a single-player request while another guarded job runs")
    void shouldRefuseASinglePlayerRunWhileAnotherJobRuns() {
        when(playerRepository.existsById(3L)).thenReturn(true);
        lock.acquireOrReject();

        assertThatThrownBy(() -> service.launchPlayer(3L))
            .isInstanceOf(ConflictException.class);

        verifyNoInteractions(runner);
    }

    /**
     * Verifies that a run the executor refused does not keep the lock forever.
     */
    @Test
    @DisplayName("Releases the lock when the executor rejects the run")
    void shouldReleaseTheLockWhenTheExecutorRejectsTheRun() {
        doThrow(new TaskRejectedException("queue full")).when(runner).runAllPlayers();

        assertThatThrownBy(() -> service.launchAllPlayers())
            .isInstanceOf(TaskRejectedException.class);

        assertThat(lock.runIfFree(() -> { })).isTrue();
    }
}
