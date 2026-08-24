package io.github.thomashtn.valoquests.scoring.service;

import io.github.thomashtn.valoquests.challenge.calculator.PlayerChallengeContext;
import io.github.thomashtn.valoquests.challenge.calculator.PlayerChallengeContextFactory;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.scoring.ScoringRuleset;
import java.time.LocalDate;
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
     * Prices each match after the ruleset's daily diminishing returns.
     */
    private final WeeklyMatchDamageResolver damageResolver;

    /**
     * Creates the weekly match damage aggregator.
     *
     * @param contextFactory  player challenge context factory
     * @param damageResolver  weekly match damage resolver
     */
    public WeeklyMatchDamageAggregator(
        PlayerChallengeContextFactory contextFactory,
        WeeklyMatchDamageResolver damageResolver
    ) {
        this.contextFactory = contextFactory;
        this.damageResolver = damageResolver;
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

        int matchDamage = damageResolver.resolve(context.playerMatches(), ruleset)
            .values()
            .stream()
            .mapToInt(Integer::intValue)
            .sum();

        return new Aggregate(matchDamage, damageResolver.countActiveDays(context.playerMatches()));
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
