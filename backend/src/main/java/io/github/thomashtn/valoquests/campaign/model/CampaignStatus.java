package io.github.thomashtn.valoquests.campaign.model;

/**
 * Where a campaign stands in its own lifecycle.
 *
 * <p>Three states rather than an open/closed flag, because a campaign exists before it counts: the
 * backoffice opens it any day of the week, and it starts the Monday after. That gap is when the
 * roster is frozen and the calibration is read, and it needs a name of its own so the site can say
 * "opened, starting Monday" instead of showing a base nobody has played for yet.
 */
public enum CampaignStatus {

    /**
     * Calibrated and scheduled, waiting for its first Monday. Nothing is played yet.
     */
    OPENED,

    /**
     * Under way. The only status the replay ever writes to.
     */
    RUNNING,

    /**
     * Over, settled one last time and frozen. Its final base is its score, forever.
     */
    CLOSED
}
