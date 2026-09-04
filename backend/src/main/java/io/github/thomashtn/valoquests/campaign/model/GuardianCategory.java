package io.github.thomashtn.valoquests.campaign.model;

/**
 * Weight class of a guardian, deciding which weeks it may be drawn for.
 *
 * <p>The class is the guardian's, not the week's: a campaign schedules its ten weeks by class
 * (see {@link CampaignSchedule}) and then draws a guardian inside the class the week asks for, so
 * the catalogue can grow without ever changing the shape of a campaign.
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
