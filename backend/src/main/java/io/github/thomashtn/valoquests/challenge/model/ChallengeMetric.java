package io.github.thomashtn.valoquests.challenge.model;

/**
 * Identifies a statistic that can be evaluated by the challenge engine.
 */
public enum ChallengeMetric {

    /**
     * Number of matches played.
     */
    MATCHES_PLAYED,

    /**
     * Number of matches won.
     */
    MATCHES_WON,

    /**
     * Total number of kills.
     */
    KILLS,

    /**
     * Total number of assists.
     */
    ASSISTS,

    /**
     * Total number of headshots.
     */
    HEADSHOTS,

    /**
     * Total amount of damage dealt.
     */
    DAMAGE_DEALT,

    /**
     * Total combat score.
     */
    SCORE,

    /**
     * Total number of rounds played.
     */
    ROUNDS_PLAYED,

    /**
     * Kill-to-death ratio.
     */
    KD,

    /**
     * Average combat score per round; a challenge using it must filter on a round-based mode.
     */
    ACS,

    /**
     * Average damage dealt per round; a challenge using it must filter on a round-based mode.
     */
    ADR,

    /**
     * Share of kills that were headshots, as a ratio between zero and one.
     */
    HEADSHOT_RATE,

    /**
     * Calendar day with an eligible match, countable only by a {@code DISTINCT_COUNT} challenge.
     */
    PLAY_DAY;

    /**
     * Indicates whether the metric is a rate, calculated from totals, rather than a total.
     *
     * @return {@code true} for a rate metric
     */
    public boolean isRate() {
        return switch (this) {
            case KD, ACS, ADR, HEADSHOT_RATE -> true;
            default -> false;
        };
    }
}
