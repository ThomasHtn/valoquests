package io.github.thomashtn.valorant.tracker.player.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Exposes aggregated statistics for one map played by a player.
 */
@Schema(description = "Aggregated player statistics for one Valorant map.")
public record MapStatisticsResponse(

    String mapId,
    String mapName,
    long matchesPlayed,
    long wins,
    long losses,
    BigDecimal winRate,
    BigDecimal kda,
    BigDecimal adr,
    BigDecimal acs
) {
}
