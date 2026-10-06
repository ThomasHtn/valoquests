package io.github.thomashtn.valoquests.ranking.dto;

import io.github.thomashtn.valoquests.player.model.CompetitiveTier;
import io.github.thomashtn.valoquests.ranking.model.WeeklyTitle;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * Exposes the current weekly ranking and the week's honours as they stand.
 *
 * @param weekStart Monday identifying the week
 * @param weekEnd   Sunday closing the week
 * @param today     current day of the week
 * @param ranking   one entry per tracked player, archived ones aside, best week first
 */
@Schema(description = "Current weekly player ranking.")
public record CurrentRankingResponse(

    LocalDate weekStart,
    LocalDate weekEnd,
    LocalDate today,
    List<RankingEntryResponse> ranking
) {
    /**
     * Creates an immutable current-ranking response.
     */
    public CurrentRankingResponse {
        ranking = List.copyOf(ranking);
    }

    /**
     * Exposes one player's live standing in the current week.
     *
     * <p>An inactive player is listed with their validation counts, and nothing else: they measure
     * themselves against the squad without adding to it or taking a slot.
     *
     * @param position                 current rank, starting at 1, shared on equal points,
     *     {@code null} when the player has no points yet or is not competitive
     * @param competing                whether the player takes part in the ranking at all
     * @param positionVariation        places gained since the previous rebuild, negative when lost
     * @param player                   identity shown next to the rank
     * @param guardianDamage           damage dealt to the guardian so far this week
     * @param food                     food share of that damage
     * @param components               components share of that damage
     * @param matchCount               valued matches played so far this week
     * @param playedDays               days played this week
     * @param challengePoints          points of the challenges validated so far
     * @param completedChallenges      weekly challenges validated so far
     * @param totalChallenges          weekly challenges selected for the week
     * @param completedDailyChallenges daily challenges validated so far this week
     * @param totalPoints              guardian damage plus challenge points: the ranking key
     * @param titles                   honours the player holds as the week stands
     */
    public record RankingEntryResponse(

        Integer position,
        boolean competing,
        int positionVariation,
        PlayerRankingResponse player,
        int guardianDamage,
        int food,
        int components,
        int matchCount,
        int playedDays,
        int challengePoints,
        int completedChallenges,
        int totalChallenges,
        int completedDailyChallenges,
        int totalPoints,
        List<WeeklyTitle> titles
    ) {
        /**
         * Creates an immutable ranking entry.
         */
        public RankingEntryResponse {
            titles = List.copyOf(titles);
        }
    }

    /**
     * Exposes the player identity displayed alongside a rank.
     *
     * @param id              internal player identifier
     * @param displayName     player name shown in the ranking
     * @param portrait        player portrait URL
     * @param competitiveTier current competitive tier
     * @param rankRating      current rank rating, {@code null} when the player is unranked
     */
    public record PlayerRankingResponse(

        Long id,
        String displayName,
        String portrait,
        CompetitiveTier competitiveTier,
        Integer rankRating
    ) {
    }
}
