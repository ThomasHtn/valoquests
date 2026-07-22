package io.github.thomashtn.valorant.tracker.match.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import io.github.thomashtn.valorant.tracker.match.model.GameMode;
import io.github.thomashtn.valorant.tracker.match.model.MatchResult;
import io.github.thomashtn.valorant.tracker.player.model.CompetitiveTier;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Exposes one player match in the paginated match-history API.
 */
@Schema(description = "Player-centric match history entry.")
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
