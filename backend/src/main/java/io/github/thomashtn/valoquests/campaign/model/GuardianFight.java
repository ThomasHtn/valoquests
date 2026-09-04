package io.github.thomashtn.valoquests.campaign.model;

import java.time.Instant;

/**
 * How one week's guardian fight stands, replayed from the week's matches.
 *
 * <p>The finishing blow belongs to the match that took the guardian's last hit point, never to the
 * synchronization that discovered it half an hour later: the instant kept is the match's own start.
 *
 * @param damageDealt      damage the roster dealt over the week
 * @param defeated         whether the guardian fell
 * @param defeatedAt       start instant of the match that landed the finishing blow
 * @param playerId         operator who landed it
 * @param playerMatchId    match that landed it
 */
public record GuardianFight(
    int damageDealt,
    boolean defeated,
    Instant defeatedAt,
    Long playerId,
    Long playerMatchId
) {

    /**
     * A week nobody has played yet.
     */
    public static final GuardianFight UNTOUCHED = new GuardianFight(0, false, null, null, null);
}
