package io.github.thomashtn.valoquests.challenge.model;

/**
 * Defines the supported challenge category values.
 */
public enum ChallengeCategory {
    /**
     * Practice-oriented goals.
     */
    TRAINING,
    /**
     * Overall performance goals such as score or rating.
     */
    PERFORMANCE,
    /**
     * Assists and utility goals.
     */
    SUPPORT,
    /**
     * Precision goals such as headshots.
     */
    AIM,
    /**
     * Goals about steady results across matches.
     */
    CONSISTENCY,
    /**
     * Goals tied to the agents played.
     */
    AGENT,
    /**
     * Goals about the number of matches played.
     */
    MATCHES,
    /**
     * Damage dealt goals.
     */
    DAMAGE,
    /**
     * Goals rewarding diversity of maps, agents or modes.
     */
    VARIETY,
    /**
     * Goals about winning matches.
     */
    VICTORY,
    /**
     * Goals tied to a specific map.
     */
    MAP,
    /**
     * Goals about playing together as a squad.
     */
    TEAMPLAY,
    /**
     * Goals about staying alive.
     */
    SURVIVAL,
    /**
     * Goals about the spike and round objectives.
     */
    OBJECTIVE,
    /**
     * Challenges fitting none of the other categories.
     */
    OTHER
}
