package io.github.thomashtn.valoquests.campaign.dto;

import io.github.thomashtn.valoquests.ranking.model.WeeklyTitle;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * The day in progress: what the squad has brought in, and who is carrying it.
 *
 * <p>Provisional until midnight: a later match can reprice earlier ones and move the totals.
 *
 * @param day                       calendar day
 * @param damage                    damage the roster has dealt today
 * @param food                      food produced today
 * @param components                components produced today
 * @param presenceCount             players who have played today
 * @param rosterSize                players the campaign froze
 * @param dailyUpkeep               food the base will eat this evening
 * @param rescuesByComponentsGained wounded today's components add to what the ship can reach
 * @param rescuesByFoodGained       wounded today's food adds to what the base can settle
 * @param players                   each player's day, most damage first
 * @param titles                    the week's honours as they stand, ties omitted
 */
@Schema(description = "The day in progress: the squad's gains and who brought them.")
public record CampaignTodayResponse(
    LocalDate day,
    int damage,
    int food,
    int components,
    int presenceCount,
    int rosterSize,
    int dailyUpkeep,
    int rescuesByComponentsGained,
    int rescuesByFoodGained,
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
        return new CampaignTodayResponse(day, 0, 0, 0, 0, 0, 0, 0, 0, List.of(), Map.of());
    }
}
