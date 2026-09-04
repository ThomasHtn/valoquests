package io.github.thomashtn.valoquests.campaign;

import org.springframework.stereotype.Component;

/**
 * The constants the rescue campaign is played on, in one place.
 *
 * <p>Separate from {@code ScoringRuleset}, which prices what a player does: this prices what the
 * base does with it. Every figure here was verified by simulation on 04/09/2026 against the
 * invariants in {@code docs/GAMEPLAY.md} — a squad calibrated on itself beats eight guardians out of
 * ten, effort pays, and the result per player is the same at two players as at twenty. Moving one of
 * them means running that simulation again.
 *
 * <p>A single class rather than an interface and an implementation: nothing here varies, and a
 * second implementation would only ever be a way to write a different game.
 */
@Component
public class CampaignRuleset {

    /**
     * Damage one new inhabitant costs.
     *
     * <p>The only source of daily growth, and deliberately blind to the mode played: no mode may be
     * a bad choice for the campaign's score.
     */
    public static final double DAMAGE_PER_INHABITANT = 28;

    /**
     * Food one inhabitant eats each evening.
     *
     * <p>Small enough that upkeep goes from 0.7 % of a week's food in week one to 11 % in week ten:
     * a big base costs more attention than a camp without ever becoming the subject.
     */
    public static final double FOOD_PER_INHABITANT_PER_DAY = 0.008;

    /**
     * Share of the unfed who die on an evening the larder is empty.
     */
    public static final double FAMINE_LOSS_RATE = 0.05;

    /**
     * Evenings of food the ship never touches when it extracts.
     *
     * <p>Without it a squad that plays at the weekend emptied its larder on Sunday and starved from
     * Monday to Friday, which punished exactly the rhythm the game is played at.
     */
    public static final int PROTECTED_FOOD_DAYS = 7;

    /**
     * Guardian hit points per point of reference, per active player.
     */
    public static final double GUARDIAN_HIT_POINTS_FACTOR = 0.78;

    /**
     * Wounded stranded per point of reference, per active player, before the weekly progression.
     */
    public static final double GROUP_SIZE_FACTOR = 0.050;

    /**
     * Components spent to reach one wounded.
     */
    public static final int COMPONENTS_PER_RESCUE = 14;

    /**
     * Food spent to settle one wounded.
     */
    public static final int FOOD_PER_RESCUE = 12;

    /**
     * Share of the base a guardian left completely untouched would kill.
     *
     * <p>Applied against the square of what is left to do, so missing by a hair costs almost
     * nothing and doing nothing costs a third of the base. There is no threshold anywhere in it.
     */
    public static final double GUARDIAN_LOSS_RATE = 0.35;

    /**
     * Reference the challenge catalogue's base targets are written at.
     */
    public static final int CALIBRATION_ANCHOR_REFERENCE = 5_300;

    /**
     * Months of history a calibration reads, before any reduction.
     */
    public static final int CALIBRATION_WINDOW_MONTHS = 9;

    /**
     * Months of history under which a player is a beginner and takes the squad's median.
     */
    public static final int BEGINNER_HISTORY_MONTHS = 1;

    /**
     * Returns the hit points of one week's guardian.
     *
     * @param reference     squad's weekly reference per player
     * @param guardianWeight week's guardian weight
     * @param activePlayers players the campaign froze into its roster
     * @return hit points the guardian opens the week with
     */
    public int guardianHitPoints(int reference, double guardianWeight, int activePlayers) {
        return (int) Math.round(reference * guardianWeight * GUARDIAN_HIT_POINTS_FACTOR * activePlayers);
    }

    /**
     * Returns the number of wounded stranded on one week's planet.
     *
     * @param reference          squad's weekly reference per player
     * @param groupWeight        week's group weight
     * @param activePlayers      players the campaign froze into its roster
     * @param progressionPercent reward progression of the week, as a percentage
     * @return wounded to evacuate that week
     */
    public int groupSize(int reference, double groupWeight, int activePlayers, int progressionPercent) {
        return (int) Math.round(
            reference * groupWeight * GROUP_SIZE_FACTOR * activePlayers * progressionPercent / 100.0
        );
    }
}
