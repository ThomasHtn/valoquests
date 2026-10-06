package io.github.thomashtn.valoquests.week.service;

import io.github.thomashtn.valoquests.challenge.repository.ChallengeSelectionRepository;
import io.github.thomashtn.valoquests.shared.time.WeekCalendar;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Atomically finalizes the past weeks still open and opens the current week.
 *
 * <p>The service uses the Monday stored in weekly tables as the week
 * identifier. No dedicated week table is required.</p>
 *
 * <p>The whole rollover runs inside one database transaction. Consequently,
 * failure while creating the new challenge pack also rolls back the
 * finalization of the previous week.</p>
 */
@Service
public class DefaultWeeklyRolloverService
    implements WeeklyRolloverService {

    /**
     * Application logger.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(
            DefaultWeeklyRolloverService.class
        );

    /**
     * Repository telling which past weeks still await finalization.
     */
    private final ChallengeSelectionRepository challengeSelectionRepository;

    /**
     * Freezes each past week still open.
     */
    private final WeekFinalizer weekFinalizer;

    /**
     * Opens the current week.
     */
    private final WeekOpener weekOpener;

    /**
     * Application clock stamping the finalization.
     */
    private final Clock clock;

    /**
     * Calendar resolving the current week.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the weekly rollover service.
     *
     * @param challengeSelectionRepository challenge selection repository
     * @param weekFinalizer                finalizer of the past weeks
     * @param weekOpener                   opener of the current week
     * @param clock                        application clock
     * @param weekCalendar                 calendar resolving the current week
     */
    public DefaultWeeklyRolloverService(
        ChallengeSelectionRepository challengeSelectionRepository,
        WeekFinalizer weekFinalizer,
        WeekOpener weekOpener,
        Clock clock,
        WeekCalendar weekCalendar
    ) {
        this.challengeSelectionRepository = challengeSelectionRepository;
        this.weekFinalizer = weekFinalizer;
        this.weekOpener = weekOpener;
        this.clock = clock;
        this.weekCalendar = weekCalendar;
    }

    /**
     * Finalizes every past week still open and prepares the current one.
     *
     * <p>Catches up rather than only handling last week: every past week still holding an active
     * pack is finalized, oldest first, so a Monday the application was down or the job failed never
     * leaves its week open.</p>
     *
     * <p>The method is idempotent. A week whose challenges are already finalized is no longer
     * pending, so it is neither recalculated nor modified again.</p>
     */
    @Override
    @Transactional
    public void rolloverIfNeeded() {
        LocalDate currentWeekStart =
            weekCalendar.currentWeekStart();

        Instant rolloverTime = clock.instant();

        List<LocalDate> pendingWeekStarts =
            challengeSelectionRepository
                .findPendingWeekStartsBefore(
                    currentWeekStart
                );

        LOGGER.info(
            "Starting weekly rollover to week {}. {} past week(s) awaiting finalization: {}.",
            currentWeekStart,
            pendingWeekStarts.size(),
            pendingWeekStarts
        );

        // Weeks are independent — each is rebuilt from its own matches — but finalizing them in
        // chronological order is what keeps the log readable when several are caught up at once.
        pendingWeekStarts.forEach(
            weekStart ->
                weekFinalizer.finalizeWeek(weekStart, rolloverTime)
        );

        weekOpener.openWeek(
            currentWeekStart
        );

        LOGGER.info(
            "Weekly rollover completed. Current week is {}.",
            currentWeekStart
        );
    }
}
