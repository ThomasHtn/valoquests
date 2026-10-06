package io.github.thomashtn.valoquests.synchronization.scheduler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.thomashtn.valoquests.shared.concurrency.MatchHistoryLock;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationTrigger;
import io.github.thomashtn.valoquests.synchronization.service.SynchronizationCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link StandardSynchronizationScheduler}.
 */
@ExtendWith(MockitoExtension.class)
class StandardSynchronizationSchedulerTest {

    /**
     * Synchronization command service invoked by the scheduler.
     */
    @Mock
    private SynchronizationCommandService synchronizationCommandService;

    /**
     * Verifies that automatic executions are explicitly recorded as scheduled.
     */
    @Test
    void shouldRunScheduledStandardSynchronization() {
        StandardSynchronizationScheduler scheduler =
            new StandardSynchronizationScheduler(
                synchronizationCommandService,
                new MatchHistoryLock()
            );

        scheduler.synchronizeAllActivePlayers();

        verify(synchronizationCommandService).synchronizeAllPlayers(
            SynchronizationTrigger.SCHEDULED
        );
    }

    /**
     * Verifies that an unexpected failure does not escape the scheduled method.
     */
    @Test
    void shouldKeepSchedulerAliveWhenSynchronizationFails() {
        StandardSynchronizationScheduler scheduler =
            new StandardSynchronizationScheduler(
                synchronizationCommandService,
                new MatchHistoryLock()
            );

        doThrow(new IllegalStateException("Unexpected failure"))
            .when(synchronizationCommandService)
            .synchronizeAllPlayers(SynchronizationTrigger.SCHEDULED);

        assertThatCode(scheduler::synchronizeAllActivePlayers)
            .doesNotThrowAnyException();
    }

    /**
     * Verifies that a scheduled run does not overlap a job already holding the lock.
     */
    @Test
    @DisplayName("Skips the scheduled run while another guarded job runs")
    void shouldSkipWhileAnotherGuardedJobRuns() {
        MatchHistoryLock lock = new MatchHistoryLock();
        lock.acquireOrReject();

        new StandardSynchronizationScheduler(synchronizationCommandService, lock)
            .synchronizeAllActivePlayers();

        verifyNoInteractions(synchronizationCommandService);
    }
}
