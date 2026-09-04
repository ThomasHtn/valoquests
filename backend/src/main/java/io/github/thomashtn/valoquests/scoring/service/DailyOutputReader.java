package io.github.thomashtn.valoquests.scoring.service;

import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.scoring.ScoringRuleset;
import io.github.thomashtn.valoquests.scoring.model.DailyOutput;
import io.github.thomashtn.valoquests.scoring.model.DailyYield;
import io.github.thomashtn.valoquests.scoring.model.PlayerDayOutput;
import io.github.thomashtn.valoquests.scoring.model.ValuedMatch;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prices matches day by day, once, for everything that reads a day.
 *
 * <p>The only place a match's value is resolved. The weekly ranking, the campaign replay, the squad
 * calibration and the match history all read the same figure for one game, so they cannot drift
 * apart: value = base × daily coefficient × (1 + streak bonus), rounded once, then split into food and
 * components by the mode's share.
 *
 * <p>Both multipliers need more than the requested range. The daily coefficient ranks a match inside
 * its own calendar day, so every day the range touches is loaded whole; the streak counts the
 * consecutive played days before the range, so a fixed lookback is loaded ahead of it. One query for
 * the whole roster and the whole window, then grouped in memory: asking per player and per day cost
 * {@code players × days} round trips on a call the campaign replay makes after every synchronization.
 */
@Service
@Transactional(readOnly = true)
public class DailyOutputReader {

    /**
     * Days loaded ahead of the requested range so the streak is known on its first day.
     *
     * <p>Bounds what a streak can be observed at: a run longer than this reads as this many days.
     * Generous next to a bonus that caps at six, and finite so a one-day read never scans a year.
     */
    static final int STREAK_LOOKBACK_DAYS = 60;

    /**
     * Divisor turning a percentage into a ratio.
     */
    private static final double PERCENT_SCALE = 100.0;

    /**
     * How far past the next match {@link #dailyYield} looks for the ladder's next step down.
     *
     * <p>Finite on purpose: past its floor the ladder never pays less again, so an unbounded scan
     * would not terminate.
     */
    private static final int LADDER_LOOKAHEAD = 64;

    /**
     * Orders the matches of one day from the most to the least valuable, so a player's best games
     * always land in the highest-paying ranks.
     *
     * <p>Deliberately not chronological. Ranking by play order would tax warming up: five cheap
     * deathmatch games opening a session would push the ranked games that follow into a reduced
     * tier. Ties fall back on chronological order, so the result stays deterministic.
     */
    private static final Comparator<PricedMatch> MOST_VALUABLE_FIRST = Comparator
        .comparingInt(PricedMatch::baseDamage).reversed()
        .thenComparing(priced -> priced.playerMatch().getMatch().getStartedAt())
        .thenComparing(priced -> priced.playerMatch().getId());

    /**
     * Orders valued matches the way the finishing blow is decided: by start instant, whoever played.
     */
    private static final Comparator<ValuedMatch> CHRONOLOGICAL = Comparator
        .comparing(ValuedMatch::startedAt)
        .thenComparing(ValuedMatch::playerMatchId);

    /**
     * Repository loading the matches of a window.
     */
    private final PlayerMatchRepository playerMatchRepository;

    /**
     * Rule deciding whether a match counts at all, and what it is worth before the multipliers.
     */
    private final MatchDamageCalculator damageCalculator;

    /**
     * Barème the value is resolved against.
     */
    private final ScoringRuleset ruleset;

    /**
     * Calendar resolving the day a match falls on and a day's instant bounds.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the daily output reader.
     *
     * @param playerMatchRepository player match repository
     * @param damageCalculator      match damage calculator
     * @param ruleset               scoring ruleset
     * @param weekCalendar          week calendar
     */
    public DailyOutputReader(
        PlayerMatchRepository playerMatchRepository,
        MatchDamageCalculator damageCalculator,
        ScoringRuleset ruleset,
        WeekCalendar weekCalendar
    ) {
        this.playerMatchRepository = playerMatchRepository;
        this.damageCalculator = damageCalculator;
        this.ruleset = ruleset;
        this.weekCalendar = weekCalendar;
    }

