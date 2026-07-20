package io.github.thomashtn.valorant.tracker.challenge.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeDifficulty;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Exposes collective progress for the challenges selected for the current week.
 */
@Schema(description = "Current weekly challenges and their collective completion progress.")
public record CurrentChallengesResponse(
    LocalDate weekStart,
    LocalDate weekEnd,
    Instant lastSuccessfulSynchronizationAt,
    List<ChallengeProgressResponse> challenges
) {
    public record ChallengeProgressResponse(
        Long id,
        String name,
        String description,
        ChallengeDifficulty difficulty,
        String metric,
        BigDecimal targetValue,
        int points,
        int completedPlayers,
        int totalPlayers,
        BigDecimal completionPercentage
    ) {
    }
}
