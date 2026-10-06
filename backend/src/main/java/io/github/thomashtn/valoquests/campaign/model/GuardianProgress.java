package io.github.thomashtn.valoquests.campaign.model;

/**
 * How far the squad got on a week's guardian.
 *
 * <p>Shared by the Sunday settlement and every reading, so the forecast matches the settlement.
 */
public final class GuardianProgress {

    /**
     * Not instantiable: static helper only.
     */
    private GuardianProgress() {
    }

    /**
     * Returns how far the squad got on the guardian, as a share of its hit points.
     *
     * <p>One for a guardian that fell, whatever the overkill, and for a guardian without hit points.
     *
     * @param defeated          whether the guardian fell
     * @param damageDealt       damage the roster dealt over the week
     * @param guardianHitPoints hit points the guardian opened the week with
     * @return progress between zero and one
     */
    public static double of(boolean defeated, int damageDealt, int guardianHitPoints) {
        if (defeated || guardianHitPoints <= 0) {
            return 1;
        }

        return Math.min(1, (double) damageDealt / guardianHitPoints);
    }
}
