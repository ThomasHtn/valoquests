package io.github.thomashtn.valoquests.maintenance.service;

/**
 * Wipes every piece of data derived from match history, so a new campaign starts from a coherent
 * empty state.
 *
 * <p>Kept: the player roster, the challenge catalogue and the guardian catalogue, none of which is
 * derived from anything.
 */
public interface CampaignResetService {

    /**
     * Empties every derived table, campaigns included, and rewinds each player's synchronization
     * watermark.
     *
     * <p>Must run under the {@code MatchHistoryLock} so no synchronization or rollover writes into
     * the tables being emptied.
     */
    void resetCampaign();
}
