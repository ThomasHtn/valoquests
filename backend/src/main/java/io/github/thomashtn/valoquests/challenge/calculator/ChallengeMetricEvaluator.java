package io.github.thomashtn.valoquests.challenge.calculator;

import io.github.thomashtn.valoquests.challenge.model.ChallengeMetric;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.model.MatchResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
            case KD -> evaluateKillDeathRatio(playerMatch);
            case PLAY_DAY -> throw new IllegalArgumentException(
                "PLAY_DAY must be evaluated through a grouped calculator."
            );
        };
    }

    /**
     * Returns one when the match was won and zero otherwise.
     *
     * @param playerMatch player-match statistics
     * @return numeric win contribution
     */
    private BigDecimal evaluateWin(PlayerMatch playerMatch) {
        return playerMatch.getResult() == MatchResult.WIN
            ? BigDecimal.ONE
            : BigDecimal.ZERO;
    }

    /**
     * Calculates the kill-to-death ratio for one match.
     *
     * <p>A deathless match uses the number of kills as its ratio value. This
     * avoids division by zero while preserving the fact that such a match
     * satisfies every positive K/D threshold supported by the current
     * challenge catalogue.</p>
     *
     * @param playerMatch player-match statistics
     * @return per-match K/D ratio
     */
    private BigDecimal evaluateKillDeathRatio(
        PlayerMatch playerMatch
    ) {
        BigDecimal kills = BigDecimal.valueOf(playerMatch.getKills());

        if (playerMatch.getDeaths() == 0) {
            return kills;
        }

        return kills.divide(
            BigDecimal.valueOf(playerMatch.getDeaths()),
            RATIO_SCALE,
            RoundingMode.HALF_UP
        );
    }
}
