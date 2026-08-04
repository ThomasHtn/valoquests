package io.github.thomashtn.valorant.tracker.week.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valorant.tracker.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valorant.tracker.challenge.repository.WeeklyChallengeRepository;
import io.github.thomashtn.valorant.tracker.challenge.service.ChallengeRecalculationService;
import io.github.thomashtn.valorant.tracker.ranking.entity.WeeklyPlayerScore;
import io.github.thomashtn.valorant.tracker.ranking.repository.WeeklyPlayerScoreRepository;
import io.github.thomashtn.valorant.tracker.ranking.service.RankingRecalculationService;
import io.github.thomashtn.valorant.tracker.week.WeekCalendar;
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
     * Finalizes the previous week and prepares the current one.
     *
     * <p>The method is idempotent. A previous week whose challenges are
     * already finalized is not recalculated or modified again.</p>
     */
    @Override
    @Transactional
    public void rolloverIfNeeded() {
        LocalDate currentWeekStart =
            weekCalendar.currentWeekStart();

        LocalDate previousWeekStart =
            currentWeekStart.minusWeeks(1);

        Instant rolloverTime = clock.instant();

        LOGGER.info(
            "Starting weekly rollover from week {} to week {}.",
            previousWeekStart,
            currentWeekStart
        );

        finalizePreviousWeekIfNeeded(
            previousWeekStart,
            rolloverTime
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
     * Finalizes the previous week when it contains an active challenge pack.
     *
     * @param previousWeekStart Monday identifying the previous week
     * @param finalizedAt       shared finalization timestamp
     */
    private void finalizePreviousWeekIfNeeded(
        LocalDate previousWeekStart,
        Instant finalizedAt
    ) {
        List<WeeklyChallenge> weeklyChallenges =
            weeklyChallengeRepository
                .findAllByWeekStartOrderByIdAsc(
                    previousWeekStart
                );

        if (weeklyChallenges.isEmpty()) {
            LOGGER.info(
                "No challenge pack exists for previous week {}. "
                    + "Nothing needs to be finalized.",
                previousWeekStart
            );

            return;
        }

        FinalizationState finalizationState =
            resolveFinalizationState(weeklyChallenges);

        if (finalizationState
            == FinalizationState.ALREADY_FINALIZED) {

            LOGGER.info(
                "Previous week {} is already finalized.",
                previousWeekStart
            );

            return;
        }

        if (finalizationState
            == FinalizationState.INCONSISTENT) {

            throw new IllegalStateException(
                "Weekly challenge pack for week "
                    + previousWeekStart
                    + " is only partially finalized"
            );
        }

        // Rebuilt before the ranking that freezes it: the last synchronization of the week runs
        // hours before this rollover, so matches played in that gap are only imported now. Without
        // this refresh they would land in a week that is already finalized and count for nothing.
        challengeRecalculationService.recalculateWeekProgress(
            previousWeekStart
        );

        rankingRecalculationService.recalculateWeek(
            previousWeekStart
        );

        weeklyLifecycleCoordinator.closeBossEncounterIfNeeded(previousWeekStart, finalizedAt);

        List<WeeklyPlayerScore> weeklyScores =
            weeklyPlayerScoreRepository
                .findAllByWeekStartOrderByPositionAsc(
                    previousWeekStart
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
            previousWeekStart,
            weeklyChallenges.size(),
            weeklyScores.size()
        );
    }

    /**
     * Determines the current finalization state of a weekly challenge pack.
     *
     * @param weeklyChallenges challenges belonging to one week
     * @return detected finalization state
     */
    private FinalizationState resolveFinalizationState(
        List<WeeklyChallenge> weeklyChallenges
    ) {
        long finalizedCount = weeklyChallenges.stream()
            .filter(
                challenge ->
                    challenge.getFinalizedAt() != null
            )
            .count();

        if (finalizedCount == 0) {
            return FinalizationState.NOT_FINALIZED;
        }

        if (finalizedCount == weeklyChallenges.size()) {
            return FinalizationState.ALREADY_FINALIZED;
        }

        return FinalizationState.INCONSISTENT;
    }

    /**
     * Describes the persisted finalization state of one weekly pack.
     */
    private enum FinalizationState {

        /**
         * No challenge has been finalized yet.
         */
        NOT_FINALIZED,

        /**
         * Every challenge has already been finalized.
         */
        ALREADY_FINALIZED,

        /**
         * Only part of the weekly pack is finalized.
         */
        INCONSISTENT
    }
}
