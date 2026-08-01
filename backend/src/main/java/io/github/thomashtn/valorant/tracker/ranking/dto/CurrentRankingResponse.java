package io.github.thomashtn.valorant.tracker.ranking.dto;

import io.github.thomashtn.valorant.tracker.player.model.CompetitiveTier;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Exposes the current weekly ranking and per-challenge progress.
 */
@Schema(description = "Current weekly player ranking.")
public record CurrentRankingResponse(

    LocalDate weekStart,
    LocalDate weekEnd,
    Instant calculatedAt,
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
     * @param position            current rank, starting at 1
     * @param previousPosition    rank held in the previous week, {@code null} when the player had
     *     none
     * @param positionVariation   places gained since the previous week, negative when lost
     * @param player              identity shown next to the rank
     * @param points              points accumulated so far this week
     * @param completedChallenges challenges completed so far
     * @param totalChallenges     challenges selected for the week
     * @param challengeProgress   per-challenge progress backing the counters above
     */
    public record RankingEntryResponse(

        int position,
        Integer previousPosition,
        int positionVariation,
        PlayerRankingResponse player,
        int points,
        int completedChallenges,
        int totalChallenges,
        List<ChallengeProgressResponse> challengeProgress
    ) {
        /**
         * Creates an immutable ranking entry.
         */
        public RankingEntryResponse {
            challengeProgress = List.copyOf(challengeProgress);
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

    /**
     * Exposes one player's progress towards a single weekly challenge.
     *
     * @param challengeId   internal challenge identifier
     * @param challengeName challenge name shown to players
     * @param metric        metric the challenge measures
     * @param currentValue  value reached so far
     * @param targetValue   value required to complete the challenge
     * @param unit          unit the values are expressed in
     * @param completed     whether the target has been reached
     */
    public record ChallengeProgressResponse(

        Long challengeId,
        String challengeName,
        String metric,
        BigDecimal currentValue,
        BigDecimal targetValue,
        String unit,
        boolean completed
    ) {
    }
}
