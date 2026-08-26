package io.github.thomashtn.valoquests.colony.service;

import io.github.thomashtn.valoquests.colony.ColonyRuleset;
import io.github.thomashtn.valoquests.colony.model.ColonyDayActivity;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.scoring.ScoringRuleset;
import io.github.thomashtn.valoquests.scoring.service.MatchDamageCalculator;
import io.github.thomashtn.valoquests.scoring.service.WeeklyMatchDamageResolver;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.LocalDate;
import java.util.HashMap;
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
 * <p>Every player counts, whatever their status. Archiving a player does not delete their matches, so a
 * numerator built this way is stable — which is what makes the replay genuinely pure, and what would
 * break if this filtered on the roster as it currently stands.
 */
@Service
@Transactional(readOnly = true)
public class ColonyActivityReader {

    /**
     * Repository listing the players whose matches feed the colony.
     */
    private final PlayerRepository playerRepository;

    /**
     * Repository loading one player's matches over a period.
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
     * @param playerRepository      player repository
     * @param playerMatchRepository player match repository
     * @param damageResolver        weekly match damage resolver
     * @param damageCalculator      match damage calculator
     * @param scoringRuleset        scoring ruleset
     * @param colonyRuleset         colony ruleset
     * @param weekCalendar          week calendar
     */
    public ColonyActivityReader(
        PlayerRepository playerRepository,
        PlayerMatchRepository playerMatchRepository,
        WeeklyMatchDamageResolver damageResolver,
        MatchDamageCalculator damageCalculator,
        ScoringRuleset scoringRuleset,
        ColonyRuleset colonyRuleset,
        WeekCalendar weekCalendar
    ) {
        this.playerRepository = playerRepository;
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
     * Walks every player's weeks over a range and folds both readings into one pass.
     *
     * @param firstDay first day of the range, inclusive
     * @param lastDay  last day of the range, inclusive
     * @return both readings, keyed by day
     */
    private Reading read(LocalDate firstDay, LocalDate lastDay) {
        Reading reading = new Reading(new HashMap<>(), new HashMap<>());

        List<Player> players = playerRepository.findAllByOrderByIdAsc();
        LocalDate lastWeek = weekCalendar.weekStartOf(lastDay);

        for (
            LocalDate week = weekCalendar.weekStartOf(firstDay);
            !week.isAfter(lastWeek);
            week = week.plusWeeks(1)
        ) {
            for (Player player : players) {
                accumulateWeek(player, week, reading);
            }
        }

        return reading;
    }

    /**
     * Prices one player's week and folds it into both accumulators.
     *
     * @param player  player whose week is being read
     * @param week    Monday identifying the week
     * @param reading accumulators to fold into
     */
    private void accumulateWeek(Player player, LocalDate week, Reading reading) {
        List<PlayerMatch> matches = playerMatchRepository.findForChallengePeriod(
            player.getId(),
            weekCalendar.startOf(week),
            weekCalendar.endOf(week)
        );

        if (matches.isEmpty()) {
            return;
        }

        Map<Long, Integer> damageByMatchId = damageResolver.resolve(matches, scoringRuleset);

        for (PlayerMatch match : matches) {
            LocalDate day = weekCalendar.dayOf(match.getMatch().getStartedAt());
            int rawDamage = damageCalculator.damageOf(match, scoringRuleset);

            reading.weightedDamageByDay()
                .merge(day, damageByMatchId.getOrDefault(match.getId(), 0), Integer::sum);

            if (rawDamage > 0) {
                reading.rawDamageByDayAndPlayer()
                    .computeIfAbsent(day, ignored -> new HashMap<>())
                    .merge(player.getId(), rawDamage, Integer::sum);
            }
        }
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
