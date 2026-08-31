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
     * How far past the next match {@link #dailyYield} looks for the ladder's next step down.
     *
     * <p>Generous next to a ladder whose last step is the tenth match of a day, and finite on
     * purpose: past its floor the ladder never pays less again, so an unbounded scan would not
     * terminate.
     */
    private static final int LADDER_LOOKAHEAD = 64;

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
     * @param playerMatches one player's matches for the week, in any order
     * @param ruleset       ruleset the owning week was resolved against
     * @return damage indexed by player-match identifier, one entry per supplied match
     */
    public Map<Long, Integer> resolve(List<PlayerMatch> playerMatches, ScoringRuleset ruleset) {
        Map<Long, Integer> damageByPlayerMatchId = new LinkedHashMap<>();

        resolveDetailed(playerMatches, ruleset).forEach(
            (playerMatchId, damage) -> damageByPlayerMatchId.put(playerMatchId, damage.damage())
        );

        return damageByPlayerMatchId;
    }

    /**
     * Prices every supplied match, keeping the daily coefficient each one was reduced by.
     *
     * <p>An ineligible match is worth zero and never consumes a rank: a remake must not push a real
     * game of the same day into a reduced tier. It carries a zero coefficient too, since it never
     * entered the ladder at all.
     *
     * <p>Exists beside {@link #resolve} for the match-history API, which shows a player what one
     * game was worth and has to name the coefficient to explain the amount. The scoring itself only
     * ever needs the amount, and reads it through {@link #resolve}.
     *
     * @param playerMatches one player's matches for the week, in any order
     * @param ruleset       ruleset the owning week was resolved against
     * @return damage and coefficient indexed by player-match identifier, one entry per match
     */
    public Map<Long, MatchDamage> resolveDetailed(
        List<PlayerMatch> playerMatches,
        ScoringRuleset ruleset
    ) {
        Map<Long, MatchDamage> damageByPlayerMatchId = new LinkedHashMap<>();
        Map<LocalDate, List<PricedMatch>> eligibleByDay = new HashMap<>();

        for (PlayerMatch playerMatch : playerMatches) {
            int baseDamage = matchDamageCalculator.damageOf(playerMatch, ruleset);

            damageByPlayerMatchId.put(playerMatch.getId(), MatchDamage.NONE);

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
        Map<Long, MatchDamage> damageByPlayerMatchId
    ) {
        dayMatches.sort(MOST_VALUABLE_FIRST);

        int rankInDay = 0;
        for (PricedMatch priced : dayMatches) {
            rankInDay++;

            int coefficientPercent = ruleset.matchDamageCoefficientPercent(rankInDay);
            int reducedDamage =
                (int) Math.round(priced.baseDamage() * coefficientPercent / PERCENT_SCALE);

            damageByPlayerMatchId.put(
                priced.playerMatch().getId(),
                new MatchDamage(reducedDamage, coefficientPercent)
            );
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

    /**
     * What one match ended up dealing, and the daily coefficient it was reduced by.
     *
     * @param damage            damage after the day's diminishing returns
     * @param coefficientPercent percentage of its base damage the match kept, or {@code 0} for a
     *     match that never entered the day's ladder
     */
    public record MatchDamage(int damage, int coefficientPercent) {

        /**
         * What an ineligible match is worth: nothing, on no rank.
         */
        public static final MatchDamage NONE = new MatchDamage(0, 0);
    }

    /**
     * Reports where a player stands on today's ladder, before they play their next match.
     *
     * <p>The ladder is applied retroactively everywhere else: a match is priced once it exists, and
     * the interface could only ever say what a game had already been worth. This says what the next
     * one <em>will</em> be worth, which is the only form in which a rule meant to discourage
     * marathon sessions can actually discourage one.
     *
     * <p>Probes the ruleset rather than reading its thresholds: the ladder's shape belongs to
     * {@link ScoringRuleset#matchDamageCoefficientPercent(int)} and a second copy of it here would
     * be a second thing to keep in step.
     *
     * @param playerMatches one player's matches, in any order and over any period
     * @param ruleset       ruleset supplying the coefficient ladder
     * @param day           calendar day to report on
     * @return the day's standing
     */
    public DailyYield dailyYield(List<PlayerMatch> playerMatches, ScoringRuleset ruleset, LocalDate day) {
        int playedToday = (int) playerMatches.stream()
            .filter(matchDamageCalculator::isEligible)
            .filter(playerMatch -> dayOf(playerMatch).equals(day))
            .count();

        int nextRank = playedToday + 1;
        int nextPercent = ruleset.matchDamageCoefficientPercent(nextRank);

        // Scans forward for the first rank paying less than the next match would. Bounded rather
        // than open: a ladder already at its floor never pays less again, and the bound is what says
        // so without asking the ruleset to describe its own shape.
        for (int rank = nextRank + 1; rank <= nextRank + LADDER_LOOKAHEAD; rank++) {
            int percent = ruleset.matchDamageCoefficientPercent(rank);
            if (percent < nextPercent) {
                return new DailyYield(playedToday, nextPercent, rank, percent);
            }
        }

        return new DailyYield(playedToday, nextPercent, null, null);
    }

    /**
     * Where a player stands on one day's diminishing-returns ladder.
     *
     * @param matchesToday     valued matches already played that day
     * @param nextMatchPercent share of its base damage the next match would keep
     * @param dropsAtRank      rank at which the share falls below {@link #nextMatchPercent}, or
     *     {@code null} once the ladder has reached its floor and nothing falls further
     * @param dropsToPercent   share kept from {@link #dropsAtRank} on, or {@code null} at the floor
     */
    public record DailyYield(
        int matchesToday,
        int nextMatchPercent,
        Integer dropsAtRank,
        Integer dropsToPercent
    ) {
    }
}
