package io.github.thomashtn.valoquests.campaign.model;

import java.time.Instant;

/**
 * How one week's guardian fight stands, replayed from the week's matches.
 *
 * <p>The finishing blow is dated by the match's start, never by the synchronization that found it.
 *
 * @param damageDealt      damage the roster dealt over the week
 * @param defeated         whether the guardian fell
 * @param defeatedAt       start instant of the match that landed the finishing blow
 * @param playerId         player who landed it
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
