package io.github.thomashtn.valoquests.scoring;

import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.scoring.model.BossCategory;
import io.github.thomashtn.valoquests.scoring.model.MatchOutcome;

/**
 * Defines one versioned set of damage barèmes.
 *
 * <p>A ruleset is resolved once, at the moment a week or a boss encounter is created, and its version
 * number is persisted alongside the result it produced. Recalculating an already-finalized week must
 * always resolve the ruleset through that persisted version, never through
 * {@link ScoringRulesetRegistry#current()} — otherwise a future barème adjustment would silently rewrite
 * closed history.
 *
 * <p>Implemented as versioned Java classes rather than database rows, on purpose: barème adjustments are
 * expected to be rare (see chapter 11 of the design notes — the collective difficulty modifier absorbs
 * most calibration drift on its own) and this mirrors how {@link GameMode} is already a fixed enum rather
 * than editable data.
 */
public interface ScoringRuleset {

    /**
     * Returns the version number this ruleset resolves to.
     *
     * @return ruleset version, starting at 1
     */
    int version();

    /**
     * Returns the damage dealt by one valued match.
     *
     * @param gameMode mode the match was played in
     * @param outcome  normalized outcome from the tracked player's perspective
     * @return damage inflicted, or zero when the mode is not valued
     */
    int matchDamage(GameMode gameMode, MatchOutcome outcome);

    /**
     * Returns the damage dealt by completing one challenge of the given difficulty.
     *
     * @param difficulty completed challenge's difficulty tier
     * @return damage inflicted
     */
    int challengeDamage(ChallengeDifficulty difficulty);

    /**
     * Returns the regularity bonus for a number of distinct active days in the week.
     *
     * @param activeDays number of distinct days with at least one valid match, from 0 to 7
     * @return total regularity bonus, not cumulative across tiers
     */
    int regularityBonus(int activeDays);

    /**
     * Returns the per-player team bonus once a fixed number of players completed the same challenge.
     *
     * @param playersWhoCompleted final number of players who completed the challenge this week
     * @return per-player bonus, not cumulative across tiers
     */
    int teamBonus(int playersWhoCompleted);

    /**
     * Returns the base hit points of one boss category, before the weekly difficulty modifier applies.
     *
     * @param category weight class of the drawn boss
     * @return base hit points
     */
    int bossBaseHp(BossCategory category);
}
