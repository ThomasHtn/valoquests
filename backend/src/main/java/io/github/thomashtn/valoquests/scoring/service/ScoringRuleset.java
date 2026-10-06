package io.github.thomashtn.valoquests.scoring.service;

import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.ScoredOutcome;
import io.github.thomashtn.valoquests.scoring.model.ChallengeCadence;
import io.github.thomashtn.valoquests.scoring.model.ChallengeCalibration;
import io.github.thomashtn.valoquests.scoring.model.ChallengeTier;

/**
 * Prices matches and challenges: the single scoring table the ranking and the campaign read.
 *
 * <p>A plain bean, not versioned: recalculating any week applies the current table. The campaign's own
 * economy (guardian, survivors, costs) lives with the campaign.
 */
public interface ScoringRuleset {

    /**
     * Divisor turning a percentage into a ratio.
     */
    double PERCENT_SCALE = 100.0;

    /**
     * Returns the damage dealt by one valued match, before the two multipliers apply.
     *
     * @param gameMode mode the match was played in
     * @param outcome  normalized outcome from the tracked player's perspective
     * @return damage inflicted, or zero when the mode is not valued
     */
    int matchDamage(GameMode gameMode, ScoredOutcome outcome);

    /**
     * Returns the percentage of its base damage a match keeps, given its rank within its own day.
     *
     * <p>Ranks are 1-based within a calendar day and assigned by decreasing base damage, not play order,
     * so a player's best matches always keep full value.
     *
     * @param rankInDay 1-based rank of the match within its own calendar day
     * @return percentage of the base damage kept, from 0 to 100
     */
    int matchDamageCoefficientPercent(int rankInDay);

    /**
     * Returns the bonus a match earns from the days its player has played so far this week.
     *
     * <p>"Streak" counts days played since Monday, consecutive or not. The first day gives nothing and the
     * cap stays low so a player who plays less can catch up.
     *
     * @param playedDays number of days of the week with at least one valued match, the day of the
     *                   match included; zero or one means no bonus
     * @return bonus percentage applied on top of the daily coefficient
     */
    int streakBonusPercent(int playedDays);

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
     * Returns the wounded one validated challenge brings back, which are also its ranking points.
     *
     * <p>Proportional to the reference so it weighs the same at every difficulty, and priced at the
     * calibration in force so a challenge validated between two campaigns still pays.
     *
     * @param cadence     whether the challenge was drawn as a daily or a weekly one
     * @param tier        weekly tier, ignored for a daily challenge
     * @param calibration reference and campaign week the challenge is priced at
     * @return wounded rescued and ranking points, rounded once
     */
    int challengeReward(ChallengeCadence cadence, ChallengeTier tier, ChallengeCalibration calibration);

}
