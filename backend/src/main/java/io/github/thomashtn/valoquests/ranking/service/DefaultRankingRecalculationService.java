package io.github.thomashtn.valoquests.ranking.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.ranking.entity.WeeklyPlayerScore;
import io.github.thomashtn.valoquests.ranking.repository.WeeklyPlayerScoreRepository;
import io.github.thomashtn.valoquests.ranking.service.ChallengePointsReader.ChallengeTally;
import io.github.thomashtn.valoquests.scoring.model.DailyOutput;
import io.github.thomashtn.valoquests.scoring.model.PlayerDayOutput;
import io.github.thomashtn.valoquests.scoring.service.DailyOutputReader;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
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
 * Rebuilds a week's ranking from the stored matches and challenge progress.
 *
 * <p>Two things are added: the guardian damage of the week, priced by {@link DailyOutputReader}
 * exactly as the campaign prices it, and the points of the challenges validated that week. Nothing
 * else: a challenge damages nothing, regularity is already paid inside every match by the streak,
 * and there is no team bonus.
 *
 * <p>Who counts is decided here and nowhere else. An inactive player is still given a row, so their
 * progress stays visible, but the row carries no damage, no points and no position: they measure
 * themselves against the squad without adding to it or taking a slot from it.
 */
@Service
public class DefaultRankingRecalculationService implements RankingRecalculationService {

    /**
     * Application logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRankingRecalculationService.class);

    /**
     * Days in one ranking week.
     */
    private static final int DAYS_PER_WEEK = 7;

    /**
     * Orders a week: most points first, then whoever hit the guardian hardest, then whoever validated
     * the most, then the earliest player so two identical weeks always read in the same order.
     */
    private static final Comparator<WeeklyPlayerScore> RANKING_ORDER = Comparator
        .comparingInt(WeeklyPlayerScore::getTotalPoints).reversed()
        .thenComparing(Comparator.comparingInt(WeeklyPlayerScore::getGuardianDamage).reversed())
        .thenComparing(Comparator.comparingInt(WeeklyPlayerScore::completedAllChallenges).reversed())
        .thenComparing(Comparator.comparingInt(WeeklyPlayerScore::getActiveDays).reversed())
        .thenComparing(score -> score.getPlayer().getId());

    /**
     * Repository listing the players a row is built for.
     */
    private final PlayerRepository playerRepository;

    /**
     * Repository persisting the weekly rows.
     */
    private final WeeklyPlayerScoreRepository scoreRepository;

    /**
     * Reader pricing the week's matches, shared with the campaign.
     */
    private final DailyOutputReader dailyOutputReader;

    /**
     * Reader pricing the week's validated challenges.
     */
    private final ChallengePointsReader challengePointsReader;

