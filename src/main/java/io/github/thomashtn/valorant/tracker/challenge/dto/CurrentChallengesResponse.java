package io.github.thomashtn.valorant.tracker.challenge.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import io.github.thomashtn.valorant.tracker.challenge.model.*;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * Represents the API response payload for current challenges response.
 */
@Schema(description = "API response model documented by the Valorant Tracker OpenAPI specification.")
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
