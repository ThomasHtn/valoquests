package io.github.thomashtn.valoquests.week.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.challenge.entity.ChallengeSelection;
import io.github.thomashtn.valoquests.challenge.repository.ChallengeSelectionRepository;
import io.github.thomashtn.valoquests.challenge.service.ChallengeRecalculationService;
import io.github.thomashtn.valoquests.ranking.entity.WeeklyPlayerScore;
import io.github.thomashtn.valoquests.ranking.repository.WeeklyPlayerScoreRepository;
import io.github.thomashtn.valoquests.ranking.service.RankingRecalculationService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Freezes one past week: rebuilds its progress and ranking, then marks its pack and scores final.
 *
 * <p>Runs inside the rollover's transaction, so a failure later in the rollover undoes it too.
 */
@Component
public class WeekFinalizer {

    /**
     * Application logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(WeekFinalizer.class);

    /**
     * Repository used to load and finalize the week's challenge selections.
     */
    private final ChallengeSelectionRepository challengeSelectionRepository;

    /**
     * Repository used to load and finalize the week's score snapshots.
     */
    private final WeeklyPlayerScoreRepository weeklyPlayerScoreRepository;

    /**
     * Service refreshing the week's progress before it is frozen.
     */
    private final ChallengeRecalculationService challengeRecalculationService;

    /**
     * Service computing the week's final ranking.
     */
    private final RankingRecalculationService rankingRecalculationService;

    /**
     * Creates the week finalizer.
     *
     * @param challengeSelectionRepository  challenge selection repository
     * @param weeklyPlayerScoreRepository   weekly score repository
     * @param challengeRecalculationService challenge progress recalculation service
     * @param rankingRecalculationService   ranking recalculation service
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public WeekFinalizer(
        ChallengeSelectionRepository challengeSelectionRepository,
        WeeklyPlayerScoreRepository weeklyPlayerScoreRepository,
        ChallengeRecalculationService challengeRecalculationService,
        RankingRecalculationService rankingRecalculationService
    ) {
        this.challengeSelectionRepository = challengeSelectionRepository;
        this.weeklyPlayerScoreRepository = weeklyPlayerScoreRepository;
        this.challengeRecalculationService = challengeRecalculationService;
        this.rankingRecalculationService = rankingRecalculationService;
    }

    /**
     * Finalizes one past week whose challenge pack is still active.
     *
     * @param weekStart   Monday identifying the week to finalize
     * @param finalizedAt shared finalization timestamp
     */
    public void finalizeWeek(
        LocalDate weekStart,
        Instant finalizedAt
    ) {
        List<ChallengeSelection> selections =
            challengeSelectionRepository
                .findAllByWeekStartOrderByIdAsc(
                    weekStart
                );

        rejectPartiallyFinalizedPack(
            weekStart,
            selections
        );

        // Rebuilt before the frozen ranking so the matches imported by this rollover still count.
        challengeRecalculationService.recalculateWeekProgress(weekStart);
        rankingRecalculationService.recalculateWeek(weekStart);

        List<WeeklyPlayerScore> weeklyScores =
            weeklyPlayerScoreRepository
                .findAllByWeekStartOrderByPositionAscPlayerIdAsc(
                    weekStart
                );

        selections.forEach(
            challenge ->
                challenge.setFinalizedAt(finalizedAt)
        );

        weeklyScores.forEach(
            score ->
                score.setFinalizedAt(finalizedAt)
        );

        // Both lists are managed by the rollover's transaction: the finalization flushes with it.
        LOGGER.info(
            "Week {} finalized with {} challenge(s) and {} score(s).",
            weekStart,
            selections.size(),
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
     * @param selections challenges belonging to that week
     */
    private void rejectPartiallyFinalizedPack(
        LocalDate weekStart,
        List<ChallengeSelection> selections
    ) {
        boolean partiallyFinalized = selections.stream()
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
