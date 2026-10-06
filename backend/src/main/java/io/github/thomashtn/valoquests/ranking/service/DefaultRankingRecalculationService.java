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
import io.github.thomashtn.valoquests.shared.time.WeekCalendar;
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
 * <p>Score = guardian damage priced by {@link DailyOutputReader} plus challenge points, nothing else. Who
 * counts is decided here: an inactive player gets a row with no damage, points or position.
 */
@Service
public class DefaultRankingRecalculationService implements RankingRecalculationService {

    /**
     * Application logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRankingRecalculationService.class);

    /**
     * Orders a week by points, then damage, validated challenges, played days and player id for stability.
     */
    private static final Comparator<WeeklyPlayerScore> RANKING_ORDER = Comparator
        .comparingInt(WeeklyPlayerScore::getTotalPoints).reversed()
        .thenComparing(Comparator.comparingInt(WeeklyPlayerScore::getGuardianDamage).reversed())
        .thenComparing(Comparator.comparingInt(WeeklyPlayerScore::totalCompletedChallenges).reversed())
        .thenComparing(Comparator.comparingInt(WeeklyPlayerScore::getPlayedDays).reversed())
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
            .findAllByWeekStartOrderByPositionAscPlayerIdAsc(weekStart)
            .stream()
            .collect(Collectors.toMap(score -> score.getPlayer().getId(), Function.identity()));

        // Only the competing squad is priced: an inactive player's matches are worth nothing here.
        DailyOutput output = dailyOutputReader.read(
            EnumSet.of(Player.COMPETITIVE_STATUS),
            weekStart,
            WeekCalendar.lastDayOf(weekStart)
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
        rank(scores);

        scoreRepository.saveAll(scores);

        LOGGER.info("Ranking recalculated for week {} with {} player(s).", weekStart, scores.size());
    }

    /**
     * Assigns competition positions (1, 1, 3), none to a player without points or not competing.
     *
     * @param scores rows already in ranking order
     */
    private static void rank(List<WeeklyPlayerScore> scores) {
        List<Integer> positions = CompetitionRanking.positions(
            scores,
            score -> score.getPlayer().isCompetitive() && score.getTotalPoints() > 0,
            WeeklyPlayerScore::getTotalPoints
        );

        for (int index = 0; index < scores.size(); index++) {
            scores.get(index).setPosition(positions.get(index));
        }
    }

    /**
     * Writes one player's week into their row.
     *
     * <p>An inactive player keeps only their validation counts, so they can still see their pace.
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
        score.setPlayedDays(week.playedDays());
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
        int playedDays = 0;

        LocalDate lastDay = WeekCalendar.lastDayOf(weekStart);
        for (LocalDate day = weekStart; !day.isAfter(lastDay); day = day.plusDays(1)) {
            PlayerDayOutput dayOutput = output.of(playerId, day);

            damage += dayOutput.damage();
            food += dayOutput.food();
            components += dayOutput.components();
            matchCount += dayOutput.matchCount();
            playedDays = Math.max(playedDays, output.playedDaysUpTo(playerId, day));
        }

        return new WeekOutput(damage, food, components, matchCount, playedDays);
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
     * @param playedDays days played this week
     */
    private record WeekOutput(int damage, int food, int components, int matchCount, int playedDays) {

        /**
         * The week of a player whose matches do not count.
         */
        private static final WeekOutput NONE = new WeekOutput(0, 0, 0, 0, 0);
    }
}
