package io.github.thomashtn.valoquests.colony.service;

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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads what the squad did, day by day, over a stretch of the calendar.
 *
 * <p>Damage goes through {@link WeeklyMatchDamageResolver}, which is the whole point: the colony and the
 * weekly ranking then price a given match to the unit, daily diminishing returns included, and the
 * feature inherits its anti-farming from a barème it does not own.
 *
 * <p>Every player counts, whatever their status. Archiving a player does not delete their matches, so a
 * numerator built this way is stable — which is what makes the replay genuinely pure, and what would
 * break if this filtered on the roster as it currently stands.
 */
@Service
@Transactional(readOnly = true)
public class ColonyActivityReader {

    /**
     * Repository listing the players whose matches feed the gauges.
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
     * Calculator deciding whether a match counts at all.
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
     * Creates the activity reader.
     *
     * @param playerRepository      player repository
     * @param playerMatchRepository player match repository
     * @param damageResolver        weekly match damage resolver
     * @param damageCalculator      match damage calculator
     * @param scoringRuleset        scoring ruleset
     * @param weekCalendar          week calendar
     */
    public ColonyActivityReader(
        PlayerRepository playerRepository,
        PlayerMatchRepository playerMatchRepository,
        WeeklyMatchDamageResolver damageResolver,
        MatchDamageCalculator damageCalculator,
        ScoringRuleset scoringRuleset,
        WeekCalendar weekCalendar
    ) {
        this.playerRepository = playerRepository;
        this.playerMatchRepository = playerMatchRepository;
        this.damageResolver = damageResolver;
        this.damageCalculator = damageCalculator;
        this.scoringRuleset = scoringRuleset;
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
        Map<LocalDate, Integer> damageByDay = new HashMap<>();
        Map<LocalDate, Set<Long>> playersByDay = new HashMap<>();

        List<Player> players = playerRepository.findAllByOrderByIdAsc();
        LocalDate lastWeek = weekCalendar.weekStartOf(lastDay);

        for (
            LocalDate week = weekCalendar.weekStartOf(firstDay);
            !week.isAfter(lastWeek);
            week = week.plusWeeks(1)
        ) {
            for (Player player : players) {
                accumulateWeek(player, week, damageByDay, playersByDay);
            }
        }

        Map<LocalDate, ColonyDayActivity> activityByDay = new HashMap<>();
        damageByDay.keySet().stream()
            .filter(day -> !day.isBefore(firstDay) && !day.isAfter(lastDay))
            .forEach(day -> activityByDay.put(day, new ColonyDayActivity(
                damageByDay.getOrDefault(day, 0),
                playersByDay.getOrDefault(day, Set.of()).size()
            )));

        return activityByDay;
    }

    /**
     * Prices one player's week and folds it into the daily accumulators.
     *
     * @param player       player whose week is being read
     * @param week         Monday identifying the week
     * @param damageByDay  accumulator of damage per day
     * @param playersByDay accumulator of distinct active players per day
     */
    private void accumulateWeek(
        Player player,
        LocalDate week,
        Map<LocalDate, Integer> damageByDay,
        Map<LocalDate, Set<Long>> playersByDay
    ) {
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

            damageByDay.merge(day, damageByMatchId.getOrDefault(match.getId(), 0), Integer::sum);

            if (damageCalculator.isEligible(match)) {
                playersByDay.computeIfAbsent(day, ignored -> new HashSet<>()).add(player.getId());
            }
        }
    }
}
