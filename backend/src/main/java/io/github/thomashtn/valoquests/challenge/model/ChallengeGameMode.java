package io.github.thomashtn.valoquests.challenge.model;

import io.github.thomashtn.valoquests.match.model.GameMode;

/**
 * Defines the game-mode filters supported by challenge conditions.
 *
 * <p>Restricted to modes synchronization actually imports: a filter on a mode the tracker does not
 * store would define a challenge that can never progress. See {@link GameMode#isImportEligible()}.
 *
 * <p>The long-format filter is an explicit list rather than {@link GameMode#isRoundBased()}: Spike
 * Rush and Skirmish are round-based too, and short. Premier is left out on purpose, so that every
 * label can say "en Compétitif ou Non classé" and stay true.
 */
public enum ChallengeGameMode {

    /**
     * Includes every supported game mode.
     */
    ANY,

    /**
     * Includes competitive matches only. Reserved to the hardest weekly tier.
     */
    COMPETITIVE,

    /**
     * Includes unrated matches only.
     */
    UNRATED,

    /**
     * Includes competitive and unrated matches: the long format without the ranked requirement.
     */
    COMPETITIVE_OR_UNRATED,

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
        if (gameMode == null) {
            return this == ANY;
        }

        return switch (this) {
            case ANY -> true;
            case COMPETITIVE -> gameMode == GameMode.COMPETITIVE;
            case UNRATED -> gameMode == GameMode.UNRATED;
            case COMPETITIVE_OR_UNRATED ->
                gameMode == GameMode.COMPETITIVE || gameMode == GameMode.UNRATED;
            case DEATHMATCH -> gameMode == GameMode.DEATHMATCH;
            case TEAM_DEATHMATCH -> gameMode == GameMode.TEAM_DEATHMATCH;
        };
    }

    /**
     * Tells whether this filter only lets ranked matches through.
     *
     * <p>What the catalogue exposes as "competitive only": a player who never queues ranked cannot
     * complete such a challenge, and the interface has to say so rather than let them find out.
     *
     * @return {@code true} for the competitive-only filter
     */
    public boolean isCompetitiveOnly() {
        return this == COMPETITIVE;
    }
}
