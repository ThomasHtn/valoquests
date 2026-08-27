package io.github.thomashtn.valoquests.colony.service;

import io.github.thomashtn.valoquests.colony.ColonyRuleset;
import io.github.thomashtn.valoquests.colony.model.ColonyDayActivity;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.scoring.ScoringRuleset;
import io.github.thomashtn.valoquests.scoring.service.MatchDamageCalculator;
import io.github.thomashtn.valoquests.scoring.service.WeeklyMatchDamageResolver;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads what the squad did, day by day, over a stretch of the calendar.
 *
 * <p>Two readings of the same matches, and they are deliberately not the same number.
 *
 * <ul>
 *   <li><b>What was brought home</b> goes through {@link WeeklyMatchDamageResolver}, which is the whole
 *       point: the colony and the weekly ranking then price a given match to the unit, daily diminishing
 *       returns included, and the feature inherits its anti-farming from a barème it does not own.</li>
 *   <li><b>Who turned up</b> is read on raw damage instead, before those diminishing returns. They exist
 *       to stop farming, not to decide whether somebody logged in tonight — without that distinction a
 *       player stringing fifteen games together could watch their own turnout drop by playing more.</li>
 * </ul>
 *
 * <p>Only players holding {@link Player#COMPETITIVE_STATUS} count. This used to read every match
 * whatever the player's status, on the argument that a numerator ignoring the roster is stable across
 * a deactivation and therefore keeps the replay pure. It bought that purity at the price of a town no
 * gauge could account for: a deactivated player kept bringing food in while appearing on none of the
 * roster the turnout rail draws, so an evening's harvest had no author anywhere in the interface, and
 * an operator who had deliberately taken somebody off the roster went on being fed by them.
 *
 * <p>The status is the one the player holds now, so deactivating somebody rewrites the run's past days
 * on the next replay. That is the intended reading — the roster is a statement about who is in the
 * campaign, not only about who is in it today — and it is what the turnout readout has always done,
 * having named the current roster on every day it draws since it was written.
 */
@Service
@Transactional(readOnly = true)
public class ColonyActivityReader {

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
     * Ruleset supplying the raw damage a day must clear to count towards turnout.
     */
    private final ColonyRuleset colonyRuleset;

    /**
     * Calendar resolving week bounds and the day a match falls on.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the activity reader.
     *
     * @param playerMatchRepository player match repository
     * @param damageResolver        weekly match damage resolver
     * @param damageCalculator      match damage calculator
     * @param scoringRuleset        scoring ruleset
     * @param colonyRuleset         colony ruleset
     * @param weekCalendar          week calendar
     */
    public ColonyActivityReader(
        PlayerMatchRepository playerMatchRepository,
        WeeklyMatchDamageResolver damageResolver,
        MatchDamageCalculator damageCalculator,
        ScoringRuleset scoringRuleset,
        ColonyRuleset colonyRuleset,
        WeekCalendar weekCalendar
    ) {
        this.playerMatchRepository = playerMatchRepository;
        this.damageResolver = damageResolver;
        this.damageCalculator = damageCalculator;
        this.scoringRuleset = scoringRuleset;
        this.colonyRuleset = colonyRuleset;
        this.weekCalendar = weekCalendar;
    }

    /**
     * Reads every day of an inclusive range on which at least one match was played.
     *
     * <p>Walks whole weeks because that is the unit the resolver ranks matches in. A day absent from the
     * result is a day nobody played.
     *
     * @param firstDay first day of the range, inclusive
     * @param lastDay  last day of the range, inclusive
     * @return activity indexed by day, days without a match omitted
     */
    public Map<LocalDate, ColonyDayActivity> readActivity(LocalDate firstDay, LocalDate lastDay) {
        Reading reading = read(firstDay, lastDay);
        Map<LocalDate, ColonyDayActivity> activityByDay = new HashMap<>();

        reading.weightedDamageByDay().forEach((day, weightedDamage) -> {
            if (day.isBefore(firstDay) || day.isAfter(lastDay)) {
                return;
            }

            Map<Long, Integer> rawByPlayer = reading.rawDamageByDayAndPlayer()
                .getOrDefault(day, Map.of());

            activityByDay.put(day, new ColonyDayActivity(weightedDamage, presenceCount(rawByPlayer)));
        });

        return activityByDay;
    }

    /**
     * Reads what each player brought to one day, before the daily diminishing returns.
     *
     * <p>Feeds the turnout readout, which shows every player of the roster whether they cleared the
     * threshold, fell short of it, or did not play at all. Players absent from the result played
     * nothing.
     *
     * @param day day to read
     * @return raw damage indexed by player identifier, players who did not play omitted
     */
    public Map<Long, Integer> readRawDamageByPlayer(LocalDate day) {
        return read(day, day).rawDamageByDayAndPlayer().getOrDefault(day, Map.of());
    }

    /**
     * Returns how many players of a day cleared the turnout threshold.
     *
     * @param rawDamageByPlayer raw damage of the day, indexed by player identifier
     * @return players counting towards turnout
     */
    public int presenceCount(Map<Long, Integer> rawDamageByPlayer) {
        return (int) rawDamageByPlayer.values().stream()
            .filter(rawDamage -> rawDamage >= colonyRuleset.presenceDamageThreshold())
            .count();
    }

    /**
     * Reads every player's matches over a range in one query and folds both readings into one pass.
     *
     * <p>Whole weeks are loaded because the daily diminishing returns are ranked inside a player's
     * week: a range cut mid-week would rank a Monday's games against a partial week and price them
     * above what the weekly ranking pays.
     *
     * <p>One query for the whole roster and the whole range, then grouped in memory. Asking per player
     * and per week instead cost {@code players x weeks} round trips on a call the replay makes after
     * every synchronization — eleven weeks against a roster that is free to grow.
     *
     * @param firstDay first day of the range, inclusive
     * @param lastDay  last day of the range, inclusive
     * @return both readings, keyed by day
     */
    private Reading read(LocalDate firstDay, LocalDate lastDay) {
        Reading reading = new Reading(new HashMap<>(), new HashMap<>());

        List<PlayerMatch> matches = playerMatchRepository.findAllForPeriod(
            Player.COMPETITIVE_STATUS,
            weekCalendar.startOf(weekCalendar.weekStartOf(firstDay)),
            weekCalendar.endOf(weekCalendar.weekStartOf(lastDay))
        );

        groupByPlayerAndWeek(matches).values().forEach(week -> accumulateWeek(week, reading));

        return reading;
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
     * Prices one player's week and folds it into both accumulators.
     *
     * @param matches that player-week's matches, chronologically ordered
     * @param reading accumulators to fold into
     */
    private void accumulateWeek(List<PlayerMatch> matches, Reading reading) {
        Map<Long, Integer> damageByMatchId = damageResolver.resolve(matches, scoringRuleset);

        for (PlayerMatch match : matches) {
            LocalDate day = weekCalendar.dayOf(match.getMatch().getStartedAt());
            int rawDamage = damageCalculator.damageOf(match, scoringRuleset);

            reading.weightedDamageByDay()
                .merge(day, damageByMatchId.getOrDefault(match.getId(), 0), Integer::sum);

            if (rawDamage > 0) {
                reading.rawDamageByDayAndPlayer()
                    .computeIfAbsent(day, ignored -> new HashMap<>())
                    .merge(match.getPlayer().getId(), rawDamage, Integer::sum);
            }
        }
    }

    /**
     * One player's slice of one week, the unit the daily diminishing returns are ranked inside.
     *
     * @param playerId  internal player identifier
     * @param weekStart Monday identifying the week
     */
    private record PlayerWeek(Long playerId, LocalDate weekStart) {
    }

    /**
     * Both readings of a stretch of the calendar, accumulated in one pass.
     *
     * @param weightedDamageByDay      damage per day, after the daily diminishing returns
     * @param rawDamageByDayAndPlayer  raw damage per day and per player, before them
     */
    private record Reading(
        Map<LocalDate, Integer> weightedDamageByDay,
        Map<LocalDate, Map<Long, Integer>> rawDamageByDayAndPlayer
    ) {
    }
}
