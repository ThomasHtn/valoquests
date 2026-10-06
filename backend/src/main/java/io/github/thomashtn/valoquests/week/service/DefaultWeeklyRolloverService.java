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
 * <p>Weeks are identified by their Monday. Everything runs in one transaction, so a failure opening the
 * new week also rolls back the finalization.
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
     * <p>Every pending past week is finalized, oldest first, so a missed Monday never leaves a week open.
     * Idempotent: a finalized week is no longer pending.
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

        // Weeks are independent; chronological order only keeps the log readable.
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
