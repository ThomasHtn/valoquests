package io.github.thomashtn.valoquests.scoring.service;

import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.ScoredOutcome;
import io.github.thomashtn.valoquests.scoring.model.ChallengeCadence;
import io.github.thomashtn.valoquests.scoring.model.ChallengeCalibration;
import io.github.thomashtn.valoquests.scoring.model.ChallengeTier;
import io.github.thomashtn.valoquests.scoring.model.ValuedMatch;
import org.springframework.stereotype.Component;

/**
 * The scoring table in force.
 *
 * <p>Values are tuned so an hour of play brings 680 to 780 damage whatever the mode. Diminishing returns
 * curb long sessions while the streak bonus rewards playing often.
 */
@Component
public final class DefaultScoringRuleset implements ScoringRuleset {

    /**
     * Highest daily rank still worth full damage.
     */
    private static final int FULL_DAMAGE_RANK_LIMIT = 5;

    /**
     * Highest daily rank still worth half damage.
     */
    private static final int HALF_DAMAGE_RANK_LIMIT = 9;

    /**
     * Coefficient applied once a day's fifth match has been played.
     */
    private static final int HALF_DAMAGE_PERCENT = 50;

    /**
     * Coefficient applied once a day's ninth match has been played.
     */
    private static final int REDUCED_DAMAGE_PERCENT = 25;

    /**
     * Bonus added per played day of the week beyond the first.
     */
    private static final int STREAK_BONUS_PERCENT_PER_DAY = 2;

    /**
     * Number of bonus days past which the streak bonus stops growing, capping it at 10%.
     */
    private static final int STREAK_BONUS_DAY_CAP = 5;

    /**
     * Food share of a long mode: competitive, premier and unrated.
     */
    private static final int LONG_MODE_FOOD_SHARE_PERCENT = 30;

    /**
     * Food share of a quick mode: deathmatch, team deathmatch, spike rush and skirmish.
     */
    private static final int QUICK_MODE_FOOD_SHARE_PERCENT = 70;

    /**
     * Weight of the daily challenge.
     */
    private static final double DAILY_CHALLENGE_WEIGHT = 1.2;

    /**
     * Reward growth per campaign week, linear.
     */
    private static final int REWARD_PROGRESSION_PERCENT_PER_WEEK = 4;

    /**
     * Divisor turning a challenge weight into survivors per unit of reference.
     */
    private static final double SURVIVORS_PER_REFERENCE_DIVISOR = 1_000.0;

    @Override
    public int matchDamage(GameMode gameMode, ScoredOutcome outcome) {
        if (gameMode == null || outcome == null) {
            return 0;
        }

        return switch (gameMode) {
            case COMPETITIVE, PREMIER -> switch (outcome) {
                case LOSS -> 350;
                case DRAW -> 425;
                case WIN -> 500;
            };
            case UNRATED -> switch (outcome) {
                case LOSS -> 320;
                case DRAW -> 390;
                case WIN -> 460;
            };
            case TEAM_DEATHMATCH -> switch (outcome) {
                case LOSS -> 110;
                case DRAW -> 135;
                case WIN -> 160;
            };
            case SWIFTPLAY -> winOrLose(outcome, 160, 230);
            case SPIKE_RUSH, ESCALATION -> winOrLose(outcome, 110, 150);
            case DEATHMATCH -> winOrLose(outcome, 100, 150);
            case SKIRMISH -> switch (outcome) {
                case LOSS -> 90;
                case DRAW -> 110;
                case WIN -> 130;
            };
            default -> 0;
        };
    }

    /**
     * Resolves damage for the modes that cannot end on a draw.
     *
     * <p>An unexpected {@link ScoredOutcome#DRAW} falls into the defeat tier rather than breaking the
     * calculation.
     *
     * @param outcome    match outcome
     * @param lossDamage damage on defeat
     * @param winDamage  damage on victory
     * @return resolved damage
     */
    private static int winOrLose(ScoredOutcome outcome, int lossDamage, int winDamage) {
        return outcome == ScoredOutcome.WIN ? winDamage : lossDamage;
    }

    @Override
    public int matchDamageCoefficientPercent(int rankInDay) {
        if (rankInDay <= FULL_DAMAGE_RANK_LIMIT) {
            return ValuedMatch.FULL_COEFFICIENT_PERCENT;
        }

        if (rankInDay <= HALF_DAMAGE_RANK_LIMIT) {
            return HALF_DAMAGE_PERCENT;
        }

        return REDUCED_DAMAGE_PERCENT;
    }

    @Override
    public int streakBonusPercent(int playedDays) {
        int bonusDays = Math.clamp(playedDays - 1L, 0, STREAK_BONUS_DAY_CAP);

        return bonusDays * STREAK_BONUS_PERCENT_PER_DAY;
    }

    @Override
    public int foodSharePercent(GameMode gameMode) {
        if (gameMode == null) {
            return 0;
        }

        return switch (gameMode) {
            case COMPETITIVE, PREMIER, UNRATED -> LONG_MODE_FOOD_SHARE_PERCENT;
            case DEATHMATCH, TEAM_DEATHMATCH, SPIKE_RUSH, SKIRMISH, SWIFTPLAY, ESCALATION ->
                QUICK_MODE_FOOD_SHARE_PERCENT;
            default -> 0;
        };
    }

    @Override
    public int rewardProgressionPercent(int weekIndex) {
        int weeksElapsed = Math.max(0, weekIndex - 1);

        return (int) PERCENT_SCALE + weeksElapsed * REWARD_PROGRESSION_PERCENT_PER_WEEK;
    }

    @Override
    public int challengeReward(ChallengeCadence cadence, ChallengeTier tier, ChallengeCalibration calibration) {
        double weight = challengeWeight(cadence, tier);
        double progression = rewardProgressionPercent(calibration.weekIndex()) / PERCENT_SCALE;

        return (int) Math.round(calibration.reference() * weight / SURVIVORS_PER_REFERENCE_DIVISOR * progression);
    }

    /**
     * Returns the weight of a validated challenge, the unit its reward is priced from.
     *
     * @param cadence whether the challenge is a daily or a weekly one
     * @param tier    weekly tier, ignored for a daily challenge
     * @return weight, dimensionless
     */
    private static double challengeWeight(ChallengeCadence cadence, ChallengeTier tier) {
        if (cadence == ChallengeCadence.DAILY) {
            return DAILY_CHALLENGE_WEIGHT;
        }

        if (tier == null) {
            return 0;
        }

        return switch (tier) {
            case EASY -> 1.0;
            case NORMAL -> 1.7;
            case MEDIUM -> 2.7;
            case HARD -> 3.9;
            case VERY_HARD -> 5.4;
        };
    }

}
