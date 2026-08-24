package io.github.thomashtn.valoquests.boss.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.player.entity.Player;

/**
 * Outcome of replaying one week's damage chronology against a boss's effective hit points.
 *
 * @param defeated             whether the cumulative chronology ever reached the effective hit points
 * @param defeatedByPlayer     player who dealt the finishing blow, present only when {@code defeated}
 * @param finishingPlayerMatch match that dealt the finishing blow, present only when {@code defeated}
 * @param totalDamage          damage the whole week dealt, counted past the finishing blow
 */
public record BossChronologyResult(
    boolean defeated,
    Player defeatedByPlayer,
    PlayerMatch finishingPlayerMatch,
    int totalDamage
) {

    /**
     * Result used when no match was played at all.
     */
    public static final BossChronologyResult UNTOUCHED =
        new BossChronologyResult(false, null, null, 0);

    /**
     * Creates the result of a week that dealt damage without ever felling the boss.
     *
     * @param totalDamage damage the whole week dealt
     * @return surviving-boss result
     */
    public static BossChronologyResult survived(int totalDamage) {
        return new BossChronologyResult(false, null, null, totalDamage);
    }

    /**
     * Creates a chronology result.
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "This result is an internal domain value, not a defensively-copyable DTO: the "
            + "player and match it names are the same JPA entities the caller already holds."
    )
    public BossChronologyResult {
    }

    @Override
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "This result is an internal domain value, not a defensively-copyable DTO: the "
            + "player it names is the same JPA entity the caller already holds."
    )
    public Player defeatedByPlayer() {
        return defeatedByPlayer;
    }

    @Override
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "This result is an internal domain value, not a defensively-copyable DTO: the "
            + "match it names is the same JPA entity the caller already holds."
    )
    public PlayerMatch finishingPlayerMatch() {
        return finishingPlayerMatch;
    }
}
