package io.github.thomashtn.valoquests.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Exposes aggregated statistics for one map played by a player.
 *
 * @param mapName       map played
 * @param matchesPlayed matches played
 * @param wins          matches won
 * @param losses        matches lost
 * @param winRate       share of matches won, as a percentage
 * @param kda           ratio of kills and assists to deaths
 * @param adr           average damage per round
 * @param acs           average combat score
 */
@Schema(description = "Aggregated player statistics for one Valorant map.")
public record MapStatisticsResponse(

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
