package io.github.thomashtn.valorant.tracker.ranking.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Rebuilds weekly rankings from persisted challenge progress.
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
     *
     * @param playerRepository   player repository
     * @param progressRepository challenge progress repository
     * @param scoreRepository    weekly score repository
     * @param clock              application clock
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
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
     * Recalculates scores and positions for the current UTC week.
     */
    @Override
    @Transactional
    public void recalculateCurrentRanking() {
        recalculateWeek(resolveCurrentWeekStart());
    }

    /**
     * Recalculates scores and deterministic positions for one UTC week.
     *
     * @param weekStart Monday identifying the week to recalculate
     */
    @Override
    @Transactional
    public void recalculateWeek(LocalDate weekStart) {
        validateWeekStart(weekStart);

        Instant calculatedAt = clock.instant();

        List<Player> activePlayers = playerRepository
            .findAllByStatusOrderByIdAsc(PlayerStatus.ACTIVE);

        if (activePlayers.isEmpty()) {
            scoreRepository.deleteAllByWeekStart(weekStart);

            LOGGER.info(
                "Ranking cleared for week {} because no player is active.",
                weekStart
            );

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

        Map<Long, RankingAggregate> aggregates =
            aggregateProgress(weekStart);

        List<WeeklyPlayerScore> scores = activePlayers.stream()
            .map(player -> buildScore(
                player,
                weekStart,
                calculatedAt,
                aggregates.getOrDefault(
                    player.getId(),
                    RankingAggregate.EMPTY
                ),
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
     *
     * @param weekStart week being recalculated
     * @return player aggregations indexed by player identifier
     */
    private Map<Long, RankingAggregate> aggregateProgress(
        LocalDate weekStart
    ) {
        Map<Long, RankingAggregate> aggregates = new HashMap<>();

        List<PlayerChallengeProgress> progressRows =
            progressRepository
                .findAllByWeeklyChallengeWeekStartOrderByPlayerIdAscWeeklyChallengeIdAsc(
                    weekStart
                );

        for (PlayerChallengeProgress progress : progressRows) {
            if (!progress.isCompleted()) {
                continue;
            }

            long playerId = progress.getPlayer().getId();

            int challengePoints = progress
                .getWeeklyChallenge()
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
     * Creates or refreshes one score while preserving its former position.
     *
     * @param player       ranked player
     * @param weekStart    ranking week
     * @param calculatedAt calculation timestamp
     * @param aggregate    calculated player aggregation
     * @param existing     existing score, when available
     * @return score ready to persist
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
        score.setCompletedChallenges(
            aggregate.completedChallenges()
        );
        score.setPreviousPosition(
            existing == null
                ? null
                : existing.getPosition()
        );
        score.setCalculatedAt(calculatedAt);

        return score;
    }

    /**
     * Defines stable ranking order and deterministic tie breaking.
     *
     * @return ranking comparator
     */
    private Comparator<WeeklyPlayerScore> rankingComparator() {
        return Comparator
            .comparingInt(WeeklyPlayerScore::getPoints)
            .reversed()
            .thenComparing(
                Comparator.comparingInt(
                    WeeklyPlayerScore::getCompletedChallenges
                ).reversed()
            )
            .thenComparing(
                score -> score.getPlayer().getId()
            );
    }

    /**
     * Resolves the Monday beginning the current UTC calendar week.
     *
     * @return current UTC week start
     */
    private LocalDate resolveCurrentWeekStart() {
        return LocalDate
            .now(clock.withZone(ZoneOffset.UTC))
            .with(
                TemporalAdjusters.previousOrSame(
                    DayOfWeek.MONDAY
                )
            );
    }

    /**
     * Ensures that the supplied date identifies a Monday.
     *
     * @param weekStart week identifier to validate
     */
    private void validateWeekStart(LocalDate weekStart) {
        Objects.requireNonNull(
            weekStart,
            "weekStart must not be null"
        );

        if (weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new IllegalArgumentException(
                "weekStart must be a Monday"
            );
        }
    }

    /**
     * Immutable score aggregation for one player.
     *
     * @param points              completed challenge points
     * @param completedChallenges number of completed challenges
     */
    private record RankingAggregate(
        int points,
        int completedChallenges
    ) {

        /**
         * Empty aggregation used when no challenge is completed.
         */
        private static final RankingAggregate EMPTY =
            new RankingAggregate(0, 0);

        /**
         * Combines two player aggregations.
         *
         * @param other aggregation to add
         * @return combined aggregation
         */
        private RankingAggregate add(
            RankingAggregate other
        ) {
            return new RankingAggregate(
                points + other.points,
                completedChallenges
                    + other.completedChallenges
            );
        }
    }
}
