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
     * Average combat score per round.
     *
     * <p>Only meaningful for round-based modes; a challenge using it must filter on one.
     */
    ACS,

    /**
     * Average damage dealt per round.
     *
     * <p>Only meaningful for round-based modes; a challenge using it must filter on one.
     */
    ADR,

    /**
     * Share of kills that were headshots, as a ratio between zero and one.
     */
    HEADSHOT_RATE,

    /**
     * Calendar day on which at least one eligible match was played.
     */
    PLAY_DAY
}
