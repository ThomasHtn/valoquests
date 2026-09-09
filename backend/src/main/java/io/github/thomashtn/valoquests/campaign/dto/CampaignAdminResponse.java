package io.github.thomashtn.valoquests.campaign.dto;

import io.github.thomashtn.valoquests.campaign.model.CampaignStatus;
import io.github.thomashtn.valoquests.challenge.model.CampaignDifficulty;
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
 * @param rosterSize     operators frozen into it
 */
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
}
