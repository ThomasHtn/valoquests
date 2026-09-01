package io.github.thomashtn.valoquests.scoring.service;

import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.scoring.ScoringRuleset;
import io.github.thomashtn.valoquests.scoring.model.DailyMatchDamage;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prices the roster's matches day by day, once, for everything that reads a day.
 *
 * <p>Lives in {@code scoring/} rather than in either of the features that read it, because what a day
 * was worth is a barème question and both pillars have to get the same answer: the colony turns a day
 * into food, the leaderboard's day scope ranks the same day, and the two publishing different figures
 * for one evening would be a bug nobody could see from either screen alone.
 *
 * <p>Whole weeks are loaded whatever range is asked for, because the daily diminishing returns are
 * ranked <b>inside a player's week</b>: a range cut mid-week would rank a Monday's games against a
 * partial week and price them above what the weekly ranking pays.
 *
 * <p>One query for the whole roster and the whole range, then grouped in memory. Asking per player and
 * per week instead cost {@code players x weeks} round trips on a call the colony replay makes after
 * every synchronization.
 */
@Service
@Transactional(readOnly = true)
public class DailyMatchDamageReader {

    /**
     * Repository loading every tracked player's matches over a period.
     */
    private final PlayerMatchRepository playerMatchRepository;

    /**
     * Resolver pricing every match after the daily diminishing returns.
     */
    private final WeeklyMatchDamageResolver damageResolver;

    /**
     * Calculator deciding whether a match counts at all, and what it is worth before those returns.
     */
    private final MatchDamageCalculator damageCalculator;

    /**
     * Barèmes the damage is resolved against.
     */
    private final ScoringRuleset scoringRuleset;

    /**
     * Calendar resolving week bounds and the day a match falls on.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the daily match damage reader.
     *
     * @param playerMatchRepository player match repository
     * @param damageResolver        weekly match damage resolver
     * @param damageCalculator      match damage calculator
     * @param scoringRuleset        scoring ruleset
     * @param weekCalendar          week calendar
     */
    public DailyMatchDamageReader(
        PlayerMatchRepository playerMatchRepository,
        WeeklyMatchDamageResolver damageResolver,
        MatchDamageCalculator damageCalculator,
        ScoringRuleset scoringRuleset,
        WeekCalendar weekCalendar
    ) {
        this.playerMatchRepository = playerMatchRepository;
        this.damageResolver = damageResolver;
        this.damageCalculator = damageCalculator;
        this.scoringRuleset = scoringRuleset;
        this.weekCalendar = weekCalendar;
    }

    /**
     * Reads an inclusive range of days, both before and after the daily diminishing returns.
     *
     * @param statuses statuses a player must hold for their matches to be priced
     * @param firstDay first day of the range, inclusive
     * @param lastDay  last day of the range, inclusive
     * @return both readings, keyed by day, days and players without a match omitted
     */
    public DailyMatchDamage read(
        Collection<PlayerStatus> statuses,
        LocalDate firstDay,
        LocalDate lastDay
    ) {
        Map<LocalDate, Integer> weightedByDay = new HashMap<>();
        Map<LocalDate, Map<Long, Integer>> weightedByDayAndPlayer = new HashMap<>();
        Map<LocalDate, Map<Long, Integer>> rawByDayAndPlayer = new HashMap<>();

        List<PlayerMatch> matches = playerMatchRepository.findAllForPeriod(
            statuses,
            weekCalendar.startOf(weekCalendar.weekStartOf(firstDay)),
            weekCalendar.endOf(weekCalendar.weekStartOf(lastDay))
        );

        for (List<PlayerMatch> playerWeek : groupByPlayerAndWeek(matches).values()) {
            Map<Long, Integer> damageByMatchId = damageResolver.resolve(playerWeek, scoringRuleset);

            for (PlayerMatch match : playerWeek) {
                LocalDate day = weekCalendar.dayOf(match.getMatch().getStartedAt());
                Long playerId = match.getPlayer().getId();
                int weightedDamage = damageByMatchId.getOrDefault(match.getId(), 0);
                int rawDamage = damageCalculator.damageOf(match, scoringRuleset);

                weightedByDay.merge(day, weightedDamage, Integer::sum);

                if (weightedDamage > 0) {
                    mergeInto(weightedByDayAndPlayer, day, playerId, weightedDamage);
                }

                if (rawDamage > 0) {
                    mergeInto(rawByDayAndPlayer, day, playerId, rawDamage);
                }
            }
        }

        return new DailyMatchDamage(weightedByDay, weightedByDayAndPlayer, rawByDayAndPlayer);
    }

    /**
     * Adds one figure to a day-and-player accumulator.
     *
     * @param accumulator accumulator to fold into
     * @param day         day the figure belongs to
     * @param playerId    internal player identifier
     * @param damage      figure to add
     */
    private void mergeInto(
        Map<LocalDate, Map<Long, Integer>> accumulator,
        LocalDate day,
        Long playerId,
        int damage
    ) {
        accumulator.computeIfAbsent(day, ignored -> new HashMap<>()).merge(playerId, damage, Integer::sum);
    }

    /**
     * Splits a flat list of matches into the player-weeks the resolver prices one at a time.
     *
     * @param matches every match of the range, whoever played them
     * @return matches grouped by the player and the week they belong to
     */
    private Map<PlayerWeek, List<PlayerMatch>> groupByPlayerAndWeek(List<PlayerMatch> matches) {
        Map<PlayerWeek, List<PlayerMatch>> grouped = new LinkedHashMap<>();

        for (PlayerMatch match : matches) {
            PlayerWeek key = new PlayerWeek(
                match.getPlayer().getId(),
                weekCalendar.weekStartOf(match.getMatch().getStartedAt())
            );

            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(match);
        }

        return grouped;
    }

    /**
     * One player's slice of one week, the unit the daily diminishing returns are ranked inside.
     *
     * @param playerId  internal player identifier
     * @param weekStart Monday identifying the week
     */
    private record PlayerWeek(Long playerId, LocalDate weekStart) {
    }
}
