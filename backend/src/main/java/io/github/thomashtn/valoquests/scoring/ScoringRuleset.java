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
     * Returns the damage dealt by one valued match, before the daily coefficient applies.
     *
     * @param gameMode mode the match was played in
     * @param outcome  normalized outcome from the tracked player's perspective
     * @return damage inflicted, or zero when the mode is not valued
     */
    int matchDamage(GameMode gameMode, MatchOutcome outcome);

    /**
     * Returns the percentage of its base damage a match keeps, given its rank within its own day.
     *
     * <p>Diminishing returns on daily volume: this is what turns "play more" into "play more often",
     * which is the whole point of the regularity bonus existing alongside it. Ranks are 1-based and
     * assigned over a single calendar day, by decreasing base damage rather than chronologically, so
     * a player's best matches of the day always keep full value and warming up in a cheap mode can
     * never devalue the ranked games that follow.
     *
     * @param rankInDay 1-based rank of the match within its own calendar day
     * @return percentage of the base damage kept, from 0 to 100
     */
    int matchDamageCoefficientPercent(int rankInDay);

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
     * Returns the per-player team bonus once a number of players completed the same challenge.
     *
     * <p>Retroactive and identical for every player who completed it, whatever the order they got
     * there in: the bonus rewards the squad converging on the same objective, not being late to it.
     * This is the only place the bonus is priced, so the weekly ranking and the boss chronology
     * cannot drift apart on what a completion is worth.
     *
     * @param difficulty          completed challenge's difficulty tier
     * @param playersWhoCompleted number of players who completed the challenge so far
     * @return per-player bonus, not cumulative across tiers
     */
    int challengeTeamBonus(ChallengeDifficulty difficulty, int playersWhoCompleted);

    /**
     * Returns the base hit points of one boss category, before the weekly difficulty modifier and any
     * carried-over hit points apply.
     *
     * @param category          weight class of the drawn boss
     * @param activePlayerCount number of players the roster holds active when the week opens
     * @return base hit points
     */
    int bossBaseHp(BossCategory category, int activePlayerCount);

    /**
     * Returns the difficulty modifier a new week opens with, given how the previous one ended.
     *
     * <p>Owned by the ruleset rather than by the selection service so it is frozen along with every
     * other barème: a modifier living outside the version would silently change how every future week
     * is sized, whichever version that week resolves to.
     *
     * @param previousModifierPercent modifier the most recently finalized encounter was sized with
     * @param previousDefeated        whether that encounter ended in the boss being defeated
     * @return modifier to apply, already clamped to this ruleset's supported range
     */
    int nextDifficultyModifierPercent(int previousModifierPercent, boolean previousDefeated);

    /**
     * Returns the difficulty modifier the very first encounter of a campaign opens with.
     *
     * @return neutral starting modifier, in percent
     */
    int initialDifficultyModifierPercent();

    /**
     * Returns the share of a new boss's base hit points that hit points carried over from a surviving
     * predecessor may not exceed.
     *
     * <p>Uncapped, a surviving boss compounds: its remainder inflates the next fight, which is then
     * likelier to survive and carry an even larger remainder, and the mechanic dies within a month.
     *
     * @return cap expressed as a percentage of the new boss's base hit points, or zero to carry nothing
     */
    int carriedOverHpCapPercent();
}