    /**
     * Application clock stamping the calculation.
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
     * @param scoreRepository       weekly score repository
     * @param dailyOutputReader     daily output reader
     * @param challengePointsReader challenge points reader
     * @param clock                 application clock
     * @param weekCalendar          week calendar
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public DefaultRankingRecalculationService(
        PlayerRepository playerRepository,
        WeeklyPlayerScoreRepository scoreRepository,
        DailyOutputReader dailyOutputReader,
        ChallengePointsReader challengePointsReader,
        Clock clock,
        WeekCalendar weekCalendar
    ) {
        this.playerRepository = playerRepository;
        this.scoreRepository = scoreRepository;
        this.dailyOutputReader = dailyOutputReader;
        this.challengePointsReader = challengePointsReader;
        this.clock = clock;
        this.weekCalendar = weekCalendar;
    }

    @Override
    @Transactional
    public void recalculateCurrentRanking() {
        recalculateWeek(weekCalendar.currentWeekStart());
    }

    @Override
    @Transactional
    public void recalculateWeek(LocalDate weekStart) {
        validateWeekStart(weekStart);

        Instant calculatedAt = clock.instant();
        List<Player> players = playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED);

        if (players.isEmpty()) {
            scoreRepository.deleteAllByWeekStart(weekStart);
            LOGGER.info("Ranking cleared for week {} because no player is tracked.", weekStart);

            return;
        }

        scoreRepository.deleteAllByWeekStartAndPlayerIdNotIn(
            weekStart,
            players.stream().map(Player::getId).toList()
        );

        Map<Long, WeeklyPlayerScore> existingByPlayerId = scoreRepository
            .findAllByWeekStartOrderByPositionAsc(weekStart)
            .stream()
            .collect(Collectors.toMap(score -> score.getPlayer().getId(), Function.identity()));

        // Only the competing squad's matches are priced: an inactive player's evening is worth
        // nothing to the ranking, so there is no point loading it.
        DailyOutput output = dailyOutputReader.read(
            EnumSet.of(Player.COMPETITIVE_STATUS),
            weekStart,
            weekStart.plusDays(DAYS_PER_WEEK - 1L)
        );
        Map<Long, ChallengeTally> tallies = challengePointsReader.read(weekStart);

        List<WeeklyPlayerScore> scores = new ArrayList<>(players.size());
        for (Player player : players) {
            WeeklyPlayerScore score = existingByPlayerId.getOrDefault(player.getId(), new WeeklyPlayerScore());
            fill(score, player, weekStart, output, tallies.getOrDefault(player.getId(), ChallengeTally.NONE));
            score.setPreviousPosition(score.getId() == null ? null : score.getPosition());
            score.setCalculatedAt(calculatedAt);
            scores.add(score);
        }

        scores.sort(RANKING_ORDER);

        int position = 1;
        for (WeeklyPlayerScore score : scores) {
            score.setPosition(score.getPlayer().isCompetitive() ? position++ : null);
        }

        scoreRepository.saveAll(scores);

        LOGGER.info("Ranking recalculated for week {} with {} player(s).", weekStart, scores.size());
    }

    /**
     * Writes one player's week into their row.
     *
     * <p>An inactive player keeps their validation counts and nothing else: the counts are what
     * lets them see how fast they would go, the rest is what they are not taking part in.
     *
     * @param score     row to fill
     * @param player    player the row belongs to
     * @param weekStart Monday identifying the week
     * @param output    the competing squad's priced week
     * @param tally     the player's validated challenges
     */
    private void fill(
        WeeklyPlayerScore score,
        Player player,
        LocalDate weekStart,
        DailyOutput output,
        ChallengeTally tally
    ) {
        boolean competitive = player.isCompetitive();
        WeekOutput week = competitive ? weekOf(player.getId(), weekStart, output) : WeekOutput.NONE;

        score.setPlayer(player);
        score.setWeekStart(weekStart);
        score.setGuardianDamage(week.damage());
        score.setFood(week.food());
        score.setComponents(week.components());
        score.setMatchCount(week.matchCount());
        score.setActiveDays(week.activeDays());
        score.setStreakDays(week.streakDays());
        score.setChallengePoints(competitive ? tally.points() : 0);
        score.setCompletedChallenges(tally.completedWeekly());
        score.setCompletedDailyChallenges(tally.completedDaily());
        score.setTotalPoints(score.getGuardianDamage() + score.getChallengePoints());
    }

    /**
     * Sums one player's seven days.
     *
     * @param playerId  internal player identifier
     * @param weekStart Monday identifying the week
     * @param output    priced week
     * @return the player's week
     */
    private WeekOutput weekOf(long playerId, LocalDate weekStart, DailyOutput output) {
        int damage = 0;
        int food = 0;
        int components = 0;
        int matchCount = 0;
        int activeDays = 0;
        int streakDays = 0;

        for (int offset = 0; offset < DAYS_PER_WEEK; offset++) {
            LocalDate day = weekStart.plusDays(offset);
            PlayerDayOutput dayOutput = output.of(playerId, day);

            damage += dayOutput.damage();
            food += dayOutput.food();
            components += dayOutput.components();
            matchCount += dayOutput.matchCount();
            activeDays += dayOutput.matchCount() > 0 ? 1 : 0;
            streakDays = Math.max(streakDays, output.streakEndingOn(playerId, day));
        }

        return new WeekOutput(damage, food, components, matchCount, activeDays, streakDays);
    }

    /**
     * Ensures that the supplied date identifies a Monday.
     *
     * @param weekStart week identifier to validate
     */
    private void validateWeekStart(LocalDate weekStart) {
        Objects.requireNonNull(weekStart, "weekStart must not be null");

        if (!weekCalendar.isWeekStart(weekStart)) {
            throw new IllegalArgumentException("weekStart must be a Monday");
        }
    }

    /**
     * What one player's matches produced over a week.
     *
     * @param damage     guardian damage, both multipliers applied
     * @param food       food share
     * @param components components share
     * @param matchCount valued matches played
     * @param activeDays days with at least one valued match
     * @param streakDays longest streak reached during the week
     */
    private record WeekOutput(int damage, int food, int components, int matchCount, int activeDays, int streakDays) {

        /**
         * The week of a player whose matches do not count.
         */
        private static final WeekOutput NONE = new WeekOutput(0, 0, 0, 0, 0, 0);
    }
}
