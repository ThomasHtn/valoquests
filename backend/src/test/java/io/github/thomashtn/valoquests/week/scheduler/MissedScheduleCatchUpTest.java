package io.github.thomashtn.valoquests.week.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.campaign.scheduler.CampaignDailyTickScheduler;
import io.github.thomashtn.valoquests.challenge.repository.WeeklyChallengeRepository;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.scheduling.TaskScheduler;

/**
 * Verifies that a restart runs the jobs it missed, and only those that are due.
 */
@ExtendWith(MockitoExtension.class)
class MissedScheduleCatchUpTest {

    /**
     * Zone the week is split in.
     */
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    /**
     * Monday of the week in progress.
     */
    private static final LocalDate MONDAY = LocalDate.of(2026, 9, 21);

    @Mock
    private WeeklyRolloverScheduler rolloverScheduler;

    @Mock
    private CampaignDailyTickScheduler tickScheduler;

    @Mock
    private WeeklyChallengeRepository weeklyChallengeRepository;

    @Mock
    private TaskScheduler taskScheduler;

    @Test
    @DisplayName("Runs a missed rollover, then the day's tick, once this week's rollover time has passed")
    void shouldCatchUpAnOverdueRollover() {
        MissedScheduleCatchUp catchUp = catchUpAt("2026-09-23T10:00:00+02:00");
        when(weeklyChallengeRepository.findPendingWeekStartsBefore(MONDAY)).thenReturn(List.of(MONDAY.minusWeeks(1)));

        catchUp.catchUp();

        verify(rolloverScheduler).rolloverWeek();
        verify(tickScheduler).tick();
    }

    @Test
    @DisplayName("Leaves a Monday's rollover to its own firing when the application starts before it")
    void shouldWaitForTheScheduledRolloverOnMondayNight() {
        MissedScheduleCatchUp catchUp = catchUpAt("2026-09-21T01:00:00+02:00");
        when(weeklyChallengeRepository.findPendingWeekStartsBefore(MONDAY)).thenReturn(List.of(MONDAY.minusWeeks(1)));

        catchUp.catchUp();

        verify(rolloverScheduler, never()).rolloverWeek();
        verify(tickScheduler).tick();
    }

    @Test
    @DisplayName("Runs no rollover when every past week is already finalized")
    void shouldSkipTheRolloverWhenNothingIsPending() {
        MissedScheduleCatchUp catchUp = catchUpAt("2026-09-23T10:00:00+02:00");
        when(weeklyChallengeRepository.findPendingWeekStartsBefore(MONDAY)).thenReturn(List.of());

        catchUp.catchUp();

        verify(rolloverScheduler, never()).rolloverWeek();
        verify(tickScheduler).tick();
    }

    @Test
    @DisplayName("Hands the catch-up to the scheduler instead of running it on the startup thread")
    void shouldScheduleTheCatchUpAtStartup() {
        MissedScheduleCatchUp catchUp = catchUpAt("2026-09-23T10:00:00+02:00");

        catchUp.scheduleCatchUp();

        verify(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
        verify(tickScheduler, never()).tick();
    }

    /**
     * Builds the catch-up on a clock frozen at one instant.
     *
     * @param now instant the catch-up reads as now, with its offset
     * @return the catch-up
     */
    private MissedScheduleCatchUp catchUpAt(String now) {
        Clock clock = Clock.fixed(OffsetDateTime.parse(now).toInstant(), PARIS);

        return new MissedScheduleCatchUp(
            provider(WeeklyRolloverScheduler.class, rolloverScheduler),
            provider(CampaignDailyTickScheduler.class, tickScheduler),
            weeklyChallengeRepository,
            taskScheduler,
            new WeekCalendar(clock, PARIS),
            clock,
            "0 5 2 * * MON"
        );
    }

    /**
     * Wraps one bean in a provider.
     *
     * @param type bean type
     * @param bean bean to provide
     * @param <T>  bean type
     * @return the provider
     */
    private static <T> ObjectProvider<T> provider(Class<T> type, T bean) {
        StaticListableBeanFactory factory = new StaticListableBeanFactory();
        factory.addBean(type.getSimpleName(), bean);

        return factory.getBeanProvider(type);
    }
}
