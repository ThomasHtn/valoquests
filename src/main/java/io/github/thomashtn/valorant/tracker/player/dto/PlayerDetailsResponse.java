package io.github.thomashtn.valorant.tracker.player.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import io.github.thomashtn.valorant.tracker.player.model.*;
import java.math.*;
import java.time.*;
import java.util.*;

/**
 * Represents the API response payload for player details response.
 */
@Schema(description = "API response model documented by the Valorant Tracker OpenAPI specification.")
public record PlayerDetailsResponse(
    Long id,
    String riotId,
    String displayName,
    String portrait,
    CompetitiveTier competitiveTier,
    Integer rankRating,
    Instant lastSuccessfulSynchronizationAt,
    PlayerStatistics statistics,
    List<AgentStatisticsResponse> agents,
    List<MapStatisticsResponse> maps
) {
    public record PlayerStatistics(
        BigDecimal kda,
        BigDecimal winRate,
        BigDecimal adr,
        BigDecimal acs,
        BigDecimal headshotPercentage,
        long kills,
        long deaths,
        long assists,
        long matchesPlayed,
        long wins,
        long losses,
        long mvps
    ) {
    }
}
