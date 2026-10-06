package io.github.thomashtn.valoquests.match.service;

import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.model.GameMode;
import org.springframework.stereotype.Component;

/**
 * Decides whether one played match counts, anywhere in the application.
 *
 * <p>Shared by damage, played days and challenge progress, so a remake or an unpriced mode such as
 * {@link GameMode#OTHER} counts nowhere.
 */
@Component
public final class MatchEligibility {

    /**
     * Determines whether a played match counts at all.
     *
     * @param playerMatch tracked player's statistics for the match
     * @return {@code true} for a scored mode with a round played and a positive score
     */
    public boolean isEligible(PlayerMatch playerMatch) {
        GameMode gameMode = playerMatch.getMatch().getGameMode();

        return gameMode != null
            && gameMode.isScored()
            && playerMatch.getRoundsPlayed() >= 1
            && playerMatch.getScore() > 0;
    }
}
