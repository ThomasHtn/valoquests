package io.github.thomashtn.valoquests.campaign.dto;

import io.github.thomashtn.valoquests.challenge.model.CampaignDifficulty;
import java.time.LocalDate;
import java.util.List;

/**
 * One closed campaign, as the history table reads it.
 *
 * <p>The difficulty is what makes two of these comparable: a base of 30 000 at Amateur and one of
 * 60 000 at Pro describe the same ten weeks played at two settings.
 *
 * @param id                campaign identifier
 * @param number            campaign number
 * @param difficulty        difficulty the campaign was played at
 * @param reference         squad's weekly reference per player
 * @param rosterSize        operators frozen into it
 * @param firstWeekStart    Monday it started on
 * @param lastWeekStart     Monday its tenth week started on
 * @param stoppedOn         day an operator cut it short, {@code null} when it ran its course
 * @param guardiansDefeated guardians that fell
 * @param population        base it finished at, which is its score
 * @param rescued           wounded it brought home
 * @param weeklyPopulation  base at the close of each settled week, week one first
 */
public record CampaignHistoryResponse(
    long id,
    int number,
    CampaignDifficulty difficulty,
    int reference,
    int rosterSize,
    LocalDate firstWeekStart,
    LocalDate lastWeekStart,
    LocalDate stoppedOn,
    int guardiansDefeated,
    int population,
    int rescued,
    List<Integer> weeklyPopulation
) {

    /**
     * Creates the response, copying the curve.
     */
    public CampaignHistoryResponse {
        weeklyPopulation = List.copyOf(weeklyPopulation);
    }
}
