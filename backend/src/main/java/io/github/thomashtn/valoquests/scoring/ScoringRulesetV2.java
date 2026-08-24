package io.github.thomashtn.valoquests.scoring;

import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.scoring.model.BossCategory;
import io.github.thomashtn.valoquests.scoring.model.MatchOutcome;
import org.springframework.stereotype.Component;

/**
 * Second version of the barèmes, rebalanced so the ranking rewards playing often rather than playing a
 * lot, and so completing a challenge alongside the squad is worth appreciably more than doing it alone.
 *
 * <p>What changed against {@link ScoringRulesetV1}, and why:
 *
 * <ul>
 *   <li><b>Daily diminishing returns.</b> Match damage used to be linear and uncapped, so the week was
 *       won by whoever had the most free time. Past the fifth match of a day a game is worth half, past
 *       the ninth a quarter. A regular player on fourteen matches across seven days now finishes ahead
 *       of a grinder on thirty-six across three.</li>
 *   <li><b>Regularity doubled.</b> The old ladder topped out at 3 000, roughly seven ranked wins, which
 *       daily volume drowned out. Doubling it is what actually puts assiduity ahead of volume; the
 *       diminishing returns alone were not enough once the curve was softened to start at the sixth
 *       match.</li>
 *   <li><b>Team bonus made proportional.</b> A flat 1 100 on a 9 000 challenge was decoration. It is now
 *       a share of what the challenge is worth, so the squad bonus scales with the stake.</li>
 *   <li><b>Boss hit points per active player.</b> A fixed total made a full roster trivial and a holiday
 *       week mathematically unwinnable. Sizing on the active roster also means an active player who does
 *       not play makes the week harder for everyone, which is the intended shared responsibility.</li>
 *   <li><b>Symmetric difficulty modifier.</b> At +5/−10 the modifier needed two wins out of three just to
 *       hold station, so it drifted to its floor and stopped regulating anything.</li>
 *   <li><b>Carry-over.</b> A boss left at one percent used to score exactly like one never touched.</li>
 * </ul>
 *
 * <p>Match damage values themselves are unchanged, and deliberately restated here rather than inherited:
 * a published ruleset is frozen whole, so each version stands alone.
 */
@Component
public final class ScoringRulesetV2 implements ScoringRuleset {

    /**
     * Version number this ruleset resolves to.
     */
    private static final int VERSION = 2;

    /**
     * Regularity bonus by number of active days, index 0 unused (days are 1-based), index 7 is the max.
     */
    private static final int[] REGULARITY_BONUS_BY_DAYS = {0, 0, 600, 1400, 2400, 3600, 4800, 6000};

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
     * Team bonus granted per player joining a challenge beyond the first, as a share of its damage.
     */
    private static final int TEAM_BONUS_PERCENT_PER_EXTRA_PLAYER = 10;

    /**
     * Number of joining players past which the team bonus stops growing, capping it at 50%.
     */
    private static final int TEAM_BONUS_EXTRA_PLAYER_CAP = 5;

    /**
     * Neutral starting value of the collective difficulty modifier, in percent.
     */
    private static final int INITIAL_MODIFIER_PERCENT = 100;

    /**
     * Symmetric step the collective difficulty modifier moves by, in percent.
     */
    private static final int MODIFIER_STEP = 7;

    /**
     * Lower bound of the collective difficulty modifier, in percent.
     */
    private static final int MINIMUM_MODIFIER_PERCENT = 70;

    /**
     * Upper bound of the collective difficulty modifier, in percent.
     */
    private static final int MAXIMUM_MODIFIER_PERCENT = 130;

    /**
     * Share of a new boss's base hit points that a surviving predecessor's remainder may not exceed.
     */
    private static final int CARRIED_OVER_HP_CAP_PERCENT = 25;

    /**
     * Divisor turning a percentage into a ratio.
     */
    private static final double PERCENT_SCALE = 100.0;

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
     * @param outcome     match outcome
     * @param lossDamage  damage on defeat
     * @param winDamage   damage on victory
     * @return resolved damage
     */
    private int spikeRushOrDeathmatchDamage(MatchOutcome outcome, int lossDamage, int winDamage) {
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

    /**
     * {@inheritDoc}
     *
     * <p>Priced as a share of the challenge's own damage rather than as a flat amount, so joining a hard
     * challenge alongside the squad is worth more than joining an easy one.
     */
    @Override
    public int challengeTeamBonus(ChallengeDifficulty difficulty, int playersWhoCompleted) {
        int extraPlayers = Math.clamp(playersWhoCompleted - 1L, 0, TEAM_BONUS_EXTRA_PLAYER_CAP);
        int bonusPercent = extraPlayers * TEAM_BONUS_PERCENT_PER_EXTRA_PLAYER;

        return (int) Math.round(challengeDamage(difficulty) * bonusPercent / PERCENT_SCALE);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sized on the active roster, not on who actually turned up: a player left active and absent all
     * week adds their share of hit points without dealing any, which is what makes attendance a
     * collective commitment rather than an individual one. Deactivating someone through the backoffice is
     * the supported way to shrink a week.
     */
    @Override
    public int bossBaseHp(BossCategory category, int activePlayerCount) {
        if (category == null) {
            return 0;
        }

        int hpPerPlayer = switch (category) {
            case MINOR -> 13_000;
            case STANDARD -> 16_000;
            case ELITE -> 20_000;
        };

        return hpPerPlayer * Math.max(1, activePlayerCount);
    }

    @Override
    public int nextDifficultyModifierPercent(int previousModifierPercent, boolean previousDefeated) {
        int adjusted = previousDefeated
            ? previousModifierPercent + MODIFIER_STEP
            : previousModifierPercent - MODIFIER_STEP;

        return Math.clamp(adjusted, MINIMUM_MODIFIER_PERCENT, MAXIMUM_MODIFIER_PERCENT);
    }

    @Override
    public int initialDifficultyModifierPercent() {
        return INITIAL_MODIFIER_PERCENT;
    }

    @Override
    public int carriedOverHpCapPercent() {
        return CARRIED_OVER_HP_CAP_PERCENT;
    }
}
