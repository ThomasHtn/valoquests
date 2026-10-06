package io.github.thomashtn.valoquests.campaign.service;

import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.exception.CampaignLifecycleException;
import io.github.thomashtn.valoquests.campaign.model.CampaignStartWeek;
import io.github.thomashtn.valoquests.campaign.model.CampaignStatus;
import io.github.thomashtn.valoquests.scoring.model.CampaignDifficulty;
import io.github.thomashtn.valoquests.shared.exception.ResourceNotFoundException;
import java.util.Optional;

/**
 * Opens, starts, stops, closes and deletes campaigns.
 *
 * <p>Only an admin opens a campaign; between two campaigns only the weekly ranking keeps turning.
 */
public interface CampaignLifecycleService {

    /**
     * Opens a campaign on the chosen Monday, freezing its roster and its difficulty.
     *
     * @param difficulty difficulty the campaign is played at
     * @param startWeek  week the campaign starts on
     * @return the campaign, {@link CampaignStatus#RUNNING} when its first Monday is today or already past
     * @throws CampaignLifecycleException when a campaign is already live or no player is active
     */
    Campaign open(CampaignDifficulty difficulty, CampaignStartWeek startWeek);

    /**
     * Starts the opened campaign once its first Monday has come; idempotent.
     *
     * @return the campaign that just started, empty when none was waiting
     */
    Optional<Campaign> startIfDue();

    /**
     * Closes the running campaign once its tenth Sunday has been settled; idempotent.
     *
     * @return the campaign that just closed, empty when none was due
     */
    Optional<Campaign> closeIfComplete();

    /**
     * Stops the live campaign now, freezing it at yesterday's base.
     *
     * @return the campaign that was stopped
     * @throws CampaignLifecycleException when no campaign is live
     */
    Campaign stop();

    /**
     * Deletes one campaign and everything it owns.
     *
     * @param id campaign identifier
     * @throws ResourceNotFoundException when no campaign owns the identifier
     */
    void delete(long id);
}
