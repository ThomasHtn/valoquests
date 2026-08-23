package io.github.thomashtn.valoquests.scoring.service;

import io.github.thomashtn.valoquests.challenge.calculator.PlayerChallengeContext;
import io.github.thomashtn.valoquests.challenge.calculator.PlayerChallengeContextFactory;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.scoring.ScoringRuleset;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aggregates one player's match damage and active-day count for a week.
 */
@Component
public class WeeklyMatchDamageAggregator {

    /**
     * Factory building the player's weekly match context.
     */
    private final PlayerChallengeContextFactory contextFactory;

    /**
     * Resolves whether one match is valued and how much damage it deals.
     */
    private final MatchDamageCalculator matchDamageCalculator;

    /**
     * Calendar resolving the calendar day a match falls on.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the weekly match damage aggregator.
     *
     * @param contextFactory        player challenge context factory
     * @param matchDamageCalculator match damage calculator
     * @param weekCalendar          calendar resolving the calendar day a match falls on
     */
    public WeeklyMatchDamageAggregator(
        PlayerChallengeContextFactory contextFactory,
        MatchDamageCalculator matchDamageCalculator,
        WeekCalendar weekCalendar
    ) {
        this.contextFactory = contextFactory;
        this.matchDamageCalculator = matchDamageCalculator;
        this.weekCalendar = weekCalendar;
    }

    /**
     * Aggregates one player's match damage and active-day count for a week.
     *
     * @param player    aggregated player
     * @param weekStart week being aggregated
     * @param ruleset   ruleset resolved for this week
     * @return aggregated match damage and active-day count
     */
    @Transactional(readOnly = true)
    public Aggregate aggregate(Player player, LocalDate weekStart, ScoringRuleset ruleset) {
        PlayerChallengeContext context = contextFactory.create(player, weekStart);

        int matchDamage = 0;
        Set<LocalDate> activeDays = new HashSet<>();

        for (PlayerMatch playerMatch : context.playerMatches()) {
            matchDamage += matchDamageCalculator.damageOf(playerMatch, ruleset);

            if (matchDamageCalculator.isEligible(playerMatch)) {
                activeDays.add(weekCalendar.dayOf(playerMatch.getMatch().getStartedAt()));
            }
        }

        return new Aggregate(matchDamage, activeDays.size());
    }

    /**
     * Aggregated match damage and active-day count for one player and week.
     *
     * @param matchDamage total damage dealt by valued matches
     * @param activeDays  number of distinct days with at least one valid match
     */
    public record Aggregate(int matchDamage, int activeDays) {
    }
}
