package io.github.thomashtn.valorant.tracker.ranking.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.*;
import java.util.*;

/**
 * Represents the API response payload for ranking history week response.
 */
@Schema(description = "API response model documented by the Valorant Tracker OpenAPI specification.")
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
}
