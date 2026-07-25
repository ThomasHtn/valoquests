package io.github.thomashtn.valorant.tracker.challenge.model;

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
     * Calendar day on which at least one eligible match was played.
     */
    PLAY_DAY
}
