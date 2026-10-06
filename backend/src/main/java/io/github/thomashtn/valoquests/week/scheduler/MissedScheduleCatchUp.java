package io.github.thomashtn.valoquests.week.scheduler;

import io.github.thomashtn.valoquests.campaign.scheduler.CampaignDailyTickScheduler;
import io.github.thomashtn.valoquests.challenge.repository.ChallengeSelectionRepository;
import io.github.thomashtn.valoquests.shared.time.WeekCalendar;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

/**
 * Catches up, at startup, on the scheduled jobs a stopped application missed.
 *
 * <p>Runs the overdue rollover then the day's tick, both idempotent, on the scheduler's thread so the
 * rollover's synchronization neither delays startup nor overlaps a cron job.
 */
@Component
public class MissedScheduleCatchUp {

    /**
     * Application logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MissedScheduleCatchUp.class);

    /**
     * Rollover job, absent when the rollover is disabled.
     */
    private final ObjectProvider<WeeklyRolloverScheduler> rolloverScheduler;

    /**
     * Daily tick job, absent when the tick is disabled.
     */
    private final ObjectProvider<CampaignDailyTickScheduler> tickScheduler;

    /**
     * Repository telling which past weeks still await finalization.
     */
    private final ChallengeSelectionRepository challengeSelectionRepository;

    /**
     * Scheduler the catch-up is handed to.
     */
    private final TaskScheduler taskScheduler;

    /**
     * Calendar resolving the current week in the rollover's zone.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Application clock.
     */
    private final Clock clock;

    /**
     * When the weekly rollover fires.
     */
    private final CronExpression rolloverCron;

    /**
     * Creates the catch-up.
     *
     * @param rolloverScheduler         rollover job, when enabled
     * @param tickScheduler             daily tick job, when enabled
     * @param challengeSelectionRepository challenge selection repository
     * @param taskScheduler             scheduler running the catch-up
     * @param weekCalendar              week calendar
     * @param clock                     application clock
     * @param rolloverCron              cron expression of the weekly rollover
     */
    public MissedScheduleCatchUp(
        ObjectProvider<WeeklyRolloverScheduler> rolloverScheduler,
        ObjectProvider<CampaignDailyTickScheduler> tickScheduler,
        ChallengeSelectionRepository challengeSelectionRepository,
        TaskScheduler taskScheduler,
        WeekCalendar weekCalendar,
        Clock clock,
        @Value("${app.scheduling.week-rollover-cron}") String rolloverCron
    ) {
        this.rolloverScheduler = rolloverScheduler;
        this.tickScheduler = tickScheduler;
        this.challengeSelectionRepository = challengeSelectionRepository;
        this.taskScheduler = taskScheduler;
        this.weekCalendar = weekCalendar;
        this.clock = clock;
        this.rolloverCron = CronExpression.parse(rolloverCron);
    }

    /**
     * Hands the catch-up to the scheduler once the application is ready.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void scheduleCatchUp() {
        // Spring Boot's scheduler has a single thread: that is what keeps this from overlapping a cron job.
        taskScheduler.schedule(this::catchUp, clock.instant());
    }

    /**
     * Runs the overdue rollover, if any, then the day's tick.
     */
    public void catchUp() {
        WeeklyRolloverScheduler rollover = rolloverScheduler.getIfAvailable();

        if (rollover != null && isRolloverOverdue()) {
            LOGGER.info("A weekly rollover was missed while the application was down: catching up.");
            rollover.rolloverWeek();
        }

        tickScheduler.ifAvailable(CampaignDailyTickScheduler::tick);
    }

    /**
     * Tells whether a past week awaits finalization although this week's rollover time has passed.
     *
     * <p>Before that time on a Monday, the scheduled rollover keeps its margin for Sunday's last matches.
     *
     * @return {@code true} when the rollover should have run already
     */
    private boolean isRolloverOverdue() {
        LocalDate currentWeekStart = weekCalendar.currentWeekStart();

        if (challengeSelectionRepository.findPendingWeekStartsBefore(currentWeekStart).isEmpty()) {
            return false;
        }

        ZonedDateTime weekOpening = currentWeekStart.atStartOfDay(weekCalendar.zone());
        ZonedDateTime firstFiring = rolloverCron.next(weekOpening.minusSeconds(1));

        return firstFiring != null && !ZonedDateTime.now(clock).isBefore(firstFiring);
    }
}
