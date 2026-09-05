package io.github.thomashtn.valoquests.scoring;

import io.github.thomashtn.valoquests.challenge.model.ChallengeCadence;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.MatchOutcome;
import org.springframework.stereotype.Component;

/**
 * The barème in force.
 *
 * <p>Match values are tuned so an hour of play brings roughly the same amount whatever the mode,
 * diminishing returns included: between 680 and 780 damage per hour over a one-hour session. The
 * two multipliers pull in opposite directions on purpose: past the fifth match of a day a game is
 * worth half, so the week is not won by whoever had the most free time, while a run of consecutive
 * days adds a small bonus, so turning up every evening is what wins it.
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
     * Coefficient applied to a day's first matches.
     */
    private static final int FULL_DAMAGE_PERCENT = 100;

    /**
     * Coefficient applied once a day's fifth match has been played.
     */
    private static final int HALF_DAMAGE_PERCENT = 50;

    /**
     * Coefficient applied once a day's ninth match has been played.
     */
    private static final int REDUCED_DAMAGE_PERCENT = 25;

    /**
     * Bonus added per consecutive played day beyond the first.
     */
    private static final int STREAK_BONUS_PERCENT_PER_DAY = 2;

    /**
     * Number of consecutive days past which the streak bonus stops growing, capping it at 10%.
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

    /**
     * Lowest reference a campaign can be calibrated at: four competitive matches and three quick
     * games a week, which is what an irregular squad's first campaign is played at, on purpose.
     */
    private static final int REFERENCE_FLOOR = 2_000;

    /**
     * Divisor turning a percentage into a ratio.
     */
    private static final double PERCENT_SCALE = 100.0;

    @Override
    public int matchDamage(GameMode gameMode, MatchOutcome outcome) {
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
            case SPIKE_RUSH -> winOrLose(outcome, 110, 150);
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
     * <p>Henrik is not expected to ever report {@link MatchOutcome#DRAW} for these modes, but a draw is
     * folded into the defeat tier rather than rejected, so a future upstream surprise degrades quietly
     * instead of breaking the weekly calculation.
     *
     * @param outcome    match outcome
     * @param lossDamage damage on defeat
     * @param winDamage  damage on victory
     * @return resolved damage
     */
    private static int winOrLose(MatchOutcome outcome, int lossDamage, int winDamage) {
        return outcome == MatchOutcome.WIN ? winDamage : lossDamage;
    }

    @Override
    public int matchDamageCoefficientPercent(int rankInDay) {
        if (rankInDay <= FULL_DAMAGE_RANK_LIMIT) {
            return FULL_DAMAGE_PERCENT;
        }

        if (rankInDay <= HALF_DAMAGE_RANK_LIMIT) {
            return HALF_DAMAGE_PERCENT;
        }

        return REDUCED_DAMAGE_PERCENT;
    }

    @Override
    public int streakBonusPercent(int streakDays) {
        int bonusDays = Math.clamp(streakDays - 1L, 0, STREAK_BONUS_DAY_CAP);

        return bonusDays * STREAK_BONUS_PERCENT_PER_DAY;
    }

    @Override
    public int foodSharePercent(GameMode gameMode) {
        if (gameMode == null) {
            return 0;
        }

        return switch (gameMode) {
            case COMPETITIVE, PREMIER, UNRATED -> LONG_MODE_FOOD_SHARE_PERCENT;
            case DEATHMATCH, TEAM_DEATHMATCH, SPIKE_RUSH, SKIRMISH -> QUICK_MODE_FOOD_SHARE_PERCENT;
            default -> 0;
        };
    }

    @Override
    public double challengeWeight(ChallengeCadence cadence, ChallengeDifficulty difficulty) {
        if (cadence == ChallengeCadence.DAILY) {
            return DAILY_CHALLENGE_WEIGHT;
        }

        if (difficulty == null) {
            return 0;
        }

        return switch (difficulty) {
            case EASY -> 1.0;
            case NORMAL -> 1.7;
            case MEDIUM -> 2.7;
            case HARD -> 3.9;
            case VERY_HARD -> 5.4;
        };
    }

    @Override
    public int rewardProgressionPercent(int weekIndex) {
        int weeksElapsed = Math.max(0, weekIndex - 1);

        return (int) PERCENT_SCALE + weeksElapsed * REWARD_PROGRESSION_PERCENT_PER_WEEK;
    }

    @Override
    public int challengeSurvivors(int reference, double weight, int weekIndex) {
        double progression = rewardProgressionPercent(weekIndex) / PERCENT_SCALE;

        return (int) Math.round(reference * weight / SURVIVORS_PER_REFERENCE_DIVISOR * progression);
    }

    @Override
    public int challengeRankingPoints(int reference, double weight, int weekIndex) {
        return challengeSurvivors(reference, weight, weekIndex);
    }

    @Override
    public int referenceFloor() {
        return REFERENCE_FLOOR;
    }
}
