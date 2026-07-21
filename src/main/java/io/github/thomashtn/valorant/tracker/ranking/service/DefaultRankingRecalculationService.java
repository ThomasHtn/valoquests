package io.github.thomashtn.valorant.tracker.ranking.service;

import io.github.thomashtn.valorant.tracker.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valorant.tracker.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valorant.tracker.player.entity.Player;
import io.github.thomashtn.valorant.tracker.player.model.PlayerStatus;
import io.github.thomashtn.valorant.tracker.player.repository.PlayerRepository;
import io.github.thomashtn.valorant.tracker.ranking.entity.WeeklyPlayerScore;
import io.github.thomashtn.valorant.tracker.ranking.repository.WeeklyPlayerScoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Rebuilds the current weekly ranking from persisted challenge progress.
 */
@Service
public class DefaultRankingRecalculationService
    implements RankingRecalculationService {

    /**
     * Application logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(
        DefaultRankingRecalculationService.class
    );

    /**
     * Repository used to retrieve active players.
     */
    private final PlayerRepository playerRepository;

    /**
     * Repository used to retrieve calculated challenge progress.
     */
    private final PlayerChallengeProgressRepository progressRepository;

    /**
     * Repository used to persist weekly score snapshots.
     */
    private final WeeklyPlayerScoreRepository scoreRepository;

    /**
     * Application clock used for deterministic week and timestamp handling.
     */
    private final Clock clock;

    /**
     * Creates the ranking recalculation service.
     */
    public DefaultRankingRecalculationService(
        PlayerRepository playerRepository,
        PlayerChallengeProgressRepository progressRepository,
        WeeklyPlayerScoreRepository scoreRepository,
        Clock clock
    ) {
        this.playerRepository = playerRepository;
        this.progressRepository = progressRepository;
        this.scoreRepository = scoreRepository;
        this.clock = clock;
    }

    /**
     * Recalculates scores and deterministic positions for the current UTC week.
     */
    @Override
    @Transactional
    public void recalculateCurrentRanking() {
        LocalDate weekStart = resolveCurrentWeekStart();
        Instant calculatedAt = clock.instant();
        List<Player> activePlayers = playerRepository
            .findAllByStatusOrderByIdAsc(PlayerStatus.ACTIVE);

        if (activePlayers.isEmpty()) {
            scoreRepository.deleteAllByWeekStart(weekStart);
            LOGGER.info("Ranking cleared for week {} because no player is active.", weekStart);
            return;
        }

        List<Long> activePlayerIds = activePlayers.stream()
            .map(Player::getId)
            .toList();
        scoreRepository.deleteAllByWeekStartAndPlayerIdNotIn(
            weekStart,
            activePlayerIds
        );

        Map<Long, WeeklyPlayerScore> existingByPlayerId = scoreRepository
            .findAllByWeekStartOrderByPositionAsc(weekStart)
            .stream()
            .collect(Collectors.toMap(
                score -> score.getPlayer().getId(),
                Function.identity()
            ));

        Map<Long, RankingAggregate> aggregates = aggregateProgress(weekStart);
        List<WeeklyPlayerScore> scores = activePlayers.stream()
            .map(player -> buildScore(
                player,
                weekStart,
                calculatedAt,
                aggregates.getOrDefault(player.getId(), RankingAggregate.EMPTY),
                existingByPlayerId.get(player.getId())
            ))
            .sorted(rankingComparator())
            .collect(Collectors.toCollection(ArrayList::new));

        for (int index = 0; index < scores.size(); index++) {
            scores.get(index).setPosition(index + 1);
        }

        scoreRepository.saveAll(scores);
        LOGGER.info(
            "Ranking recalculated for week {} with {} player(s).",
            weekStart,
            scores.size()
        );
    }

    /**
     * Aggregates completed challenge points by player.
     */
    private Map<Long, RankingAggregate> aggregateProgress(LocalDate weekStart) {
        Map<Long, RankingAggregate> aggregates = new HashMap<>();

        for (PlayerChallengeProgress progress : progressRepository
            .findAllByWeeklyChallengeWeekStartOrderByPlayerIdAscWeeklyChallengeIdAsc(
                weekStart
            )) {
            if (!progress.isCompleted()) {
                continue;
            }

            long playerId = progress.getPlayer().getId();
            int challengePoints = progress.getWeeklyChallenge()
                .getChallenge()
                .getPoints();
            aggregates.merge(
                playerId,
                new RankingAggregate(challengePoints, 1),
                RankingAggregate::add
            );
        }

        return aggregates;
    }

    /**
     * Creates or refreshes one score while preserving the former position.
     */
    private WeeklyPlayerScore buildScore(
        Player player,
        LocalDate weekStart,
        Instant calculatedAt,
        RankingAggregate aggregate,
        WeeklyPlayerScore existing
    ) {
        WeeklyPlayerScore score = existing == null
            ? new WeeklyPlayerScore()
            : existing;

        score.setPlayer(player);
        score.setWeekStart(weekStart);
        score.setPoints(aggregate.points());
        score.setCompletedChallenges(aggregate.completedChallenges());
        score.setPreviousPosition(existing == null ? null : existing.getPosition());
        score.setCalculatedAt(calculatedAt);
        return score;
    }

    /**
     * Defines stable ranking order and deterministic tie breaking.
     */
    private Comparator<WeeklyPlayerScore> rankingComparator() {
        return Comparator.comparingInt(WeeklyPlayerScore::getPoints)
            .reversed()
            .thenComparing(
                Comparator.comparingInt(
                    WeeklyPlayerScore::getCompletedChallenges
                ).reversed()
            )
            .thenComparing(score -> score.getPlayer().getId());
    }

    /**
     * Resolves the Monday beginning the current UTC calendar week.
     */
    private LocalDate resolveCurrentWeekStart() {
        return LocalDate.now(clock.withZone(ZoneOffset.UTC)).with(
            TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
        );
    }

    /**
     * Immutable score aggregation for one player.
     */
    private record RankingAggregate(int points, int completedChallenges) {

        /**
         * Empty aggregation used when no challenge is completed.
         */
        private static final RankingAggregate EMPTY = new RankingAggregate(0, 0);

        /**
         * Combines two aggregations.
         */
        private RankingAggregate add(RankingAggregate other) {
            return new RankingAggregate(
                points + other.points,
                completedChallenges + other.completedChallenges
            );
        }
    }
}
