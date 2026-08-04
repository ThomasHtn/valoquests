package io.github.thomashtn.valorant.tracker.ranking.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valorant.tracker.boss.service.WeekRulesetResolver;
import io.github.thomashtn.valorant.tracker.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valorant.tracker.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valorant.tracker.player.entity.Player;
import io.github.thomashtn.valorant.tracker.player.repository.PlayerRepository;
import io.github.thomashtn.valorant.tracker.ranking.entity.WeeklyPlayerScore;
import io.github.thomashtn.valorant.tracker.ranking.repository.WeeklyPlayerScoreRepository;
import io.github.thomashtn.valorant.tracker.scoring.ScoringRuleset;
import io.github.thomashtn.valorant.tracker.scoring.service.WeeklyMatchDamageAggregator;
import io.github.thomashtn.valorant.tracker.week.WeekCalendar;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * Resolves the ruleset a week was opened with.
     */
    private final WeekRulesetResolver rulesetResolver;

    /**
     * Aggregates one player's match damage and active-day count for a week.
     */
    private final WeeklyMatchDamageAggregator matchDamageAggregator;

    /**
     * Application clock used for deterministic week and timestamp handling.
     */
    private final Clock clock;

    /**
     * Calendar resolving the current week.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the ranking recalculation service.
     *
     * @param playerRepository      player repository
     * @param progressRepository    challenge progress repository
     * @param scoreRepository       weekly score repository
     * @param rulesetResolver       week ruleset resolver
     * @param matchDamageAggregator weekly match damage aggregator
     * @param clock                 application clock
     * @param weekCalendar          calendar resolving the current week
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public DefaultRankingRecalculationService(
        PlayerRepository playerRepository,
        PlayerChallengeProgressRepository progressRepository,
        WeeklyPlayerScoreRepository scoreRepository,
        WeekRulesetResolver rulesetResolver,
        WeeklyMatchDamageAggregator matchDamageAggregator,
        Clock clock,
        WeekCalendar weekCalendar
    ) {
        this.playerRepository = playerRepository;
        this.progressRepository = progressRepository;
        this.scoreRepository = scoreRepository;
        this.rulesetResolver = rulesetResolver;
        this.matchDamageAggregator = matchDamageAggregator;
        this.clock = clock;
        this.weekCalendar = weekCalendar;
    }

    /**
     * Recalculates scores and positions for the current week.
     */
    @Override
    @Transactional
    public void recalculateCurrentRanking() {
        recalculateWeek(weekCalendar.currentWeekStart());
    }

    /**
     * Recalculates scores and deterministic positions for one week.
     *
     * @param weekStart Monday identifying the week to recalculate
     */
    @Override
    @Transactional
    public void recalculateWeek(LocalDate weekStart) {
        validateWeekStart(weekStart);

        Instant calculatedAt = clock.instant();

        List<Player> players = playerRepository.findAllByOrderByIdAsc();

        if (players.isEmpty()) {
            scoreRepository.deleteAllByWeekStart(weekStart);

            LOGGER.info(
                "Ranking cleared for week {} because no player is tracked.",
                weekStart
            );

            return;
        }

        List<Long> playerIds = players.stream()
            .map(Player::getId)
            .toList();

        scoreRepository.deleteAllByWeekStartAndPlayerIdNotIn(
            weekStart,
            playerIds
        );

        Map<Long, WeeklyPlayerScore> existingByPlayerId = scoreRepository
            .findAllByWeekStartOrderByPositionAsc(weekStart)
            .stream()
            .collect(Collectors.toMap(
                score -> score.getPlayer().getId(),
                Function.identity()
            ));

        ScoringRuleset ruleset = rulesetResolver.resolve(weekStart);
        Map<Long, Integer> completedCountByWeeklyChallengeId =
            countCompletionsByWeeklyChallenge(weekStart);
        Map<Long, RankingAggregate> aggregates =
            aggregateProgress(weekStart, players, ruleset, completedCountByWeeklyChallengeId);

        List<WeeklyPlayerScore> scores = players.stream()
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

        int position = 1;
        for (WeeklyPlayerScore score : scores) {
            // A non-competitive player (e.g. a showcased pro player) still gets a score built and
            // sorted for display, but never consumes a ranking slot.
            score.setPosition(score.getPlayer().isCompetitive() ? position++ : null);
        }

        scoreRepository.saveAll(scores);

        LOGGER.info(
            "Ranking recalculated for week {} with {} player(s).",
            weekStart,
            scores.size()
        );
    }

    /**
     * Counts, for every weekly challenge, how many competitive players have completed it so far.
     *
     * <p>An inactive player's completion is deliberately excluded: it must not inflate the team
     * bonus earned by the players who actually compete.
     *
     * @param weekStart week being recalculated
     * @return completed-player count indexed by weekly challenge identifier
     */
    private Map<Long, Integer> countCompletionsByWeeklyChallenge(LocalDate weekStart) {
        Map<Long, Integer> completedCounts = new HashMap<>();

        for (PlayerChallengeProgress progress : progressRepository
            .findAllByWeeklyChallengeWeekStartOrderByPlayerIdAscWeeklyChallengeIdAsc(weekStart)) {

            if (progress.isCompleted() && progress.getPlayer().isCompetitive()) {
                completedCounts.merge(progress.getWeeklyChallenge().getId(), 1, Integer::sum);
            }
        }

        return completedCounts;
    }

    /**
     * Aggregates match damage, challenge damage, regularity bonus and team bonus by player.
     *
     * <p>An inactive player only ever accumulates {@link RankingAggregate#completedChallenges()},
     * so their individual progress stays visible while never contributing damage of any kind.
     *
     * @param weekStart                        week being recalculated
     * @param players                          players to aggregate
     * @param ruleset                          ruleset resolved for this week
     * @param completedCountByWeeklyChallengeId final completed-player count per weekly challenge
     * @return player aggregations indexed by player identifier
     */
    private Map<Long, RankingAggregate> aggregateProgress(
        LocalDate weekStart,
        List<Player> players,
        ScoringRuleset ruleset,
        Map<Long, Integer> completedCountByWeeklyChallengeId
    ) {
        Map<Long, RankingAggregate> aggregates = new HashMap<>();

        for (Player player : players) {
            aggregates.put(
                player.getId(),
                player.isCompetitive()
                    ? aggregateMatchDamage(player, weekStart, ruleset)
                    : RankingAggregate.EMPTY
            );
        }

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

            if (!progress.getPlayer().isCompetitive()) {
                aggregates.merge(
                    playerId,
                    new RankingAggregate(0, 0, 1, 0, 0, 0),
                    RankingAggregate::add
                );
                continue;
            }

            int challengeDamage = ruleset.challengeDamage(
                progress.getWeeklyChallenge().getChallenge().getDifficulty()
            );

            int teamBonus = ruleset.teamBonus(
                completedCountByWeeklyChallengeId.getOrDefault(
                    progress.getWeeklyChallenge().getId(),
                    1
                )
            );

            aggregates.merge(
                playerId,
                new RankingAggregate(0, challengeDamage, 1, teamBonus, 0, 0),
                RankingAggregate::add
            );
        }

        return aggregates;
    }

    /**
     * Aggregates one player's match damage and active-day count for the week.
     *
     * @param player    aggregated player
     * @param weekStart week being recalculated
     * @param ruleset   ruleset resolved for this week
     * @return match-damage-only aggregate for that player
     */
    private RankingAggregate aggregateMatchDamage(
        Player player,
        LocalDate weekStart,
        ScoringRuleset ruleset
    ) {
        WeeklyMatchDamageAggregator.Aggregate aggregate =
            matchDamageAggregator.aggregate(player, weekStart, ruleset);

        return new RankingAggregate(
            aggregate.matchDamage(),
            0,
            0,
            0,
            ruleset.regularityBonus(aggregate.activeDays()),
            aggregate.activeDays()
        );
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
        score.setChallengeDamage(aggregate.challengeDamage());
        score.setCompletedChallenges(
            aggregate.completedChallenges()
        );
        score.setMatchDamage(aggregate.matchDamage());
        score.setRegularityBonus(aggregate.regularityBonus());
        score.setTeamBonus(aggregate.teamBonus());
        score.setActiveDays(aggregate.activeDays());
        score.setTotalDamage(
            aggregate.matchDamage()
                + aggregate.challengeDamage()
                + aggregate.regularityBonus()
                + aggregate.teamBonus()
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
            .comparingInt(WeeklyPlayerScore::getTotalDamage)
            .reversed()
            .thenComparing(
                Comparator.comparingInt(
                    WeeklyPlayerScore::getCompletedChallenges
                ).reversed()
            )
            .thenComparing(
                Comparator.comparingInt(
                    WeeklyPlayerScore::getActiveDays
                ).reversed()
            )
            .thenComparing(
                score -> score.getPlayer().getId()
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

        if (!weekCalendar.isWeekStart(weekStart)) {
            throw new IllegalArgumentException(
                "weekStart must be a Monday"
            );
        }
    }

    /**
     * Immutable score aggregation for one player.
     *
     * @param matchDamage         damage dealt by valued matches
     * @param challengeDamage     damage dealt by completed challenges
     * @param completedChallenges number of completed challenges
     * @param teamBonus           sum of per-challenge team bonuses
     * @param regularityBonus     bonus for the number of active days
     * @param activeDays          number of distinct active days
     */
    private record RankingAggregate(
        int matchDamage,
        int challengeDamage,
        int completedChallenges,
        int teamBonus,
        int regularityBonus,
        int activeDays
    ) {

        /**
         * Empty aggregation used when no challenge is completed.
         */
        private static final RankingAggregate EMPTY =
            new RankingAggregate(0, 0, 0, 0, 0, 0);

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
                matchDamage + other.matchDamage,
                challengeDamage + other.challengeDamage,
                completedChallenges + other.completedChallenges,
                teamBonus + other.teamBonus,
                regularityBonus + other.regularityBonus,
                activeDays + other.activeDays
            );
        }
    }
}
