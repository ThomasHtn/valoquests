package io.github.thomashtn.valorant.tracker.ranking.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Exposes the finalized ranking for one historical week.
 */
@Schema(description = "Finalized ranking for one week.")
public record RankingHistoryWeekResponse(

    LocalDate weekStart,
    LocalDate weekEnd,
    Instant finalizedAt,
    Long winnerPlayerId,
    List<FinalRankingEntryResponse> ranking
) {
    public record FinalRankingEntryResponse(

        int position,
        Long playerId,
        String displayName,
        int points,
        int completedChallenges
    ) {
    }

    /**
     * Creates an immutable historical ranking response.
     */
    public RankingHistoryWeekResponse {
        ranking = List.copyOf(ranking);
    }

}
