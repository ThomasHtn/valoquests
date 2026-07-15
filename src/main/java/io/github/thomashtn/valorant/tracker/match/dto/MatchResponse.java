package io.github.thomashtn.valorant.tracker.match.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import io.github.thomashtn.valorant.tracker.match.model.*;
import io.github.thomashtn.valorant.tracker.player.model.CompetitiveTier;
import java.math.*;
import java.time.*;

/**
 * Represents the API response payload for match response.
 */
@Schema(description = "API response model documented by the Valorant Tracker OpenAPI specification.")
public record MatchResponse(
    Long id,
    Instant startedAt,
    String mapName,
    GameMode gameMode,
    String agentName,
    MatchResult result,
    Integer allyScore,
    Integer enemyScore,
    int kills,
    int deaths,
    int assists,
    BigDecimal kda,
    BigDecimal acs,
    BigDecimal adr,
    BigDecimal headshotPercentage,
    CompetitiveTier competitiveTier,
    Integer rankRating
) {
}
