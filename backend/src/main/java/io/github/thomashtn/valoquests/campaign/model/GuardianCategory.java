package io.github.thomashtn.valoquests.campaign.model;

/**
 * Weight class of a guardian, deciding which weeks it may be drawn for.
 *
 * <p>{@link CampaignSchedule} sets each week's class, then a guardian of that class is drawn.
 */
public enum GuardianCategory {

    /**
     * The two breather weeks of a campaign.
     */
    MINOR,

    /**
     * The six ordinary weeks.
     */
    STANDARD,

    /**
     * The two peaks, in week five and week ten.
     */
    ELITE
}
