package io.github.thomashtn.valoquests.match.service;

import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.ScoredOutcome;
import org.springframework.stereotype.Component;

/**
 * Decides how one played match ended, from the tracked player's point of view.
 *
 * <p>Shared by damage scoring and challenge progress. Deathmatch has no team result, so reaching the
 * kill target counts as the win.
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
    public ScoredOutcome outcomeOf(PlayerMatch playerMatch) {
        if (playerMatch.getMatch().getGameMode() == GameMode.DEATHMATCH) {
            return playerMatch.getKills() >= DEATHMATCH_VICTORY_KILLS
                ? ScoredOutcome.WIN
                : ScoredOutcome.LOSS;
        }

        return switch (playerMatch.getResult()) {
            case WIN -> ScoredOutcome.WIN;
            case DRAW -> ScoredOutcome.DRAW;
            // UNKNOWN: Henrik did not expose a reliable team result.
            case LOSS, UNKNOWN -> ScoredOutcome.LOSS;
        };
    }

    /**
     * Determines whether the tracked player won one match.
     *
     * @param playerMatch tracked player's statistics for the match
     * @return {@code true} when the match counts as a victory
     */
    public boolean isVictory(PlayerMatch playerMatch) {
        return outcomeOf(playerMatch) == ScoredOutcome.WIN;
    }
}
