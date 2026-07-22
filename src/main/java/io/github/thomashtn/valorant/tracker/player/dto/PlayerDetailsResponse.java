package io.github.thomashtn.valorant.tracker.player.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import io.github.thomashtn.valorant.tracker.player.model.CompetitiveTier;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Exposes a tracked player and all calculated profile statistics.
 */
@Schema(description = "Detailed tracked-player profile and aggregated statistics.")
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

    /**
     * Creates an immutable player-details response.
     */
    public PlayerDetailsResponse {
        agents = List.copyOf(agents);
        maps = List.copyOf(maps);
    }

}
