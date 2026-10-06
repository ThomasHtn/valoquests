package io.github.thomashtn.valoquests.campaign.model;

import java.util.List;
import java.util.Map;

/**
 * Everything one replay of a campaign reads, gathered before a single figure is computed.
 *
 * <p>The replay is a pure function of these inputs: the same inputs always give the same base.
 *
 * @param days       every day of the campaign so far, oldest first, days nobody played included
 * @param weeks      the weeks whose Sunday has been reached, week one first
 * @param fights     how each started week's guardian fight stands, by one-based week index
 * @param yields     what each week's challenges brought back, by one-based week index
 * @param playerDays what each roster player produced on each day
 */
public record CampaignReplayInputs(
    List<CampaignDayInput> days,
    List<CampaignWeekInput> weeks,
    Map<Integer, GuardianFight> fights,
    Map<Integer, WeekChallengeYield> yields,
    List<CampaignPlayerDayInput> playerDays
) {

    /**
     * Creates immutable inputs.
     */
    public CampaignReplayInputs {
        days = List.copyOf(days);
        weeks = List.copyOf(weeks);
        fights = Map.copyOf(fights);
        yields = Map.copyOf(yields);
        playerDays = List.copyOf(playerDays);
    }
}
