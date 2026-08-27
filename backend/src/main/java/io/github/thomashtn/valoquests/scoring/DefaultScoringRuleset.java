package io.github.thomashtn.valoquests.scoring;

import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.MatchOutcome;
import io.github.thomashtn.valoquests.scoring.model.BossCategory;
import org.springframework.stereotype.Component;

/**
 * The damage barèmes in force.
 *
 * <p>Tuned so the ranking rewards playing often rather than playing a lot, and so completing a
 * challenge alongside the squad is worth appreciably more than doing it alone:
 *
 * <ul>
 *   <li><b>Daily diminishing returns.</b> Past the fifth match of a day a game is worth half, past
 *       the ninth a quarter, so the week is not won by whoever had the most free time.</li>
 *   <li><b>Team bonus proportional to the stake.</b> Each player joining a challenge beyond the first
 *       adds ten percent of its damage for everyone who completed it, up to fifty.</li>
 *   <li><b>A boss sized on measured output.</b> Hit points are a share of what the roster has recently
 *       been producing, per active player, so the fight follows the group instead of a constant that
 *       makes a full roster trivial and a holiday week unwinnable.</li>
 * </ul>
 *
 * <p>No collective difficulty modifier and no carried-over hit points: measuring the reference already
 * moves the bar in the direction those did, and keeping them would have moved it twice for one event.
 */
@Component
public final class DefaultScoringRuleset implements ScoringRuleset {

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
     * Per-player weekly output a fight is sized against until enough weeks have been closed to measure
     * the real one.
     */
    private static final int SEED_REFERENCE_DAMAGE_PER_PLAYER = 10_000;

    /**
     * Number of recently finalized weeks the measured reference is drawn from.
     *
     * <p>Four: long enough that a single holiday or marathon week does not set the bar, short enough
     * that the fight follows the roster's current habits rather than its spring form.
     */
    private static final int CALIBRATION_WINDOW_WEEKS = 4;

    /**
     * Lower bound of the measured reference, as a percentage of the seed.
     */
    private static final int CALIBRATION_FLOOR_PERCENT = 50;

    /**
     * Upper bound of the measured reference, as a percentage of the seed.
     */
    private static final int CALIBRATION_CEILING_PERCENT = 250;

    /**
     * Divisor turning a percentage into a ratio.
     */
    private static final double PERCENT_SCALE = 100.0;

