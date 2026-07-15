package io.github.thomashtn.valorant.tracker.player.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import io.github.thomashtn.valorant.tracker.player.model.*;
import java.math.*;
import java.time.*;

/**
 * Represents the API response payload for player summary response.
 */
@Schema(description = "API response model documented by the Valorant Tracker OpenAPI specification.")
public record PlayerSummaryResponse(
    Long id,
    String riotId,
    String displayName,
    String portrait,
    CompetitiveTier competitiveTier,
    Integer rankRating,
    BigDecimal kda,
    BigDecimal winRate,
    BigDecimal headshotPercentage,
    long matchesPlayed,
    PlayerStatus status,
    Instant lastSuccessfulSynchronizationAt
) {
}
