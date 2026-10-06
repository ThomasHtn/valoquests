package io.github.thomashtn.valoquests.campaign.model;

/**
 * Where a campaign stands in its own lifecycle.
 *
 * <p>Three states because a campaign can be opened before its first Monday starts.
 */
public enum CampaignStatus {

    /**
     * Roster and difficulty frozen, waiting for its first Monday. Nothing is played yet.
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
