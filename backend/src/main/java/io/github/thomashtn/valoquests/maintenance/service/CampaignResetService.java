package io.github.thomashtn.valoquests.maintenance.service;

/**
 * Wipes every piece of data derived from match history, so a new campaign starts empty.
 *
 * <p>Keeps the player roster, the challenge catalogue and the guardian catalogue.
 */
public interface CampaignResetService {

    /**
     * Empties every derived table, campaigns included, and clears each player's synchronization watermark.
     *
     * <p>Must run under the {@code MatchHistoryLock} so nothing writes into the tables being emptied.
     */
    void resetCampaign();
}
