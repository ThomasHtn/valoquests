package io.github.thomashtn.valoquests.player.dto;

import io.github.thomashtn.valoquests.player.model.CompetitiveTier;
import io.swagger.v3.oas.annotations.media.Schema;
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
    /**
     * Exposes one player's aggregated statistics over their whole stored history.
     *
     * <p>Derived from the stored matches, so the totals only cover the acts the synchronization
     * actually imported and will read lower than a lifetime figure from an external tracker.
     *
     * @param kda                ratio of kills and assists to deaths
     * @param winRate            share of matches won, as a percentage
     * @param adr                average damage per round
     * @param acs                average combat score
     * @param headshotPercentage share of hits that landed on the head
     * @param kills              total kills
     * @param deaths             total deaths
     * @param assists            total assists
     * @param matchesPlayed      total matches played
     * @param wins               total matches won
     * @param losses             total matches lost
     * @param mvps               matches finished with the best score of the game
     */
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
