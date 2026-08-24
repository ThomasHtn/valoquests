package io.github.thomashtn.valoquests.challenge.calculator;

import io.github.thomashtn.valoquests.challenge.model.ChallengeCondition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Calculates challenges asking a player to beat their own recent form.
 *
 * <p>The catalogue otherwise measures a player against fixed thresholds, and its hard tiers almost all
 * measure volume: 65 matches, 600 kills, 170 000 of combat score. Those reward whoever has the most
 * free time, and they ask the same thing of a player who is already excellent as of one who is not.
 * This mode asks each player the only question that scales with them — are you better than you were
 * last month — and it cannot be farmed, because playing more matches moves the numerator and the
 * denominator together.
 *
 * <p>The condition's {@code target} is the improvement required, in percent, over the rate the player
 * held across the baseline window. Progress is that same improvement as currently achieved, so the
 * progress bar reads directly as "how much of the asked-for gain is in hand". A player who has
 * regressed sits at zero rather than at a negative number.
 */
@Component
public class BaselineChallengeProgressCalculator implements ChallengeProgressCalculator {

    /**
     * Scale intermediate ratios are calculated at, before being expressed as a percentage.
     */
    private static final int IMPROVEMENT_SCALE = 6;

    /**
     * Turns a ratio into a percentage.
     */
    private static final BigDecimal PERCENT_SCALE = BigDecimal.valueOf(100);

    /**
     * Calculates the rate a metric takes over a set of matches.
     */
    private final AggregateRateCalculator rateCalculator;

    /**
     * Applies the common game-mode and eligibility filters.
     */
    private final ChallengeMatchFilter matchFilter;

    /**
     * Creates the baseline progression calculator.
     *
     * @param rateCalculator aggregate rate calculator
     * @param matchFilter    condition match filter
     */
    public BaselineChallengeProgressCalculator(
        AggregateRateCalculator rateCalculator,
        ChallengeMatchFilter matchFilter
    ) {
        this.rateCalculator = rateCalculator;
        this.matchFilter = matchFilter;
    }

    /**
     * Returns the supported progress mode.
     *
     * @return {@link ProgressMode#BASELINE}
     */
    @Override
    public ProgressMode supportedMode() {
        return ProgressMode.BASELINE;
    }

    /**
     * Calculates how far a player has moved past their own baseline rate.
     *
     * @param definition parsed challenge definition
     * @param context    weekly player context
     * @return normalized progress result
     */
    @Override
    public ChallengeProgressResult calculate(
        ChallengeDefinition definition,
        PlayerChallengeContext context
    ) {
        ChallengeCondition condition = definition.singleCondition();

        validateCondition(condition);

        BigDecimal requiredGainPercent = condition.target();
        List<PlayerMatch> weekMatches = filter(context.playerMatches(), condition);
        List<PlayerMatch> baselineMatches = filter(context.baselineMatches(), condition);

        Optional<BigDecimal> baselineRate = rateCalculator.rateOf(condition.metric(), baselineMatches);
        Optional<BigDecimal> weekRate = rateCalculator.rateOf(condition.metric(), weekMatches);

        // No baseline means no form to improve on: the challenge stays visible at zero rather than
        // being handed to whoever happens to be new, or to whoever sat out the previous month.
        if (baselineRate.isEmpty()
            || baselineRate.orElseThrow().signum() <= 0
            || weekRate.isEmpty()
            || weekMatches.size() < condition.minimumMatches()) {

            return new ChallengeProgressResult(
                BigDecimal.ZERO,
                requiredGainPercent,
                BigDecimal.ZERO,
                false
            );
        }

        return ChallengeProgressResult.from(
            gainPercent(weekRate.orElseThrow(), baselineRate.orElseThrow()),
            requiredGainPercent
        );
    }

    /**
     * Expresses this week's rate as a percentage gain over the baseline rate.
     *
     * @param weekRate     rate held over the evaluated week
     * @param baselineRate rate held over the baseline window, strictly positive
     * @return gain in percent, negative when the player regressed
     */
    private BigDecimal gainPercent(BigDecimal weekRate, BigDecimal baselineRate) {
        return weekRate
            .divide(baselineRate, IMPROVEMENT_SCALE, RoundingMode.HALF_UP)
            .subtract(BigDecimal.ONE)
            .multiply(PERCENT_SCALE);
    }

    /**
     * Keeps the matches one condition applies to.
     *
     * @param playerMatches matches to filter
     * @param condition     challenge condition
     * @return matching matches
     */
    private List<PlayerMatch> filter(List<PlayerMatch> playerMatches, ChallengeCondition condition) {
        return playerMatches.stream()
            .filter(playerMatch -> matchFilter.matches(playerMatch, condition))
            .toList();
    }

    /**
     * Validates the configuration required by a baseline challenge.
     *
     * @param condition challenge condition
     */
    private void validateCondition(ChallengeCondition condition) {
        if (!rateCalculator.supports(condition.metric())) {
            throw new IllegalArgumentException(
                "BASELINE challenges require a rate metric, got: " + condition.metric()
            );
        }

        if (condition.target() == null || condition.target().signum() <= 0) {
            throw new IllegalArgumentException(
                "BASELINE challenges require a positive improvement target."
            );
        }

        if (condition.minimumMatches() == null || condition.minimumMatches() <= 0) {
            throw new IllegalArgumentException(
                "BASELINE challenges require a positive minimum number of matches."
            );
        }
    }
}
