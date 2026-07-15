package io.github.thomashtn.valorant.tracker.player.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.*;

/**
 * Represents the API response payload for agent statistics response.
 */
@Schema(description = "API response model documented by the Valorant Tracker OpenAPI specification.")
public record AgentStatisticsResponse(
    String agentId,
    String agentName,
    long matchesPlayed,
    long wins,
    long losses,
    BigDecimal winRate,
    BigDecimal kda,
    BigDecimal adr,
    BigDecimal acs
) {
}
