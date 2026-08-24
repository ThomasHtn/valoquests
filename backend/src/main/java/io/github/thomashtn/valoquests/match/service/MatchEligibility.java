package io.github.thomashtn.valoquests.match.service;

import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.model.MatchResult;
import org.springframework.stereotype.Component;

/**
 * Decides whether one played match counts, anywhere in the application.
 *
 * <p>The single answer to "was this really played". It lives in {@code match} rather than in the
 * scoring or challenge packages because it is a structural property of the match itself, owned by
 * neither consumer: damage, active days and challenge progress all have to agree on it.
 *
 * <p>They did not always. Damage required a played round, a positive score and no remake, while
 * challenge progress filtered on the game mode alone. A remake was therefore worth zero damage and no
 * active day, yet still counted towards "play 12 competitive matches" — which made every volume
 * challenge farmable by requeuing, and left the boss chronology crediting challenge damage to a match
 * that carried none. Both now ask here.
 */
@Component
public final class MatchEligibility {

    /**
     * Determines whether a played match counts at all.
     *
     * @param playerMatch tracked player's statistics for the match
     * @return {@code true} when the match was really played and was not remade
     */
    public boolean isEligible(PlayerMatch playerMatch) {
        return playerMatch.getRoundsPlayed() >= 1
            && playerMatch.getScore() > 0
            && playerMatch.getResult() != MatchResult.REMAKE;
    }
}
