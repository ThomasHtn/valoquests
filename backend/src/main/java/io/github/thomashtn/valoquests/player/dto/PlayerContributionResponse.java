package io.github.thomashtn.valoquests.player.dto;

import io.github.thomashtn.valoquests.campaign.model.CampaignStatus;
import io.github.thomashtn.valoquests.ranking.model.WeeklyTitle;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Exposes what one player brings to the squad: their week, and their campaign when there is one.
 *
 * @param playerId internal player identifier
 * @param week     the player's current week, {@code null} before the week's ranking is first built
 * @param campaign the player's campaign in progress, {@code null} when none is live or the player
 *                 is not on its roster
 */
@Schema(description = "One player's contribution to the current week and to the campaign in progress.")
public record PlayerContributionResponse(

    long playerId,
    WeekContributionResponse week,
    CampaignContributionResponse campaign
) {
    /**
     * The player's current week, as the ranking holds it.
     *
     * @param weekStart                Monday identifying the week
     * @param position                 current rank, {@code null} when the player takes no slot
     * @param guardianDamage           damage dealt to the guardian so far
     * @param food                     food share of that damage
     * @param components               components share of that damage
     * @param matchCount               valued matches played so far
     * @param activeDays               distinct days with at least one valued match
     * @param streakDays               longest run of consecutive played days reached this week
     * @param challengePoints          points of the challenges validated so far
     * @param completedChallenges      weekly challenges validated so far
     * @param completedDailyChallenges daily challenges validated so far
     * @param totalPoints              guardian damage plus challenge points
     * @param titles                   honours the player holds as the week stands
     */
    public record WeekContributionResponse(

        LocalDate weekStart,
        Integer position,
        int guardianDamage,
        int food,
        int components,
        int matchCount,
        int activeDays,
        int streakDays,
        int challengePoints,
        int completedChallenges,
        int completedDailyChallenges,
        int totalPoints,
        List<WeeklyTitle> titles
    ) {
        /**
         * Creates an immutable week.
         */
        public WeekContributionResponse {
            titles = List.copyOf(titles);
        }
    }

    /**
     * The player's campaign in progress, from its first day to today.
     *
     * @param campaignId          campaign identifier
     * @param campaignNumber      campaign number
     * @param status              where the campaign stands
     * @param damage              damage dealt to the guardians
     * @param food                food produced
     * @param components          components produced
     * @param matchCount          valued matches played
     * @param activeDays          days with at least one valued match
     * @param longestStreak       longest run of consecutive played days reached
     * @param completedChallenges challenges validated, daily and weekly together
     * @param survivorsRescued    wounded those challenges brought back
     * @param finishingBlows      guardians the player dealt the finishing blow to
     * @param titles              how many times each weekly honour went to the player
     */
    public record CampaignContributionResponse(

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
         * Creates an immutable campaign contribution.
         */
        public CampaignContributionResponse {
            titles = Map.copyOf(titles);
        }
    }
}
