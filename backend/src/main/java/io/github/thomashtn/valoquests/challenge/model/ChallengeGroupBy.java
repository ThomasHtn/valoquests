package io.github.thomashtn.valoquests.challenge.model;

/**
 * Defines the dimensions that can be used to group challenge data.
 */
public enum ChallengeGroupBy {

    /**
     * Groups matches by the agent played.
     */
    AGENT,

    /**
     * Groups matches by game mode.
     */
    GAME_MODE,

    /**
     * Groups matches by calendar day.
     */
    PLAY_DAY
}
