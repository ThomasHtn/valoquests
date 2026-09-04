package io.github.thomashtn.valoquests.campaign.dto;

import io.github.thomashtn.valoquests.campaign.model.CampaignStatus;
import io.github.thomashtn.valoquests.campaign.model.CampaignTier;
import java.time.LocalDate;
import java.util.List;

/**
 * The campaign in force, whatever state it is in.
 *
 * <p>Answers even when there is none: {@code status} is null and every other field is empty, which
 * is what lets the site say "no campaign is running" from the same call rather than from a 404 it
 * would have to treat as a state.
 *
 * @param status           where the campaign stands, {@code null} when there is none
 * @param number           campaign number
 * @param tier             bracket the reference falls in
 * @param reference        squad's weekly reference per player
 * @param rosterSize       operators frozen into the campaign
 * @param firstWeekStart   Monday the campaign starts on
 * @param lastWeekStart    Monday the campaign's tenth week starts on
 * @param today            calendar day the answer was computed on
 * @param currentWeekIndex one-based week in progress, {@code null} before the campaign starts
 * @param base             the base as it stands
 * @param forecast         what Sunday would bring home from the base as it stands, {@code null}
 *                         outside a week in progress
 * @param weeks            the ten weeks, week one first
 * @param totals           what the campaign has amounted to so far
 */
public record CampaignResponse(
    CampaignStatus status,
    Integer number,
    CampaignTier tier,
    Integer reference,
    Integer rosterSize,
    LocalDate firstWeekStart,
    LocalDate lastWeekStart,
    LocalDate today,
    Integer currentWeekIndex,
    CampaignBaseResponse base,
    CampaignForecastResponse forecast,
    List<CampaignWeekResponse> weeks,
    CampaignTotalsResponse totals
) {

    /**
     * Creates the response, copying the week list.
     */
    public CampaignResponse {
        weeks = List.copyOf(weeks);
    }

    /**
     * Returns the answer given between two campaigns.
     *
     * @param today calendar day the answer was computed on
     * @return a response saying nothing is running
     */
    public static CampaignResponse none(LocalDate today) {
        return new CampaignResponse(
            null, null, null, null, null, null, null, today, null, null, null, List.of(), null
        );
    }
}
