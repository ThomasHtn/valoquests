package io.github.thomashtn.valoquests.challenge.calculator;

import io.github.thomashtn.valoquests.challenge.model.ChallengeMetric;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.service.MatchOutcomeResolver;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Extracts normalized challenge metric values from persisted player matches.
 */
@Component
public class ChallengeMetricEvaluator {

    /**
     * Scale used when calculating per-match ratios.
     */
    private static final int RATIO_SCALE = 4;

    /**
     * Shared rule deciding how a match ended.
     */
    private final MatchOutcomeResolver outcomeResolver;

    /**
     * Creates the metric evaluator.
     *
     * @param outcomeResolver shared match outcome rule
     */
    public ChallengeMetricEvaluator(MatchOutcomeResolver outcomeResolver) {
        this.outcomeResolver = outcomeResolver;
    }

    /**
     * Returns the value contributed by one match for a challenge metric.
     *
     * @param playerMatch persisted player-match statistics
     * @param metric metric to evaluate
     * @return normalized metric value
     */
    public BigDecimal evaluate(
        PlayerMatch playerMatch,
        ChallengeMetric metric
    ) {
        return switch (metric) {
            case MATCHES_PLAYED -> BigDecimal.ONE;
            case MATCHES_WON -> evaluateWin(playerMatch);
            case KILLS -> BigDecimal.valueOf(playerMatch.getKills());
            case ASSISTS -> BigDecimal.valueOf(playerMatch.getAssists());
            case HEADSHOTS ->
                BigDecimal.valueOf(playerMatch.getHeadshots());
            case DAMAGE_DEALT ->
                BigDecimal.valueOf(playerMatch.getDamageDealt());
            case SCORE -> BigDecimal.valueOf(playerMatch.getScore());
            case ROUNDS_PLAYED ->
                BigDecimal.valueOf(playerMatch.getRoundsPlayed());
            case KD -> playerMatch.killDeathRatio(RATIO_SCALE);
            case ACS -> perRound(playerMatch.getScore(), playerMatch);
            case ADR -> perRound(playerMatch.getDamageDealt(), playerMatch);
            case HEADSHOT_RATE -> evaluateHeadshotRate(playerMatch);
            case PLAY_DAY -> throw new IllegalArgumentException(
                "PLAY_DAY must be evaluated through a grouped calculator."
            );
        };
    }

    /**
     * Sums one metric over a set of matches.
     *
     * @param metric  metric to evaluate
     * @param matches matches already filtered by the condition
     * @return the total, zero without a match
     */
    public BigDecimal sumOf(ChallengeMetric metric, List<PlayerMatch> matches) {
        return matches.stream()
            .map(playerMatch -> evaluate(playerMatch, metric))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Divides one match total by the rounds it was played over.
     *
     * <p>Every match reaching a calculator has played at least one round, since eligibility is checked
     * before any metric is evaluated. The guard covers the metric being evaluated directly in a test.
     *
     * @param total       value to average
     * @param playerMatch player-match statistics
     * @return per-round average, or zero when the match records no round
     */
    private BigDecimal perRound(int total, PlayerMatch playerMatch) {
        if (playerMatch.getRoundsPlayed() <= 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(total).divide(
            BigDecimal.valueOf(playerMatch.getRoundsPlayed()),
            RATIO_SCALE,
            RoundingMode.HALF_UP
        );
    }

    /**
     * Calculates the share of one match's kills that were headshots.
     *
     * <p>A match without a kill has no headshot rate to speak of and scores zero, which keeps it from
     * satisfying any positive threshold.
     *
     * @param playerMatch player-match statistics
     * @return headshot ratio between zero and one
     */
    private BigDecimal evaluateHeadshotRate(PlayerMatch playerMatch) {
        if (playerMatch.getKills() <= 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(playerMatch.getHeadshots()).divide(
            BigDecimal.valueOf(playerMatch.getKills()),
            RATIO_SCALE,
            RoundingMode.HALF_UP
        );
    }

    /**
     * Returns one when the match was won and zero otherwise.
     *
     * <p>Delegated rather than read from {@code result}: Deathmatch has no team result, and a
     * Deathmatch victory must count here as it does in the damage scoring table.
     *
     * @param playerMatch player-match statistics
     * @return numeric win contribution
     */
    private BigDecimal evaluateWin(PlayerMatch playerMatch) {
        return outcomeResolver.isVictory(playerMatch)
            ? BigDecimal.ONE
            : BigDecimal.ZERO;
    }
}
