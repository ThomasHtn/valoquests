package io.github.thomashtn.valoquests.campaign;

/**
 * The constants the rescue campaign is played on, in one place.
 *
 * <p>Separate from {@code ScoringRuleset}, which prices what a player does: this prices what the
 * base does with it. The figures were verified by simulation on 04/09/2026 against the invariants in
 * {@code docs/GAMEPLAY.md}; the guardian factor has changed since (0.78 to 1.10 on 09/09/2026), so
 * that simulation is due again. Moving any figure means running it again.
 *
 * <p>A single class rather than an interface and an implementation: nothing here varies, and a
 * second implementation would only ever be a way to write a different game.
 */
public final class CampaignRuleset {

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
    public static final double GUARDIAN_HIT_POINTS_FACTOR = 1.10;

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
     * Not instantiable: constants and static helpers only.
     */
    private CampaignRuleset() {
    }

    /**
     * Returns the food a base of that size eats each evening.
     *
     * @param population inhabitants
     * @return the evening's upkeep
     */
    public static double dailyUpkeep(double population) {
        return population * FOOD_PER_INHABITANT_PER_DAY;
    }

    /**
     * Returns the food an extraction never touches: the next seven evenings of upkeep.
     *
     * @param population inhabitants
     * @return the protected food
     */
    public static double protectedFood(double population) {
        // Kept in this multiplication order: the replay's stored figures depend on its rounding.
        return PROTECTED_FOOD_DAYS * population * FOOD_PER_INHABITANT_PER_DAY;
    }

    /**
     * Returns the hit points of one week's guardian.
     *
     * @param reference     reference the campaign's difficulty carries
     * @param guardianWeight week's guardian weight
     * @param activePlayers players the campaign froze into its roster
     * @return hit points the guardian opens the week with
     */
    public static int guardianHitPoints(int reference, double guardianWeight, int activePlayers) {
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
    public static int woundedCount(int reference, double groupWeight, int activePlayers, int progressionPercent) {
        return (int) Math.round(
            reference * groupWeight * GROUP_SIZE_FACTOR * activePlayers * progressionPercent / 100.0
        );
    }
}
