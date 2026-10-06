package io.github.thomashtn.valoquests.scoring.service;

import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valoquests.match.service.MatchEligibility;
import io.github.thomashtn.valoquests.match.service.MatchOutcomeResolver;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.scoring.model.DailyOutput;
import io.github.thomashtn.valoquests.scoring.model.DailyYield;
import io.github.thomashtn.valoquests.scoring.model.PlayerDayOutput;
import io.github.thomashtn.valoquests.scoring.model.ValuedMatch;
import io.github.thomashtn.valoquests.shared.time.WeekCalendar;
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
 * <p>The only place a match's value is resolved (see docs/GAMEPLAY.md), so ranking, replay and history
 * never drift. Whole days plus the earlier days of the week are loaded in one query, as both multipliers
 * need them.
 */
@Service
@Transactional(readOnly = true)
public class DailyOutputReader {

    /**
     * Days loaded ahead of the range so the week's played days are known on its first day.
     */
    static final int PLAYED_DAYS_LOOKBACK = WeekCalendar.DAYS_PER_WEEK - 1;

    /**
     * How far past the next match {@link #dailyYield} looks for a step down, finite since the floor never drops.
     */
    private static final int LADDER_LOOKAHEAD = 64;

    /**
     * Orders a day's matches from most to least valuable, ties chronological, so warm-up games never
     * push better ones into a reduced tier.
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
     * Rule deciding whether a match counts at all.
     */
    private final MatchEligibility matchEligibility;

    /**
     * Rule deciding how a match ended, which prices it before the multipliers.
     */
    private final MatchOutcomeResolver outcomeResolver;

    /**
     * Scoring table the value is resolved against.
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
     * @param matchEligibility      shared match eligibility rule
     * @param outcomeResolver       shared match outcome rule
     * @param ruleset               scoring ruleset
     * @param weekCalendar          week calendar
     */
    public DailyOutputReader(
        PlayerMatchRepository playerMatchRepository,
        MatchEligibility matchEligibility,
        MatchOutcomeResolver outcomeResolver,
        ScoringRuleset ruleset,
        WeekCalendar weekCalendar
    ) {
        this.playerMatchRepository = playerMatchRepository;
        this.matchEligibility = matchEligibility;
        this.outcomeResolver = outcomeResolver;
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
        List<PlayerMatch> matches = playerMatchRepository.findByPlayerStatusesInPeriod(
            statuses,
            weekCalendar.startOfDay(firstDay.minusDays(PLAYED_DAYS_LOOKBACK)),
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
        List<PlayerMatch> matches = playerMatchRepository.findByPlayerInPeriod(
            playerId,
            weekCalendar.startOfDay(firstDay.minusDays(PLAYED_DAYS_LOOKBACK)),
            weekCalendar.endOfDay(lastDay)
        );

        return price(matches, firstDay, lastDay);
    }

    /**
     * Reports where one player stands on a day's diminishing-returns ladder, before their next match.
     *
     * <p>Probes {@link ScoringRuleset#matchDamageCoefficientPercent(int)} rather than copying its
     * thresholds, so the ladder's shape lives in one place.
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
        Map<Long, Map<LocalDate, Integer>> playedDaysByPlayerAndDay = new HashMap<>();
        List<ValuedMatch> valuedMatches = new ArrayList<>();

        groupEligibleByPlayerAndDay(matches).forEach((playerId, days) -> {
            Map<LocalDate, Integer> playedDaysByDay = new HashMap<>();
            int playedDays = 0;
            LocalDate previousDay = null;

            for (Map.Entry<LocalDate, List<PricedMatch>> entry : days.entrySet()) {
                LocalDate day = entry.getKey();
                // Counts the played days of the week, gaps included; restarts every Monday.
                boolean continued = previousDay != null
                    && weekCalendar.weekStartOf(previousDay).equals(weekCalendar.weekStartOf(day));
                playedDays = continued ? playedDays + 1 : 1;
                previousDay = day;
                playedDaysByDay.put(day, playedDays);

                if (day.isBefore(firstDay) || day.isAfter(lastDay)) {
                    continue;
                }

                for (ValuedMatch valued : priceDay(entry.getValue(), playedDays)) {
                    valuedMatches.add(valued);
                    byDayAndPlayer
                        .computeIfAbsent(day, ignored -> new HashMap<>())
                        .merge(playerId, PlayerDayOutput.NONE.plus(valued), (total, one) -> total.plus(valued));
                }
            }

            playedDaysByPlayerAndDay.put(playerId, playedDaysByDay);
        });

        valuedMatches.sort(CHRONOLOGICAL);

        return new DailyOutput(byDayAndPlayer, playedDaysByPlayerAndDay, valuedMatches);
    }

    /**
     * Ranks one player's matches of one day and prices each of them.
     *
     * @param dayMatches valued matches sharing one player and one calendar day
     * @param playedDays days played this week up to this day, applied to every match of the day
     * @return the day's matches, priced
     */
    private List<ValuedMatch> priceDay(List<PricedMatch> dayMatches, int playedDays) {
        dayMatches.sort(MOST_VALUABLE_FIRST);
        int streakBonusPercent = ruleset.streakBonusPercent(playedDays);
        List<ValuedMatch> priced = new ArrayList<>(dayMatches.size());

        int rankInDay = 0;
        for (PricedMatch match : dayMatches) {
            rankInDay++;
            PlayerMatch playerMatch = match.playerMatch();
            int coefficientPercent = ruleset.matchDamageCoefficientPercent(rankInDay);
            int damage = (int) Math.round(
                match.baseDamage()
                    * (coefficientPercent / ScoringRuleset.PERCENT_SCALE)
                    * (1 + streakBonusPercent / ScoringRuleset.PERCENT_SCALE)
            );
            int food = (int) Math.round(
                damage * ruleset.foodSharePercent(playerMatch.getMatch().getGameMode()) / ScoringRuleset.PERCENT_SCALE
            );

            priced.add(new ValuedMatch(
                playerMatch.getId(),
                playerMatch.getPlayer().getId(),
                playerMatch.getMatch().getStartedAt(),
                match.day(),
                match.baseDamage(),
                coefficientPercent,
                playedDays,
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
     * <p>Ineligible matches are dropped first, so a remake never consumes a rank nor counts as a played day.
     *
     * @param matches every match of the window
     * @return eligible matches grouped by player and sorted by day
     */
    private Map<Long, TreeMap<LocalDate, List<PricedMatch>>> groupEligibleByPlayerAndDay(
        List<PlayerMatch> matches
    ) {
        Map<Long, TreeMap<LocalDate, List<PricedMatch>>> grouped = new HashMap<>();

        for (PlayerMatch playerMatch : matches) {
            if (!matchEligibility.isEligible(playerMatch)) {
                continue;
            }

            LocalDate day = weekCalendar.dayOf(playerMatch.getMatch().getStartedAt());
            int baseDamage = ruleset.matchDamage(
                playerMatch.getMatch().getGameMode(),
                outcomeResolver.outcomeOf(playerMatch)
            );
            grouped
                .computeIfAbsent(playerMatch.getPlayer().getId(), ignored -> new TreeMap<>())
                .computeIfAbsent(day, ignored -> new ArrayList<>())
                .add(new PricedMatch(playerMatch, day, baseDamage));
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
