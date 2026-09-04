package io.github.thomashtn.valoquests.campaign.model;

import java.util.List;

/**
 * The ten weeks every campaign is played on, in order.
 *
 * <p>Written down rather than drawn: a campaign is meant to be comparable to every other one, and a
 * schedule that varied would make two runs of the same squad incomparable. The ladder is not
 * monotonic on purpose — week 6 is a breather right after the first peak, week 7 is the hardest
 * guardian-to-group ratio of the run, and week 10 pairs the biggest group with the biggest guardian.
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
