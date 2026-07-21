package io.github.thomashtn.valorant.tracker.week.scheduler;

import io.github.thomashtn.valorant.tracker.week.service.WeeklyRolloverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Automatically finalizes the previous week and prepares the new one.
 */
@Component
@ConditionalOnProperty(
    name = "app.scheduling.week-rollover-enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class WeeklyRolloverScheduler {

    /**
     * Application logger.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(
            WeeklyRolloverScheduler.class
        );

    /**
     * Service executing the transactional weekly rollover.
     */
    private final WeeklyRolloverService
        weeklyRolloverService;

    /**
     * Creates the weekly rollover scheduler.
     *
     * @param weeklyRolloverService weekly rollover service
     */
    public WeeklyRolloverScheduler(
        WeeklyRolloverService weeklyRolloverService
    ) {
        this.weeklyRolloverService =
            weeklyRolloverService;
    }

    /**
     * Executes the rollover every Monday shortly after midnight UTC.
     *
     * <p>Errors are logged and allowed to be retried during the next
     * execution. The transactional service prevents partial finalization.</p>
     */
    @Scheduled(
        cron = "${app.scheduling.week-rollover-cron}",
        zone = "${app.scheduling.week-rollover-zone}"
    )
    public void rolloverWeek() {
        LOGGER.info("Scheduled weekly rollover started");

        try {
            weeklyRolloverService.rolloverIfNeeded();
            LOGGER.info("Scheduled weekly rollover completed");
        } catch (RuntimeException exception) {
            LOGGER.error(
                "Scheduled weekly rollover failed unexpectedly",
                exception
            );
        }
    }
}
