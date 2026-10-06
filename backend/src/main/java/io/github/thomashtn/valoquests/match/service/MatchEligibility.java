package io.github.thomashtn.valoquests.match.service;

import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.model.GameMode;
import org.springframework.stereotype.Component;

/**
 * Decides whether one played match counts, anywhere in the application.
 *
 * <p>The single answer to "was this really played". It lives in {@code match} rather than in the
 * scoring or challenge packages because it is a structural property of the match itself, owned by
 * neither consumer: damage, played days and challenge progress all ask here, so a remake worth no
 * damage cannot progress a volume challenge either, and no challenge is farmable by requeuing.
 *
 * <p>The mode is part of the question: an unrecognized queue is imported on purpose (see
 * {@link GameMode#OTHER}) but the scoring table cannot price it, so it must neither count as a day
 * played nor progress any challenge.
 */
@Component
public final class MatchEligibility {

    /**
     * Determines whether a played match counts at all.
     *
     * @param playerMatch tracked player's statistics for the match
     * @return {@code true} when the match was really played, a round and a positive score, in a mode
     *     the competition counts
     */
    public boolean isEligible(PlayerMatch playerMatch) {
        GameMode gameMode = playerMatch.getMatch().getGameMode();

        return gameMode != null
            && gameMode.isScored()
            && playerMatch.getRoundsPlayed() >= 1
            && playerMatch.getScore() > 0;
    }
}
