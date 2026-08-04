package io.github.thomashtn.valorant.tracker.scoring.service;

import io.github.thomashtn.valorant.tracker.match.entity.PlayerMatch;
import io.github.thomashtn.valorant.tracker.match.model.GameMode;
import io.github.thomashtn.valorant.tracker.match.model.MatchResult;
import io.github.thomashtn.valorant.tracker.scoring.ScoringRuleset;
import io.github.thomashtn.valorant.tracker.scoring.model.MatchOutcome;
import org.springframework.stereotype.Component;

/**
 * Resolves whether one played match is valued and, when it is, how much damage it deals.
 *
 * <p>Validity is a structural property of the match itself (was it really played, was it remade) and is
 * therefore not versioned: it does not belong to {@link ScoringRuleset}, unlike the damage amounts it
 * gates.
 */
@Component
public final class MatchDamageCalculator {

    /**
     * Kills required for a Deathmatch match to count as a victory.
     *
     * <p>Deathmatch has no reliable team result: it ends when a player reaches this many kills, which is
     * by definition first place, so this replaces {@link PlayerMatch#getResult()} for this one mode.
     */
    private static final int DEATHMATCH_VICTORY_KILLS = 40;

    /**
     * Determines whether a played match is valued at all.
     *
     * @param playerMatch tracked player's statistics for the match
     * @return {@code true} when the match was really played and was not remade
     */
    public boolean isEligible(PlayerMatch playerMatch) {
        return playerMatch.getRoundsPlayed() >= 1
            && playerMatch.getScore() > 0
            && playerMatch.getResult() != MatchResult.REMAKE;
    }

    /**
     * Resolves the normalized outcome of one match from the tracked player's perspective.
     *
     * @param playerMatch tracked player's statistics for the match
     * @return normalized outcome
     */
    public MatchOutcome outcomeOf(PlayerMatch playerMatch) {
        GameMode gameMode = playerMatch.getMatch().getGameMode();

        if (gameMode == GameMode.DEATHMATCH) {
            return playerMatch.getKills() >= DEATHMATCH_VICTORY_KILLS
                ? MatchOutcome.WIN
                : MatchOutcome.LOSS;
        }

        return switch (playerMatch.getResult()) {
            case WIN -> MatchOutcome.WIN;
            case DRAW -> MatchOutcome.DRAW;
            // UNKNOWN: Henrik did not expose a reliable team result. REMAKE never reaches here in
            // practice since callers gate on isEligible first, but the switch must stay exhaustive.
            case LOSS, UNKNOWN, REMAKE -> MatchOutcome.LOSS;
        };
    }

    /**
     * Resolves the damage dealt by one match, or zero when it is not eligible.
     *
     * @param playerMatch tracked player's statistics for the match
     * @param ruleset     ruleset the owning week or boss encounter was resolved against
     * @return damage inflicted by this match
     */
    public int damageOf(PlayerMatch playerMatch, ScoringRuleset ruleset) {
        if (!isEligible(playerMatch)) {
            return 0;
        }

        return ruleset.matchDamage(
            playerMatch.getMatch().getGameMode(),
            outcomeOf(playerMatch)
        );
    }
}
