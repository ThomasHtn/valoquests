package io.github.thomashtn.valorant.tracker.ranking.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import io.github.thomashtn.valorant.tracker.player.model.CompetitiveTier;
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
