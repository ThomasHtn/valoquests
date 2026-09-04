package io.github.thomashtn.valoquests.campaign.model;

import java.util.List;

/**
 * Everything one replay produced: the campaign's days and the settlement of each closed week.
 *
 * @param days        every day computed, oldest first
 * @param settlements every week whose Sunday the replay reached, week one first
 */
public record CampaignReplayResult(List<CampaignDayState> days, List<CampaignWeekSettlement> settlements) {

    /**
     * Creates a result, copying both lists.
     */
    public CampaignReplayResult {
        days = List.copyOf(days);
        settlements = List.copyOf(settlements);
    }

    /**
     * Returns the base's size at the end of the last computed day.
     *
     * @return the population the campaign stands at, zero before its first day
     */
    public double population() {
        return days.isEmpty() ? 0 : days.getLast().population();
    }
}
