package io.github.thomashtn.valoquests.scoring;

import io.github.thomashtn.valoquests.challenge.model.ChallengeCadence;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.MatchOutcome;

/**
 * Prices what a match and a challenge are worth: the single barème both the weekly ranking and the
 * campaign read.
 *
 * <p>Deliberately not versioned and implemented as a Java bean rather than database rows: a barème
 * adjustment is a plain edit, and recalculating any week applies the current one. This mirrors how
 * {@link GameMode} is already a fixed enum rather than editable data.
 *
 * <p>The campaign's own economy (guardian size, groups of survivors, extraction costs, base growth)
 * lives with the campaign, not here. This interface only knows the value of one match and of one
 * validated challenge, plus the two multipliers every match goes through.
 */
public interface ScoringRuleset {

    /**
     * Returns the damage dealt by one valued match, before the two multipliers apply.
     *
     * @param gameMode mode the match was played in
     * @param outcome  normalized outcome from the tracked player's perspective
     * @return damage inflicted, or zero when the mode is not valued
     */
    int matchDamage(GameMode gameMode, MatchOutcome outcome);

    /**
     * Returns the percentage of its base damage a match keeps, given its rank within its own day.
     *
     * <p>Diminishing returns on daily volume: this is what turns "play more" into "play more often".
     * Ranks are 1-based and assigned over a single calendar day, by decreasing base damage rather
     * than chronologically, so a player's best matches of the day always keep full value and warming
     * up in a cheap mode can never devalue the ranked games that follow.
     *
     * @param rankInDay 1-based rank of the match within its own calendar day
     * @return percentage of the base damage kept, from 0 to 100
     */
    int matchDamageCoefficientPercent(int rankInDay);

    /**
     * Returns the bonus a match earns from its player's run of consecutive played days.
     *
     * <p>The first day gives nothing: a bonus everyone has is not a bonus. The cap is deliberately
     * low, so a player who skipped a day can still catch up with the leader.
     *
     * @param streakDays number of consecutive calendar days with at least one valued match, the day
     *                   of the match included; zero or one means no streak
     * @return bonus percentage applied on top of the daily coefficient
     */
    int streakBonusPercent(int streakDays);

    /**
     * Returns the share of a match's value that becomes food, the rest becoming components.
     *
     * <p>Long modes lean towards components, quick modes towards food, so a session mixing one
     * competitive match with a few deathmatches is roughly balanced between the two.
     *
     * @param gameMode mode the match was played in
     * @return food share in percent, zero for a mode that is not valued
     */
    int foodSharePercent(GameMode gameMode);

    /**
     * Returns the weight of a validated challenge, the unit both its survivors and its ranking points
     * are priced from.
     *
     * @param cadence    whether the challenge is a daily or a weekly one
     * @param difficulty weekly tier, ignored for a daily challenge
     * @return weight, dimensionless
     */
    double challengeWeight(ChallengeCadence cadence, ChallengeDifficulty difficulty);

    /**
     * Returns how much the campaign's rewards have grown by a given week of the campaign.
     *
     * <p>Linear on purpose: a compounding progression rewards the last weeks out of proportion,
     * while a flat one gives the squad nothing to look forward to.
     *
     * @param weekIndex one-based position of the week inside its campaign
     * @return progression in percent, {@code 100} on the first week
     */
    int rewardProgressionPercent(int weekIndex);

    /**
     * Returns how many survivors one validated challenge brings back for the player who validated it.
     *
     * <p>Proportional to the campaign's reference so a challenge weighs the same for a squad of
     * amateurs and for a squad of professionals.
     *
     * @param reference campaign reference, the weekly output one active player is expected to produce
     * @param weight    challenge weight from {@link #challengeWeight}
     * @param weekIndex one-based week inside the campaign, driving the reward progression
     * @return survivors rescued, rounded once
     */
    int challengeSurvivors(int reference, double weight, int weekIndex);

    /**
     * Returns the points one validated challenge adds to its player's weekly ranking.
     *
     * <p>Sized so a perfect week of challenges weighs about a fifth of a median week of guardian
     * damage: a substantial bonus, never a mandatory path.
     *
     * @param reference reference in force, the weekly output one active player is expected to produce
     * @param weight    challenge weight from {@link #challengeWeight}
     * @return ranking points, rounded once
     */
    int challengeRankingPoints(int reference, double weight);

    /**
     * Returns the lowest reference any campaign can be calibrated at, and the reference used when no
     * campaign has ever been played.
     *
     * @return reference floor
     */
    int referenceFloor();
}