    /**
     * Reads an inclusive range of days for every player holding one of the given statuses.
     *
     * @param statuses statuses a player must hold for their matches to be priced
     * @param firstDay first day of the range, inclusive
     * @param lastDay  last day of the range, inclusive
     * @return the range's output, days and players without a valued match omitted
     */
    public DailyOutput read(Collection<PlayerStatus> statuses, LocalDate firstDay, LocalDate lastDay) {
        List<PlayerMatch> matches = playerMatchRepository.findAllForPeriod(
            statuses,
            weekCalendar.startOfDay(firstDay.minusDays(STREAK_LOOKBACK_DAYS)),
            weekCalendar.endOfDay(lastDay)
        );

        return price(matches, firstDay, lastDay);
    }

    /**
     * Reads an inclusive range of days for one player, whatever their status.
     *
     * @param playerId internal player identifier
     * @param firstDay first day of the range, inclusive
     * @param lastDay  last day of the range, inclusive
     * @return the range's output, days without a valued match omitted
     */
    public DailyOutput readPlayer(long playerId, LocalDate firstDay, LocalDate lastDay) {
        List<PlayerMatch> matches = playerMatchRepository.findForChallengePeriod(
            playerId,
            weekCalendar.startOfDay(firstDay.minusDays(STREAK_LOOKBACK_DAYS)),
            weekCalendar.endOfDay(lastDay)
        );

        return price(matches, firstDay, lastDay);
    }

    /**
     * Reports where one player stands on a day's diminishing-returns ladder, before their next match.
     *
     * <p>Probes the ruleset rather than reading its thresholds: the ladder's shape belongs to
     * {@link ScoringRuleset#matchDamageCoefficientPercent(int)} and a second copy of it here would
     * be a second thing to keep in step.
     *
     * @param playerId internal player identifier
     * @param day      calendar day to report on
     * @return the day's standing
     */
    public DailyYield dailyYield(long playerId, LocalDate day) {
        int playedToday = readPlayer(playerId, day, day).of(playerId, day).matchCount();
        int nextRank = playedToday + 1;
        int nextPercent = ruleset.matchDamageCoefficientPercent(nextRank);

        for (int rank = nextRank + 1; rank <= nextRank + LADDER_LOOKAHEAD; rank++) {
            int percent = ruleset.matchDamageCoefficientPercent(rank);
            if (percent < nextPercent) {
                return new DailyYield(playedToday, nextPercent, rank, percent);
            }
        }

        return new DailyYield(playedToday, nextPercent, null, null);
    }

    /**
     * Prices every eligible match of a window and keeps what falls inside the requested range.
     *
     * @param matches  every match of the window, lookback included, whoever played them
     * @param firstDay first day of the range, inclusive
     * @param lastDay  last day of the range, inclusive
     * @return the range's output
     */
    private DailyOutput price(List<PlayerMatch> matches, LocalDate firstDay, LocalDate lastDay) {
        Map<LocalDate, Map<Long, PlayerDayOutput>> byDayAndPlayer = new HashMap<>();
        Map<Long, Map<LocalDate, Integer>> streakByPlayerAndDay = new HashMap<>();
        List<ValuedMatch> valuedMatches = new ArrayList<>();

        groupEligibleByPlayerAndDay(matches).forEach((playerId, days) -> {
            Map<LocalDate, Integer> streakByDay = new HashMap<>();
            int streak = 0;
            LocalDate previousDay = null;

            for (Map.Entry<LocalDate, List<PricedMatch>> entry : days.entrySet()) {
                LocalDate day = entry.getKey();
                streak = previousDay != null && previousDay.plusDays(1).equals(day) ? streak + 1 : 1;
                previousDay = day;
                streakByDay.put(day, streak);

                if (day.isBefore(firstDay) || day.isAfter(lastDay)) {
                    continue;
                }

                for (ValuedMatch valued : priceDay(entry.getValue(), streak)) {
                    valuedMatches.add(valued);
                    byDayAndPlayer
                        .computeIfAbsent(day, ignored -> new HashMap<>())
                        .merge(playerId, PlayerDayOutput.NONE.plus(valued), (total, one) -> total.plus(valued));
                }
            }

            streakByPlayerAndDay.put(playerId, streakByDay);
        });

        valuedMatches.sort(CHRONOLOGICAL);

        return new DailyOutput(byDayAndPlayer, streakByPlayerAndDay, valuedMatches);
    }

