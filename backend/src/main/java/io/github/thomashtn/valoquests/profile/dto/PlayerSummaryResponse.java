package io.github.thomashtn.valoquests.profile.dto;

import io.github.thomashtn.valoquests.player.model.CompetitiveTier;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Exposes the compact player information used by list screens.
 *
 * <p>Statistics cover the competitive matches of the season in progress.
 *
 * @param id                              internal player identifier
 * @param riotId                          full Riot ID, {@code gameName#tagLine}
 * @param displayName                     name shown in the application
 * @param portrait                        agent portrait chosen by the player, {@code null} when none was chosen
 * @param competitiveTier                 current competitive tier
 * @param rankRating                      current rank rating, {@code null} when the player is unranked
 * @param kda                             ratio of kills and assists to deaths
 * @param winRate                         share of matches won, as a percentage
 * @param headshotPercentage              share of hits that landed on the head
 * @param matchesPlayed                   matches played
 * @param status                          lifecycle status
 * @param lastSuccessfulSynchronizationAt end of the player's last successful synchronization
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
