package io.github.thomashtn.valoquests.roster.dto;

import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Exposes one tracked player as the administration screens need it.
 *
 * <p>Unlike {@link io.github.thomashtn.valoquests.profile.dto.PlayerSummaryResponse}, carries raw Riot
 * fields and synchronization state but no statistics.
 *
 * @param id                              internal player identifier
 * @param gameName                        Riot game name
 * @param tagLine                         Riot tag line
 * @param displayName                     name shown in the application
 * @param portrait                        agent portrait, {@code null} when none was chosen
 * @param status                          lifecycle status
 * @param riotPuuid                       stable Riot identifier, {@code null} until a synchronization
 *                                        resolves it
 * @param lastSuccessfulSynchronizationAt end of the player's last successful synchronization
 * @param wasOnAnyRoster                  whether a campaign ever froze the player into its roster, which
 *                                        decides between deletion and archiving
 * @param hasRecentMatch                  whether the player has played at all in the last two weeks
 */
@Schema(description = "Tracked player as seen by the administration screens.")
public record PlayerAdminResponse(

    Long id,
    String gameName,
    String tagLine,
    String displayName,
    String portrait,
    PlayerStatus status,
    String riotPuuid,
    Instant lastSuccessfulSynchronizationAt,
    boolean wasOnAnyRoster,
    boolean hasRecentMatch
) {
}
