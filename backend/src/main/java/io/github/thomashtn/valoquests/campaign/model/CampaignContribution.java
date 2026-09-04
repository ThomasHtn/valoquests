package io.github.thomashtn.valoquests.campaign.model;

import io.github.thomashtn.valoquests.ranking.model.WeeklyTitle;
import java.util.Map;

/**
 * What one operator has brought to the campaign in progress, from its first day to today.
 *
 * @param campaignId          campaign identifier
 * @param campaignNumber      campaign number
 * @param status              where the campaign stands
 * @param damage              damage dealt to the guardians, both multipliers applied
 * @param food                food produced
 * @param components          components produced
 * @param matchCount          valued matches played
 * @param activeDays          days with at least one valued match
 * @param longestStreak       longest run of consecutive played days reached
 * @param completedChallenges challenges validated, daily and weekly together
 * @param survivorsRescued    wounded those challenges brought back
 * @param finishingBlows      guardians this operator dealt the finishing blow to
 * @param titles              how many times each weekly honour went to this operator
 */
public record CampaignContribution(
    long campaignId,
    int campaignNumber,
    CampaignStatus status,
    int damage,
    int food,
    int components,
    int matchCount,
    int activeDays,
    int longestStreak,
    int completedChallenges,
    int survivorsRescued,
    int finishingBlows,
    Map<WeeklyTitle, Integer> titles
) {

    /**
     * Creates an immutable contribution.
     */
    public CampaignContribution {
        titles = Map.copyOf(titles);
    }
}
