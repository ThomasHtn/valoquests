package io.github.thomashtn.valoquests.scoring.service;

import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.scoring.ScoringRuleset;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Prices every match one player played over a week, applying the ruleset's daily diminishing returns.
 *
 * <p>The only place a match's rank within its day is resolved. Both the weekly ranking and the boss
 * chronology walk the same matches and must agree to the unit on what each one dealt; when they each
 * priced matches themselves they drifted, and the boss health bar ended up showing a number no
 * calculation ever produced. Anything needing per-match damage goes through here.
 */
@Component
public class WeeklyMatchDamageResolver {

    /**
     * Divisor turning a percentage coefficient back into a ratio.
     */
    private static final double PERCENT_SCALE = 100.0;

    /**
     * Orders the matches of one day from the most to the least valuable, so a player's best games
     * always land in the highest-paying ranks.
     *
     * <p>Deliberately not chronological. Ranking by play order would tax warming up: five cheap
     * Deathmatch games opening a session would push the ranked games that follow into a reduced tier,
     * penalising exactly the individual practice the challenge catalogue asks for. Ties fall back on
     * the chronological order already used everywhere else, so the result stays deterministic.
     */
    private static final Comparator<PricedMatch> MOST_VALUABLE_FIRST = Comparator
        .comparingInt(PricedMatch::baseDamage).reversed()
        .thenComparing(priced -> priced.playerMatch().getMatch().getStartedAt())
        .thenComparing(priced -> priced.playerMatch().getId());

    /**
     * Resolves whether one match is valued and what its base damage is.
     */
    private final MatchDamageCalculator matchDamageCalculator;

    /**
     * Calendar resolving the calendar day a match falls on.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the weekly match damage resolver.
     *
     * @param matchDamageCalculator match damage calculator
     * @param weekCalendar          calendar resolving the calendar day a match falls on
     */
    public WeeklyMatchDamageResolver(
        MatchDamageCalculator matchDamageCalculator,
        WeekCalendar weekCalendar
    ) {
        this.matchDamageCalculator = matchDamageCalculator;
        this.weekCalendar = weekCalendar;
    }

    /**
     * Prices every supplied match, after daily diminishing returns.
     *
     * <p>An ineligible match is worth zero and never consumes a rank: a remake must not push a real
     * game of the same day into a reduced tier.
     *
     * @param playerMatches one player's matches for the week, in any order
     * @param ruleset       ruleset the owning week was resolved against
     * @return damage indexed by player-match identifier, one entry per supplied match
     */
    public Map<Long, Integer> resolve(List<PlayerMatch> playerMatches, ScoringRuleset ruleset) {
        Map<Long, Integer> damageByPlayerMatchId = new LinkedHashMap<>();
        Map<LocalDate, List<PricedMatch>> eligibleByDay = new HashMap<>();

        for (PlayerMatch playerMatch : playerMatches) {
            int baseDamage = matchDamageCalculator.damageOf(playerMatch, ruleset);

            damageByPlayerMatchId.put(playerMatch.getId(), 0);

            if (matchDamageCalculator.isEligible(playerMatch)) {
                eligibleByDay
                    .computeIfAbsent(dayOf(playerMatch), day -> new ArrayList<>())
                    .add(new PricedMatch(playerMatch, baseDamage));
            }
        }

        eligibleByDay.values().forEach(
            dayMatches -> applyDailyCoefficients(dayMatches, ruleset, damageByPlayerMatchId)
        );

        return damageByPlayerMatchId;
    }

    /**
     * Counts the distinct days on which the player played at least one valued match.
     *
     * @param playerMatches one player's matches for the week
     * @return number of distinct active days
     */
    public int countActiveDays(List<PlayerMatch> playerMatches) {
        return (int) playerMatches.stream()
            .filter(matchDamageCalculator::isEligible)
            .map(this::dayOf)
            .distinct()
            .count();
    }

    /**
     * Ranks one day's valued matches and writes each one's reduced damage into the accumulator.
     *
     * @param dayMatches            valued matches sharing one calendar day
     * @param ruleset               ruleset supplying the coefficient ladder
     * @param damageByPlayerMatchId accumulator indexed by player-match identifier
     */
    private void applyDailyCoefficients(
        List<PricedMatch> dayMatches,
        ScoringRuleset ruleset,
        Map<Long, Integer> damageByPlayerMatchId
    ) {
        dayMatches.sort(MOST_VALUABLE_FIRST);

        int rankInDay = 0;
        for (PricedMatch priced : dayMatches) {
            rankInDay++;

            int coefficientPercent = ruleset.matchDamageCoefficientPercent(rankInDay);
            int reducedDamage =
                (int) Math.round(priced.baseDamage() * coefficientPercent / PERCENT_SCALE);

            damageByPlayerMatchId.put(priced.playerMatch().getId(), reducedDamage);
        }
    }

    /**
     * Resolves the calendar day one match falls on.
     *
     * @param playerMatch tracked player's statistics for the match
     * @return local day containing that match
     */
    private LocalDate dayOf(PlayerMatch playerMatch) {
        return weekCalendar.dayOf(playerMatch.getMatch().getStartedAt());
    }

    /**
     * One valued match paired with its damage before the daily coefficient applies.
     *
     * @param playerMatch tracked player's statistics for the match
     * @param baseDamage  damage the ruleset prices this match at, before diminishing returns
     */
    private record PricedMatch(PlayerMatch playerMatch, int baseDamage) {
    }
}
