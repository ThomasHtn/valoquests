package io.github.thomashtn.valoquests.scoring;

import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.scoring.model.BossCategory;
import io.github.thomashtn.valoquests.scoring.model.MatchOutcome;
import org.springframework.stereotype.Component;

/**
 * First version of the weekly-boss damage barèmes.
 *
 * <p>Values are transcribed as-is from the design notes. Once published, this class must never change:
 * a future adjustment creates {@code ScoringRulesetV2} instead, registered alongside this one in
 * {@link ScoringRulesetRegistry}, so that weeks already resolved against version 1 keep recalculating
 * identically forever.
 */
@Component
public final class ScoringRulesetV1 implements ScoringRuleset {

    /**
     * Version number this ruleset resolves to.
     */
    private static final int VERSION = 1;

    /**
     * Regularity bonus by number of active days, index 0 unused (days are 1-based), index 7 is the max.
     */
    private static final int[] REGULARITY_BONUS_BY_DAYS = {0, 0, 300, 700, 1200, 1800, 2400, 3000};

    /**
     * Per-player team bonus by number of players who completed the challenge, index 0 unused.
     *
     * <p>The design notes only tabulate up to 6 players. The fixed roster can in principle field a 7th,
     * so any count at or beyond the table's last tier is capped to that tier rather than extrapolated.
     */
    private static final int[] TEAM_BONUS_BY_PLAYER_COUNT = {0, 0, 150, 300, 500, 750, 1100};

    /**
     * Neutral starting value of the collective difficulty modifier, in percent.
     */
    private static final int INITIAL_MODIFIER_PERCENT = 100;

    /**
     * Modifier increase applied after the boss is defeated.
     */
    private static final int MODIFIER_INCREASE_ON_VICTORY = 5;

    /**
     * Modifier decrease applied after the boss survives.
     */
    private static final int MODIFIER_DECREASE_ON_SURVIVAL = 10;

    /**
     * Lower bound of the collective difficulty modifier, in percent.
     */
    private static final int MINIMUM_MODIFIER_PERCENT = 70;

    /**
     * Upper bound of the collective difficulty modifier, in percent.
     */
    private static final int MAXIMUM_MODIFIER_PERCENT = 130;

    /**
     * Percentage of its base damage every match keeps, whatever its rank within its day.
     *
     * <p>Version 1 values daily volume linearly: it has no diminishing returns, so this is flat.
     */
    private static final int FLAT_MATCH_DAMAGE_COEFFICIENT_PERCENT = 100;

    @Override
    public int version() {
        return VERSION;
    }

    @Override
    public int matchDamage(GameMode gameMode, MatchOutcome outcome) {
        if (gameMode == null || outcome == null) {
            return 0;
        }

        return switch (gameMode) {
            case COMPETITIVE -> switch (outcome) {
                case LOSS -> 350;
                case DRAW -> 425;
                case WIN -> 500;
            };
            case UNRATED -> switch (outcome) {
                case LOSS -> 280;
                case DRAW -> 340;
                case WIN -> 400;
            };
            case SPIKE_RUSH -> spikeRushOrDeathmatchDamage(outcome, 130, 180);
            case SKIRMISH -> switch (outcome) {
                case LOSS -> 120;
                case DRAW -> 145;
                case WIN -> 170;
            };
            case TEAM_DEATHMATCH -> switch (outcome) {
                case LOSS -> 110;
                case DRAW -> 135;
                case WIN -> 160;
            };
            case DEATHMATCH -> spikeRushOrDeathmatchDamage(outcome, 100, 150);
            default -> 0;
        };
    }

    /**
     * Resolves damage for the two modes that cannot end on a draw.
     *
     * <p>Henrik is not expected to ever report {@link MatchOutcome#DRAW} for these modes, but a draw is
     * folded into the defeat tier rather than rejected, so a future upstream surprise degrades quietly
     * instead of breaking the weekly calculation.
     *
     * @param outcome     match outcome
     * @param lossDamage  damage on defeat
     * @param winDamage   damage on victory
     * @return resolved damage
     */
    private int spikeRushOrDeathmatchDamage(MatchOutcome outcome, int lossDamage, int winDamage) {
        return outcome == MatchOutcome.WIN ? winDamage : lossDamage;
    }

    @Override
    public int challengeDamage(ChallengeDifficulty difficulty) {
        if (difficulty == null) {
            return 0;
        }

        return switch (difficulty) {
            case EASY -> 1_500;
            case NORMAL -> 2_500;
            case MEDIUM -> 4_000;
            case HARD -> 6_000;
            case VERY_HARD -> 9_000;
        };
    }

    @Override
    public int regularityBonus(int activeDays) {
        int clampedDays = Math.clamp(activeDays, 0, REGULARITY_BONUS_BY_DAYS.length - 1);
        return REGULARITY_BONUS_BY_DAYS[clampedDays];
    }

    @Override
    public int matchDamageCoefficientPercent(int rankInDay) {
        return FLAT_MATCH_DAMAGE_COEFFICIENT_PERCENT;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Version 1 prices the bonus as a flat amount per tier, identical for every difficulty.
     */
    @Override
    public int challengeTeamBonus(ChallengeDifficulty difficulty, int playersWhoCompleted) {
        int clampedCount = Math.clamp(playersWhoCompleted, 0, TEAM_BONUS_BY_PLAYER_COUNT.length - 1);
        return TEAM_BONUS_BY_PLAYER_COUNT[clampedCount];
    }

    /**
     * {@inheritDoc}
     *
     * <p>Version 1 sizes a boss as a fixed total, independently of how many players the roster holds
     * active, so the count is ignored here.
     */
    @Override
    public int bossBaseHp(BossCategory category, int activePlayerCount) {
        if (category == null) {
            return 0;
        }

        return switch (category) {
            case MINOR -> 80_000;
            case STANDARD -> 95_000;
            case ELITE -> 115_000;
        };
    }

    @Override
    public int nextDifficultyModifierPercent(int previousModifierPercent, boolean previousDefeated) {
        int adjusted = previousDefeated
            ? previousModifierPercent + MODIFIER_INCREASE_ON_VICTORY
            : previousModifierPercent - MODIFIER_DECREASE_ON_SURVIVAL;

        return Math.clamp(adjusted, MINIMUM_MODIFIER_PERCENT, MAXIMUM_MODIFIER_PERCENT);
    }

    @Override
    public int initialDifficultyModifierPercent() {
        return INITIAL_MODIFIER_PERCENT;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Version 1 has no carry-over: a surviving boss leaves nothing behind.
     */
    @Override
    public int carriedOverHpCapPercent() {
        return 0;
    }
}
