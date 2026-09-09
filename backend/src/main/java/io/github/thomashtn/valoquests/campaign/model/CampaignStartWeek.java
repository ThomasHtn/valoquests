package io.github.thomashtn.valoquests.campaign.model;

/**
 * Week a campaign is asked to start on.
 *
 * <p>The choice is made once, at opening, and decides the ten Mondays the campaign is built around.
 */
public enum CampaignStartWeek {

    /**
     * The Monday of the week in progress, so the days already played count from the start.
     *
     * <p>Retroactive: the campaign is running the moment it is opened and its first days are
     * rebuilt from the matches already imported.
     */
    CURRENT_WEEK,

    /**
     * The next Monday, so the campaign begins on a week nobody has played yet.
     */
    NEXT_WEEK
}
