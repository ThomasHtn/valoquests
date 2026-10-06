package io.github.thomashtn.valoquests.campaign.model;

import java.util.List;

/**
 * The ten weeks every campaign is played on, in order.
 *
 * <p>Fixed rather than drawn so campaigns stay comparable; the ladder is deliberately not monotonic.
 */
public final class CampaignSchedule {

    /**
     * Number of weeks a campaign lasts.
     */
    public static final int WEEK_COUNT = 10;

    /**
     * The ten weeks, week one first.
     */
    private static final List<CampaignWeekShape> WEEKS = List.of(
        new CampaignWeekShape(1, "Orune", GuardianCategory.MINOR, 0.60, 1.00),
        new CampaignWeekShape(2, "Vell", GuardianCategory.STANDARD, 0.80, 1.30),
        new CampaignWeekShape(3, "Tessar", GuardianCategory.STANDARD, 0.95, 0.90),
        new CampaignWeekShape(4, "Hollin", GuardianCategory.STANDARD, 0.85, 1.10),
        new CampaignWeekShape(5, "Keshra", GuardianCategory.ELITE, 1.30, 1.50),
        new CampaignWeekShape(6, "Nyx", GuardianCategory.MINOR, 0.60, 1.20),
        new CampaignWeekShape(7, "Sarrat", GuardianCategory.STANDARD, 1.00, 0.80),
        new CampaignWeekShape(8, "Doune", GuardianCategory.STANDARD, 0.90, 1.10),
        new CampaignWeekShape(9, "Ilvenn", GuardianCategory.STANDARD, 0.95, 1.00),
        new CampaignWeekShape(10, "Maur", GuardianCategory.ELITE, 1.35, 2.00)
    );

    /**
     * Prevents instantiation of this constant holder.
     */
    private CampaignSchedule() {
    }

    /**
     * Returns the ten weeks, week one first.
     *
     * @return the campaign's schedule
     */
    public static List<CampaignWeekShape> weeks() {
        return WEEKS;
    }
}