    /**
     * Ranks one player's matches of one day and prices each of them.
     *
     * @param dayMatches valued matches sharing one player and one calendar day
     * @param streakDays streak the day sits at, applied to every match of the day
     * @return the day's matches, priced
     */
    private List<ValuedMatch> priceDay(List<PricedMatch> dayMatches, int streakDays) {
        dayMatches.sort(MOST_VALUABLE_FIRST);
        int streakBonusPercent = ruleset.streakBonusPercent(streakDays);
        List<ValuedMatch> priced = new ArrayList<>(dayMatches.size());

        int rankInDay = 0;
        for (PricedMatch match : dayMatches) {
            rankInDay++;
            PlayerMatch playerMatch = match.playerMatch();
            int coefficientPercent = ruleset.matchDamageCoefficientPercent(rankInDay);
            int damage = (int) Math.round(
                match.baseDamage() * (coefficientPercent / PERCENT_SCALE) * (1 + streakBonusPercent / PERCENT_SCALE)
            );
            int food = (int) Math.round(
                damage * ruleset.foodSharePercent(playerMatch.getMatch().getGameMode()) / PERCENT_SCALE
            );

            priced.add(new ValuedMatch(
                playerMatch.getId(),
                playerMatch.getPlayer().getId(),
                playerMatch.getMatch().getStartedAt(),
                match.day(),
                match.baseDamage(),
                coefficientPercent,
                streakDays,
                streakBonusPercent,
                damage,
                food,
                damage - food
            ));
        }

        return priced;
    }

    /**
     * Keeps the eligible matches and groups them by player, then by day in ascending order.
     *
     * <p>An ineligible match never consumes a rank and never makes a day: a remake must not push a
     * real game of the same day into a reduced tier, nor extend a streak.
     *
     * @param matches every match of the window
     * @return eligible matches grouped by player and sorted by day
     */
    private Map<Long, TreeMap<LocalDate, List<PricedMatch>>> groupEligibleByPlayerAndDay(
        List<PlayerMatch> matches
    ) {
        Map<Long, TreeMap<LocalDate, List<PricedMatch>>> grouped = new HashMap<>();

        for (PlayerMatch playerMatch : matches) {
            if (!damageCalculator.isEligible(playerMatch)) {
                continue;
            }

            LocalDate day = weekCalendar.dayOf(playerMatch.getMatch().getStartedAt());
            grouped
                .computeIfAbsent(playerMatch.getPlayer().getId(), ignored -> new TreeMap<>())
                .computeIfAbsent(day, ignored -> new ArrayList<>())
                .add(new PricedMatch(playerMatch, day, damageCalculator.damageOf(playerMatch, ruleset)));
        }

        return grouped;
    }

    /**
     * One eligible match paired with its day and its value before any multiplier.
     *
     * @param playerMatch tracked player's statistics for the match
     * @param day         calendar day the match falls on
     * @param baseDamage  value the ruleset prices this match at, before the multipliers
     */
    private record PricedMatch(PlayerMatch playerMatch, LocalDate day, int baseDamage) {
    }
}
