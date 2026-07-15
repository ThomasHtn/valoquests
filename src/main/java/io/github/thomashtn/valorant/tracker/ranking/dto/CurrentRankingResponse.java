package io.github.thomashtn.valorant.tracker.ranking.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import io.github.thomashtn.valorant.tracker.player.model.CompetitiveTier;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * Represents the API response payload for current ranking response.
 */
@Schema(description = "API response model documented by the Valorant Tracker OpenAPI specification.")
public record CurrentRankingResponse(
    LocalDate weekStart,
    LocalDate weekEnd,
    Instant calculatedAt,
    List<RankingEntryResponse> ranking
) {
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
    }
    public record PlayerRankingResponse(
        Long id,
        String displayName,
        String portrait,
        CompetitiveTier competitiveTier,
        Integer rankRating
    ) {
    }
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
