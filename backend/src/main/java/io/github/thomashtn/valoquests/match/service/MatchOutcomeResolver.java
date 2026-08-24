package io.github.thomashtn.valoquests.match.service;

import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.MatchOutcome;
import org.springframework.stereotype.Component;

/**
 * Decides how one played match ended, from the tracked player's point of view.
 *
 * <p>The single answer to "did they win this". Damage barèmes and challenge progress both ask here, so
 * a mode whose result needs interpreting is interpreted once.
 *
 * <p>Deathmatch is that mode. It has no team result to read: it ends when someone reaches the kill
 * target, which is by definition first place. Damage already knew that; challenge progress did not, and
 * read {@link PlayerMatch#getResult()} instead — so a Deathmatch win counted for damage while counting
 * as a defeat for any challenge asking for victories. No catalogue rule combined the two yet, which is
 * the only reason it never showed.
 */
@Component
public final class MatchOutcomeResolver {

    /**
     * Kills required for a Deathmatch match to count as a victory.
     */
    private static final int DEATHMATCH_VICTORY_KILLS = 40;

    /**
     * Resolves the normalized outcome of one match.
     *
     * @param playerMatch tracked player's statistics for the match
     * @return normalized outcome
     */
    public MatchOutcome outcomeOf(PlayerMatch playerMatch) {
        if (playerMatch.getMatch().getGameMode() == GameMode.DEATHMATCH) {
            return playerMatch.getKills() >= DEATHMATCH_VICTORY_KILLS
                ? MatchOutcome.WIN
                : MatchOutcome.LOSS;
        }

        return switch (playerMatch.getResult()) {
            case WIN -> MatchOutcome.WIN;
            case DRAW -> MatchOutcome.DRAW;
            // UNKNOWN: Henrik did not expose a reliable team result. REMAKE never reaches here in
            // practice since callers gate on eligibility first, but the switch must stay exhaustive.
            case LOSS, UNKNOWN, REMAKE -> MatchOutcome.LOSS;
        };
    }

    /**
     * Determines whether the tracked player won one match.
     *
     * @param playerMatch tracked player's statistics for the match
     * @return {@code true} when the match counts as a victory
     */
    public boolean isVictory(PlayerMatch playerMatch) {
        return outcomeOf(playerMatch) == MatchOutcome.WIN;
    }
}
