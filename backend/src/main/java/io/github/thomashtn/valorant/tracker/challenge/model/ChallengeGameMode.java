package io.github.thomashtn.valorant.tracker.challenge.model;

import io.github.thomashtn.valorant.tracker.match.model.GameMode;

/**
 * Defines the game-mode filters supported by challenge conditions.
 *
 * <p>Restricted to modes synchronization actually imports: a filter on a mode the tracker does not
 * store would define a challenge that can never progress. See {@link GameMode#isImportEligible()}.
 */
public enum ChallengeGameMode {

    /**
     * Includes every supported game mode.
     */
    ANY,

    /**
     * Includes competitive matches.
     */
    COMPETITIVE,

    /**
     * Includes deathmatch matches.
     */
    DEATHMATCH,

    /**
     * Includes team deathmatch matches.
     */
    TEAM_DEATHMATCH;

    /**
     * Determines whether the supplied persisted game mode matches this filter.
     *
     * @param gameMode persisted match game mode
     * @return {@code true} when the match must be included
     */
    public boolean matches(GameMode gameMode) {
        if (this == ANY) {
            return true;
        }

        return gameMode != null && name().equals(gameMode.name());
    }
}
