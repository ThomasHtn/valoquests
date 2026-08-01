package io.github.thomashtn.valorant.tracker.week.scheduler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valorant.tracker.synchronization.model.SynchronizationTrigger;
import io.github.thomashtn.valorant.tracker.synchronization.service.SynchronizationCommandService;
import io.github.thomashtn.valorant.tracker.week.service.WeeklyRolloverService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/**
 * Tests weekly rollover scheduler delegation and error isolation.
 */
class WeeklyRolloverSchedulerTest {

    /**
     * Verifies that the closing week is synchronized before it is finalized.
     *
     * <p>The last scheduled synchronization of the week ends hours before the rollover. Finalizing
     * first would freeze the week without the matches played in that gap, and no later run ever
     * revisits a finalized week.
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
                synchronizationService
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
     * <p>Skipping the rollover would leave the week open forever, since the next execution only
     * ever looks at the week that just ended.
     */
    @Test
    void shouldFinalizeWeekWhenPreRolloverSynchronizationFails() {
        WeeklyRolloverService rolloverService =
            mock(WeeklyRolloverService.class);

        SynchronizationCommandService synchronizationService =
            mock(SynchronizationCommandService.class);

        when(
            synchronizationService.synchronizeAllPlayers(
                SynchronizationTrigger.SCHEDULED
            )
        ).thenThrow(
            new IllegalStateException("Henrik unavailable")
        );

        WeeklyRolloverScheduler scheduler =
            new WeeklyRolloverScheduler(
                rolloverService,
                synchronizationService
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
                synchronizationService
            );

        assertThatCode(
            scheduler::rolloverWeek
        ).doesNotThrowAnyException();

        verify(rolloverService).rolloverIfNeeded();
    }
}
