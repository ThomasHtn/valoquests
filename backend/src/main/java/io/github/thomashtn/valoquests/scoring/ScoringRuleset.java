package io.github.thomashtn.valoquests.scoring;

import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.MatchOutcome;
import io.github.thomashtn.valoquests.scoring.model.BossCategory;

/**
 * Defines the damage barèmes every weekly calculation resolves against.
 *
 * <p>Deliberately not versioned. Versioning existed to keep closed weeks reproducible across barème
 * adjustments, but it made the barème a week depended on a side effect of when its boss encounter
 * happened to be created: a past week owning no encounter silently fell back on the oldest version,
 * so two adjacent weeks could score under different rules for no reason anyone could observe. A
 * barème adjustment is now a plain edit, and recalculating any week applies the current one.
 *
 * <p>Implemented as a Java bean rather than database rows, on purpose: adjustments are expected to be
 * rare, and this mirrors how {@link GameMode} is already a fixed enum rather than editable data.
 *
 * <p>There is deliberately no collective difficulty modifier and no carried-over hit points. Both
 * existed to keep the fight winnable while its size was a hardcoded constant; the size is now measured
 * from what the roster actually produces, which regulates the same drift from real data instead of from
 * a guess. Keeping them alongside would have stacked a second feedback loop on the first: a survival
 * would have lowered the modifier <em>and</em> the measured reference, easing the next week twice.
 */
public interface ScoringRuleset {

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
     * Returns the hit points a boss must lose to be defeated this week.
     *
     * @param category                 weight class of the drawn boss
     * @param activePlayerCount        number of players the roster holds active when the week opens
     * @param referenceDamagePerPlayer what one player is currently expected to contribute in a week
     * @return hit points for the week
     */
    int bossHitPoints(BossCategory category, int activePlayerCount, int referenceDamagePerPlayer);

    /**
     * Returns the per-player weekly output a fight is sized against before any history exists.
     *
     * <p>Only used to open a campaign. From the second closed week onwards the reference is measured
     * rather than assumed, which is what keeps the fight calibrated as the roster's habits change
     * without anyone editing a constant.
     *
     * @return seed reference damage per player
     */
    int seedReferenceDamagePerPlayer();

    /**
     * Returns how many recently finalized weeks the measured reference is drawn from.
     *
     * @return size of the calibration window, in weeks
     */
    int calibrationWindowWeeks();

    /**
     * Returns the band the measured reference may not leave, as percentages of the seed.
     *
     * <p>A guard rail, not a target. One freak week — a holiday, or a marathon — must not be able to
     * resize every future fight, and a roster that stops playing entirely must not drive the reference
     * to zero and make the next boss fall to a single match.
     *
     * @return lower and upper bound, in percent of {@link #seedReferenceDamagePerPlayer()}
     */
    int calibrationFloorPercent();

    /**
     * Returns the upper bound of the calibration band.
     *
     * @return upper bound, in percent of {@link #seedReferenceDamagePerPlayer()}
     * @see #calibrationFloorPercent()
     */
    int calibrationCeilingPercent();

}
