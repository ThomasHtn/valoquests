package io.github.thomashtn.valoquests.challenge.calculator;

import io.github.thomashtn.valoquests.challenge.model.ChallengeMetric;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.ToLongFunction;
import org.springframework.stereotype.Component;

/**
 * Calculates the rate a metric takes over a set of matches, as one ratio of totals.
 *
 * <p>Totals divided once, never an average of per-match ratios: a 30-kill Deathmatch and a 2-kill
 * remnant of one are not two equally weighted opinions about a player's aim. This is the only place a
 * rate is defined, so a challenge comparing a player to their own past and a challenge checking an
 * absolute threshold cannot disagree on what the number means.
 */
@Component
public class AggregateRateCalculator {

    /**
     * Scale every rate is calculated at.
     */
    private static final int RATE_SCALE = 4;

    /**
     * Metrics expressed as a rate rather than as a total.
     */
    private static final Set<ChallengeMetric> RATE_METRICS = EnumSet.of(
        ChallengeMetric.KD,
        ChallengeMetric.ACS,
        ChallengeMetric.ADR,
        ChallengeMetric.HEADSHOT_RATE
    );

    /**
     * Indicates whether a metric is one this calculator can express as a rate.
     *
     * @param metric metric to check
     * @return {@code true} when the metric is a rate
     */
    public boolean supports(ChallengeMetric metric) {
        return metric != null && RATE_METRICS.contains(metric);
    }

    /**
     * Calculates the rate a metric takes over the supplied matches.
     *
     * <p>Empty when the rate is not defined, which is not the same as it being zero: a player with no
     * match has no kill-to-death ratio, and treating that as 0.0 would let a challenge asking for
     * improvement be satisfied by having been absent.
     *
     * @param metric  rate metric to calculate
     * @param matches matches to aggregate, already filtered by the caller
     * @return calculated rate, or empty when it is undefined over these matches
     */
    public Optional<BigDecimal> rateOf(ChallengeMetric metric, List<PlayerMatch> matches) {
        if (!supports(metric) || matches.isEmpty()) {
            return Optional.empty();
        }

        return switch (metric) {
            case KD -> killDeathRatio(matches);
            case ACS -> ratio(matches, PlayerMatch::getScore, PlayerMatch::getRoundsPlayed);
            case ADR -> ratio(matches, PlayerMatch::getDamageDealt, PlayerMatch::getRoundsPlayed);
            case HEADSHOT_RATE -> ratio(matches, PlayerMatch::getHeadshots, PlayerMatch::getKills);
            default -> Optional.empty();
        };
    }

    /**
     * Calculates the kill-to-death ratio.
     *
     * <p>A deathless set of matches uses the kill total as its ratio, which avoids dividing by zero
     * while preserving the fact that it satisfies every positive threshold the catalogue asks for.
     *
     * @param matches matches to aggregate
     * @return calculated ratio
     */
    private Optional<BigDecimal> killDeathRatio(List<PlayerMatch> matches) {
        long totalKills = sum(matches, PlayerMatch::getKills);
        long totalDeaths = sum(matches, PlayerMatch::getDeaths);

        if (totalDeaths == 0) {
            return Optional.of(BigDecimal.valueOf(totalKills));
        }

        return Optional.of(divide(totalKills, totalDeaths));
    }

    /**
     * Calculates one total divided by another, or empty when the divisor is zero.
     *
     * @param matches   matches to aggregate
     * @param dividend  value each match contributes to the dividend
     * @param divisor   value each match contributes to the divisor
     * @return calculated ratio, or empty when the divisor totals zero
     */
    private Optional<BigDecimal> ratio(
        List<PlayerMatch> matches,
        ToLongFunction<PlayerMatch> dividend,
        ToLongFunction<PlayerMatch> divisor
    ) {
        long totalDivisor = sum(matches, divisor);

        if (totalDivisor == 0) {
            return Optional.empty();
        }

        return Optional.of(divide(sum(matches, dividend), totalDivisor));
    }

    /**
     * Sums one value across matches.
     *
     * @param matches   matches to aggregate
     * @param extractor value each match contributes
     * @return total
     */
    private long sum(List<PlayerMatch> matches, ToLongFunction<PlayerMatch> extractor) {
        return matches.stream().mapToLong(extractor).sum();
    }

    /**
     * Divides two totals at this calculator's scale.
     *
     * @param dividend dividend total
     * @param divisor  divisor total, must not be zero
     * @return calculated ratio
     */
    private BigDecimal divide(long dividend, long divisor) {
        return BigDecimal.valueOf(dividend)
            .divide(BigDecimal.valueOf(divisor), RATE_SCALE, RoundingMode.HALF_UP);
    }
}
