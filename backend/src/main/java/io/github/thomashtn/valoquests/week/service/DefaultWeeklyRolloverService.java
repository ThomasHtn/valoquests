package io.github.thomashtn.valoquests.week.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valoquests.challenge.repository.WeeklyChallengeRepository;
import io.github.thomashtn.valoquests.challenge.service.ChallengeRecalculationService;
import io.github.thomashtn.valoquests.ranking.entity.WeeklyPlayerScore;
import io.github.thomashtn.valoquests.ranking.repository.WeeklyPlayerScoreRepository;
import io.github.thomashtn.valoquests.ranking.service.RankingRecalculationService;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Atomically finalizes the previous week and prepares the current week.
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
     * Repository used to load and finalize weekly challenges.
     */
    private final WeeklyChallengeRepository

        weeklyChallengeRepository;

    /**
     * Repository used to load and finalize weekly score snapshots.
     */
    private final WeeklyPlayerScoreRepository

        weeklyPlayerScoreRepository;

    /**
     * Service used to calculate the final previous-week ranking.
     */
    private final RankingRecalculationService

        rankingRecalculationService;

    /**
     * Coordinates opening a new week's challenge pack and boss encounter, and closing the previous
     * week's boss encounter.
     */
    private final WeeklyLifecycleCoordinator weeklyLifecycleCoordinator;

    /**
     * Service used to refresh the closing week's progress before it is frozen.
     */
    private final ChallengeRecalculationService

        challengeRecalculationService;

    /**
     * Application clock used for deterministic week calculations.
     */
    private final Clock clock;

    /**
     * Calendar resolving the current week.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the weekly rollover service.
     *
     * @param weeklyChallengeRepository     weekly challenge repository
     * @param weeklyPlayerScoreRepository   weekly score repository
     * @param rankingRecalculationService   ranking recalculation service
     * @param weeklyLifecycleCoordinator    coordinator opening a new week and closing a boss encounter
     * @param challengeRecalculationService challenge progress recalculation service
     * @param clock                         application clock
     * @param weekCalendar                  calendar resolving the current week
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public DefaultWeeklyRolloverService(
        WeeklyChallengeRepository weeklyChallengeRepository,
        WeeklyPlayerScoreRepository weeklyPlayerScoreRepository,
        RankingRecalculationService rankingRecalculationService,
        WeeklyLifecycleCoordinator weeklyLifecycleCoordinator,
        ChallengeRecalculationService challengeRecalculationService,
        Clock clock,
        WeekCalendar weekCalendar
    ) {
        this.weeklyChallengeRepository =
            weeklyChallengeRepository;

        this.weeklyPlayerScoreRepository =
            weeklyPlayerScoreRepository;

        this.rankingRecalculationService =
            rankingRecalculationService;

        this.weeklyLifecycleCoordinator = weeklyLifecycleCoordinator;

        this.challengeRecalculationService =
            challengeRecalculationService;

        this.clock = clock;
        this.weekCalendar = weekCalendar;
    }

    /**
     * Finalizes every past week still open and prepares the current one.
     *
     * <p>Catches up rather than only handling last week: a rollover that never ran — the
     * application was down that Monday, or the job failed — used to leave its week open forever,
     * since the next run only ever looked at the week that had just ended. Every past week holding
     * an active pack is therefore finalized here, oldest first.</p>
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
            weeklyChallengeRepository
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
                finalizeWeek(weekStart, rolloverTime)
        );

        weeklyLifecycleCoordinator.openWeek(
            currentWeekStart
        );

        LOGGER.info(
            "Weekly rollover completed. Current week is {}.",
            currentWeekStart
        );
    }

    /**
     * Finalizes one past week whose challenge pack is still active.
     *
     * @param weekStart   Monday identifying the week to finalize
     * @param finalizedAt shared finalization timestamp
     */
    private void finalizeWeek(
        LocalDate weekStart,
        Instant finalizedAt
    ) {
        List<WeeklyChallenge> weeklyChallenges =
            weeklyChallengeRepository
                .findAllByWeekStartOrderByIdAsc(
                    weekStart
                );

        rejectPartiallyFinalizedPack(
            weekStart,
            weeklyChallenges
        );

        // Rebuilt before the ranking that freezes it: the last synchronization of the week runs
        // hours before this rollover, so matches played in that gap are only imported now. Without
        // this refresh they would land in a week that is already finalized and count for nothing.
        challengeRecalculationService.recalculateWeekProgress(
            weekStart
        );

        rankingRecalculationService.recalculateWeek(
            weekStart
        );

        weeklyLifecycleCoordinator.closeBossEncounterIfNeeded(weekStart, finalizedAt);

        List<WeeklyPlayerScore> weeklyScores =
            weeklyPlayerScoreRepository
                .findAllByWeekStartOrderByPositionAsc(
                    weekStart
                );

        weeklyChallenges.forEach(
            challenge ->
                challenge.setFinalizedAt(finalizedAt)
        );

        weeklyScores.forEach(
            score ->
                score.setFinalizedAt(finalizedAt)
        );

        weeklyChallengeRepository.saveAll(
            weeklyChallenges
        );

        weeklyPlayerScoreRepository.saveAll(
            weeklyScores
        );

        LOGGER.info(
            "Week {} finalized with {} challenge(s) and {} score(s).",
            weekStart,
            weeklyChallenges.size(),
            weeklyScores.size()
        );
    }

    /**
     * Refuses to finalize a pack that is only partly frozen.
     *
     * <p>A pending week owns at least one active challenge by construction, so a single finalized
     * one is enough to prove the pack was left half-frozen. Repairing it silently would freeze the
     * remainder against a ranking the finalized half never saw.</p>
     *
     * @param weekStart        Monday identifying the week being finalized
     * @param weeklyChallenges challenges belonging to that week
     */
    private void rejectPartiallyFinalizedPack(
        LocalDate weekStart,
        List<WeeklyChallenge> weeklyChallenges
    ) {
        boolean partiallyFinalized = weeklyChallenges.stream()
            .anyMatch(
                challenge ->
                    challenge.getFinalizedAt() != null
            );

        if (partiallyFinalized) {
            throw new IllegalStateException(
                "Weekly challenge pack for week "
                    + weekStart
                    + " is only partially finalized"
            );
        }
    }
}
