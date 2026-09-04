package io.github.thomashtn.valoquests.campaign.service;

import io.github.thomashtn.valoquests.campaign.dto.CampaignHistoryResponse;
import io.github.thomashtn.valoquests.campaign.dto.CampaignResponse;
import io.github.thomashtn.valoquests.campaign.dto.CampaignTodayResponse;
import java.util.List;

/**
 * Reads the campaign for the public site.
 *
 * <p>Read-only throughout: nothing here opens, replays or settles anything. A page view must never
 * be able to move a base.
 */
public interface CampaignQueryService {

    /**
     * Returns the campaign in force, or the last closed one, or nothing.
     *
     * @return the campaign, always answering even when there is none
     */
    CampaignResponse currentCampaign();

    /**
     * Returns the day in progress.
     *
     * @return today, empty of operators between two campaigns
     */
    CampaignTodayResponse today();

    /**
     * Returns the closed campaigns, most recent first.
     *
     * @return the campaign history
     */
    List<CampaignHistoryResponse> history();
}
