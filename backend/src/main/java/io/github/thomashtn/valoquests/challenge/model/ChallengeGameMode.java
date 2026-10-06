package io.github.thomashtn.valoquests.challenge.model;

import io.github.thomashtn.valoquests.match.model.GameMode;

/**
 * Defines the game-mode filters supported by challenge conditions.
 *
 * <p>Only modes {@link GameMode#isImportEligible()} accepts, or a challenge could never progress. The
 * long format is an explicit list, not {@link GameMode#isRoundBased()}, and leaves Premier out so every
 * label can say "en Compétitif ou Non classé".
 */
public enum ChallengeGameMode {

    /**
     * Includes every supported game mode.
     */
    ANY,

    /**
     * Includes competitive matches only, reserved to the hardest weekly tier.
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
    TEAM_DEATHMATCH,

    /**
     * Only 2v2 skirmish matches.
     */
    SKIRMISH;

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
            case SKIRMISH -> gameMode == GameMode.SKIRMISH;
        };
    }

    /**
     * Tells whether this filter only lets ranked matches through.
     *
     * <p>Exposed so the interface warns players who never queue ranked.
     *
     * @return {@code true} for the competitive-only filter
     */
    public boolean isCompetitiveOnly() {
        return this == COMPETITIVE;
    }
}
