package io.github.thomashtn.valoquests.week.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.thomashtn.valoquests.shared.concurrency.MatchHistoryLock;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationTrigger;
import io.github.thomashtn.valoquests.synchronization.service.SynchronizationCommandService;
import io.github.thomashtn.valoquests.week.service.WeeklyRolloverService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/**
 * Tests weekly rollover scheduler delegation and error isolation.
 */
class WeeklyRolloverSchedulerTest {

    /**
     * Verifies that the closing week is synchronized before it is finalized.
     *
     * <p>Otherwise matches played after the last scheduled sync are lost, since a finalized week is never revisited.
     */
    @Test
    void shouldSynchronizeBeforeFinalizingTheWeek() {
        WeeklyRolloverService rolloverService =
            mock(WeeklyRolloverService.class);

        SynchronizationCommandService synchronizationService =
            mock(SynchronizationCommandService.class);

        WeeklyRolloverScheduler scheduler =
            new WeeklyRolloverScheduler(
                rolloverService,
                synchronizationService,
                new MatchHistoryLock()
            );

        scheduler.rolloverWeek();

        InOrder rolloverOrder = inOrder(
            synchronizationService,
            rolloverService
        );

        rolloverOrder.verify(synchronizationService)
            .synchronizeAllPlayers(SynchronizationTrigger.SCHEDULED);

        rolloverOrder.verify(rolloverService).rolloverIfNeeded();
    }

    /**
     * Verifies that a failed pre-rollover synchronization still finalizes the week.
     *
     * <p>Skipping it would leave the week open forever, since the next run only looks at the week just ended.
     */
    @Test
    void shouldFinalizeWeekWhenPreRolloverSynchronizationFails() {
        WeeklyRolloverService rolloverService =
            mock(WeeklyRolloverService.class);

        SynchronizationCommandService synchronizationService =
            mock(SynchronizationCommandService.class);

        doThrow(new IllegalStateException("Henrik unavailable"))
            .when(synchronizationService)
            .synchronizeAllPlayers(SynchronizationTrigger.SCHEDULED);

        WeeklyRolloverScheduler scheduler =
            new WeeklyRolloverScheduler(
                rolloverService,
                synchronizationService,
                new MatchHistoryLock()
            );

        assertThatCode(
            scheduler::rolloverWeek
        ).doesNotThrowAnyException();

        verify(rolloverService).rolloverIfNeeded();
    }

    /**
     * Verifies that an unexpected rollover failure does not escape from the
     * scheduler method.
     */
    @Test
    void shouldContainUnexpectedRolloverFailure() {
        WeeklyRolloverService rolloverService =
            mock(WeeklyRolloverService.class);

        SynchronizationCommandService synchronizationService =
            mock(SynchronizationCommandService.class);

        doThrow(
            new IllegalStateException(
                "Database unavailable"
            )
        )
            .when(rolloverService)
            .rolloverIfNeeded();

        WeeklyRolloverScheduler scheduler =
            new WeeklyRolloverScheduler(
                rolloverService,
                synchronizationService,
                new MatchHistoryLock()
            );

        assertThatCode(
            scheduler::rolloverWeek
        ).doesNotThrowAnyException();

        verify(rolloverService).rolloverIfNeeded();
    }

    /**
     * Verifies that the rollover waits for a running job instead of skipping the week.
     */
    @Test
    @DisplayName("Waits for another guarded job to finish, then synchronizes and rolls the week over")
    void shouldWaitForAnotherGuardedJob() throws InterruptedException {
        WeeklyRolloverService rolloverService =
            mock(WeeklyRolloverService.class);

        SynchronizationCommandService synchronizationService =
            mock(SynchronizationCommandService.class);

        MatchHistoryLock lock = new MatchHistoryLock();
        lock.acquireOrReject();
        Thread runningJob = new Thread(() -> {
            sleepQuietly(100);
            lock.release();
        });
        runningJob.start();

        new WeeklyRolloverScheduler(rolloverService, synchronizationService, lock).rolloverWeek();
        runningJob.join();

        verify(synchronizationService).synchronizeAllPlayers(SynchronizationTrigger.SCHEDULED);
        verify(rolloverService).rolloverIfNeeded();
    }

    /**
     * Verifies that an interrupted wait gives up without running anything.
     */
    @Test
    @DisplayName("Gives up without running anything and keeps the interrupt flag when interrupted")
    void shouldGiveUpWhenInterruptedWhileWaiting() {
        WeeklyRolloverService rolloverService =
            mock(WeeklyRolloverService.class);

        SynchronizationCommandService synchronizationService =
            mock(SynchronizationCommandService.class);

        MatchHistoryLock lock = new MatchHistoryLock();
        lock.acquireOrReject();

        Thread.currentThread().interrupt();
        try {
            new WeeklyRolloverScheduler(rolloverService, synchronizationService, lock).rolloverWeek();
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }

        verifyNoInteractions(rolloverService, synchronizationService);
    }

    /**
     * Sleeps without propagating an interruption.
     *
     * @param millis sleep duration
     */
    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
