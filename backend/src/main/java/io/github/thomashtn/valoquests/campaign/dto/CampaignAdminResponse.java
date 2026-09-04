package io.github.thomashtn.valoquests.campaign.dto;

import io.github.thomashtn.valoquests.campaign.model.CampaignStatus;
import io.github.thomashtn.valoquests.campaign.model.CampaignTier;
import java.time.LocalDate;

/**
 * What the backoffice gets back after opening or stopping a campaign.
 *
 * @param number         campaign number
 * @param status         where it now stands
 * @param firstWeekStart Monday it starts on
 * @param lastWeekStart  Monday its tenth week starts on
 * @param stoppedOn      day it was cut short, {@code null} otherwise
 * @param reference      squad's weekly reference per player
 * @param tier           bracket the reference falls in
 * @param rosterSize     operators frozen into it
 */
public record CampaignAdminResponse(
    int number,
    CampaignStatus status,
    LocalDate firstWeekStart,
    LocalDate lastWeekStart,
    LocalDate stoppedOn,
    int reference,
    CampaignTier tier,
    int rosterSize
) {
}
