package io.github.thomashtn.valoquests.player.dto;

import io.github.thomashtn.valoquests.player.model.CompetitiveTier;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Exposes the compact player information used by list screens.
 */
@Schema(description = "Compact tracked-player summary.")
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
