package io.github.thomashtn.valoquests.campaign.dto;

import io.github.thomashtn.valoquests.campaign.model.WeeklyTitle;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * The day in progress: what the squad has brought in, and who is carrying it.
 *
 * <p>Provisional until midnight, and deliberately so: a match that comes in later can push a
 * cheaper one down a tier and move a total that was already on screen. The day is only final once
 * it is over.
 *
 * @param day             calendar day
 * @param damage          damage the roster has dealt today
 * @param food            food produced today
 * @param components      components produced today
 * @param presenceCount   operators who have played today
 * @param rosterSize      operators the campaign froze
 * @param dailyUpkeep     food the base will eat this evening
 * @param players         each operator's day, most damage first
 * @param titles          the week's honours as they stand, ties omitted
 */
public record CampaignTodayResponse(
    LocalDate day,
    int damage,
    int food,
    int components,
    int presenceCount,
    int rosterSize,
    int dailyUpkeep,
    List<CampaignPlayerDayResponse> players,
    Map<WeeklyTitle, Long> titles
) {

    /**
     * Creates the response, copying both collections.
     */
    public CampaignTodayResponse {
        players = List.copyOf(players);
        titles = Map.copyOf(titles);
    }

    /**
     * Returns the answer given between two campaigns.
     *
     * @param day calendar day
     * @return a day with nothing on it
     */
    public static CampaignTodayResponse none(LocalDate day) {
        return new CampaignTodayResponse(day, 0, 0, 0, 0, 0, 0, List.of(), Map.of());
    }
}
