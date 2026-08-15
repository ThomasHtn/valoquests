package io.github.thomashtn.valorant.tracker.synchronization.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valorant.tracker.synchronization.model.SynchronizationTrigger;
import org.junit.jupiter.api.BeforeEach;
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
     * Runner under test.
     */
    private AsyncSynchronizationRunner runner;

    /**
     * Creates the runner under test before each test.
     */
    @BeforeEach
    void setUp() {
        runner = new AsyncSynchronizationRunner(commandService);
    }

    /**
     * Verifies that a background batch run is recorded as manually triggered.
     */
    @Test
    void shouldRunABatchSynchronizationWithTheManualTrigger() {
        when(commandService.synchronizeAllPlayers(SynchronizationTrigger.MANUAL))
            .thenReturn(null);

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
        when(commandService.synchronizeAllPlayers(SynchronizationTrigger.MANUAL))
            .thenThrow(new IllegalStateException("Henrik unreachable"));

        assertThatCode(() -> runner.runAllPlayers()).doesNotThrowAnyException();
    }

    /**
     * Verifies that a background single-player run reaches the command service.
     */
    @Test
    void shouldRunASinglePlayerSynchronization() {
        when(commandService.synchronizePlayer(3L)).thenReturn(null);

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
}
