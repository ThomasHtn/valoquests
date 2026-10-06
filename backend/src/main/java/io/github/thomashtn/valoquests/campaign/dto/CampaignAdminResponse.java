package io.github.thomashtn.valoquests.campaign.dto;

import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.model.CampaignStatus;
import io.github.thomashtn.valoquests.scoring.model.CampaignDifficulty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * What the backoffice gets back after opening or stopping a campaign.
 *
 * @param id             campaign identifier
 * @param number         campaign number
 * @param status         where it now stands
 * @param firstWeekStart Monday it starts on
 * @param lastWeekStart  Monday its tenth week starts on
 * @param stoppedOn      day it was cut short, {@code null} otherwise
 * @param reference      squad's weekly reference per player
 * @param difficulty     difficulty the campaign is played at
 * @param rosterSize     players frozen into it
 */
@Schema(description = "Campaign state returned after an admin lifecycle action.")
public record CampaignAdminResponse(
    long id,
    int number,
    CampaignStatus status,
    LocalDate firstWeekStart,
    LocalDate lastWeekStart,
    LocalDate stoppedOn,
    int reference,
    CampaignDifficulty difficulty,
    int rosterSize
) {

    /**
     * Maps one campaign to the backoffice's answer.
     *
     * @param campaign campaign to map
     * @return the response
     */
    public static CampaignAdminResponse from(Campaign campaign) {
        return new CampaignAdminResponse(
            campaign.getId(),
            campaign.getNumber(),
            campaign.getStatus(),
            campaign.getFirstWeekStart(),
            campaign.getLastWeekStart(),
            campaign.getStoppedOn(),
            campaign.reference(),
            campaign.getDifficulty(),
            campaign.getRosterSize()
        );
    }
}
