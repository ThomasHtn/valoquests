package io.github.thomashtn.valorant.tracker.week.scheduler;

import io.github.thomashtn.valorant.tracker.week.service.WeeklyRolloverService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests weekly rollover scheduler delegation and error isolation.
 */
class WeeklyRolloverSchedulerTest {

    /**
     * Verifies that a scheduled execution delegates to the rollover service.
     */
    @Test
    void shouldDelegateScheduledRollover() {
        WeeklyRolloverService rolloverService =
            mock(WeeklyRolloverService.class);

        WeeklyRolloverScheduler scheduler =
            new WeeklyRolloverScheduler(
                rolloverService
            );

        scheduler.rolloverWeek();

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

        doThrow(
            new IllegalStateException(
                "Database unavailable"
            )
        )
            .when(rolloverService)
            .rolloverIfNeeded();

        WeeklyRolloverScheduler scheduler =
            new WeeklyRolloverScheduler(
                rolloverService
            );

        assertThatCode(
            scheduler::rolloverWeek
        ).doesNotThrowAnyException();

        verify(rolloverService).rolloverIfNeeded();
    }
}