    /**
     * Weight class scheduled for each week of a run, index 0 being its first week.
     *
     * <p>Two peaks, at the halfway mark and on the closing week, each followed by a breather — the
     * first week counting as the breather after the previous run's closing elite. Everything between a
     * breather and a peak is standard, so the run reads as a slope rather than as a flat line with two
     * spikes in it.
     */
    private static final BossCategory[] BOSS_CATEGORY_BY_RUN_WEEK = {
        BossCategory.MINOR,
        BossCategory.STANDARD,
        BossCategory.STANDARD,
        BossCategory.STANDARD,
        BossCategory.ELITE,
        BossCategory.MINOR,
        BossCategory.STANDARD,
        BossCategory.STANDARD,
        BossCategory.STANDARD,
        BossCategory.ELITE,
    };

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
    public int matchDamageCoefficientPercent(int rankInDay) {
        if (rankInDay <= FULL_DAMAGE_RANK_LIMIT) {
            return FULL_DAMAGE_PERCENT;
        }

        if (rankInDay <= HALF_DAMAGE_RANK_LIMIT) {
            return HALF_DAMAGE_PERCENT;
        }

        return REDUCED_DAMAGE_PERCENT;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sized against what a week of play is worth, not above it. A full pack is 12 100, which a squad
     * bonus can lift to 18 150; a regular player's fourteen competitive matches spread over the week come
     * to roughly 6 000. The pack used to be 23 000, close to four times a week of playing, so the ranking
     * was decided almost entirely by challenge completion — and since most of the catalogue's hard tiers
     * ask for raw volume, the largest single reward in the system went to whoever played the most. That
     * is the behaviour the daily diminishing returns exist to suppress.
     */
    @Override
    public int challengeDamage(ChallengeDifficulty difficulty) {
        if (difficulty == null) {
            return 0;
        }

        return switch (difficulty) {
            case EASY -> 800;
            case NORMAL -> 1_400;
            case MEDIUM -> 2_200;
            case HARD -> 3_200;
            case VERY_HARD -> 4_500;
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
     *
     * <p>Expressed as a share of what a player is currently observed to contribute, not as a fixed
     * number of hit points. The reference itself is measured from finalized weeks, so the fight follows
     * the group as it gets better, busier or quieter, and nobody has to re-tune a constant when it does.
     *
     * <p><b>All three weights sit below or barely above that reference, and that margin is the point.</b>
     * The reference is the squad's <em>own</em> recent median, so a weight of one asks it to repeat
     * itself exactly: a win by zero margin, which ordinary week-to-week noise decides instead of effort.
     * Measured over a full run, a squad whose volume wobbles by a tenth went from seven fights won to
     * four on that setting, and an elite boss a quarter above the median was unwinnable by construction
     * for any squad that was merely regular. At eighty-five percent a standard boss leaves room for one
     * slow week; at a hundred and five an elite one asks the squad to actually push; a minor one is what
     * a squad that turned up has already done.
     *
     * <p>The margin is also what keeps the fight neutral to roster size. A player's contribution
     * includes the challenge team bonus, which caps at five extra players, so a player in a pair deals
     * about a fifth less than a player in a group of six while carrying the same share of hit points.
     * On the old weights that gap landed squarely on the line between winning and losing; below the
     * reference it decides nothing, and a squad of two scores what a squad of twenty does.
     *
     * <p>This is the whole of the fight's difficulty. There is no modifier layered on top and nothing
     * carried over from the previous boss: measuring the reference already moves the bar in the same
     * direction those did, and keeping them would have moved it twice for the same event.
     */
    @Override
    public int bossHitPoints(
        BossCategory category,
        int activePlayerCount,
        int referenceDamagePerPlayer
    ) {
        if (category == null) {
            return 0;
        }

        int categoryWeightPercent = switch (category) {
            case MINOR -> 65;
            case STANDARD -> 85;
            case ELITE -> 105;
        };

        long hpPerPlayer = Math.round(
            referenceDamagePerPlayer * categoryWeightPercent / PERCENT_SCALE
        );

        return (int) (hpPerPlayer * Math.max(1, activePlayerCount));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Fixed per week rather than drawn with the boss. A random weight class made a run's shape a
     * matter of luck: three elites in a row was as likely as none at all, and neither reads as a
     * campaign. Scheduling the class instead lets the colony's whole economy be known a run ahead,
     * while the draw still decides which of that class's bosses actually shows up.
     *
     * <p>The ladder is deliberately neutral on volume. Over a run it pays 2 minor, 6 standard and 2
     * elite fights, whose materials and morale come to exactly what ten uniformly drawn fights averaged
     * before, so no colony threshold had to move for it.
     */
    @Override
    public BossCategory bossCategoryForRunWeek(int weekIndexInRun) {
        int ladderIndex = Math.clamp(weekIndexInRun - 1L, 0, BOSS_CATEGORY_BY_RUN_WEEK.length - 1);

        return BOSS_CATEGORY_BY_RUN_WEEK[ladderIndex];
    }

    /**
     * {@inheritDoc}
     *
     * <p>What a player having a real week contributes to the fight: one competitive match a day, and
     * the squad converging on the easy and normal challenges. The regularity bonus is excluded because
     * it never reaches the boss.
     */
    @Override
    public int seedReferenceDamagePerPlayer() {
        return SEED_REFERENCE_DAMAGE_PER_PLAYER;
    }

    @Override
    public int calibrationWindowWeeks() {
        return CALIBRATION_WINDOW_WEEKS;
    }

    @Override
    public int calibrationFloorPercent() {
        return CALIBRATION_FLOOR_PERCENT;
    }

    @Override
    public int calibrationCeilingPercent() {
        return CALIBRATION_CEILING_PERCENT;
    }

}
