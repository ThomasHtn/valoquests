package io.github.thomashtn.valoquests.player.dto;

import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Exposes one tracked player as the administration screens need it.
 *
 * <p>Distinct from {@link PlayerSummaryResponse}: administration edits identities rather than
 * displaying performance, so it carries the raw Riot fields and the synchronization state, and none
 * of the aggregated statistics.
 *
 * @param riotPuuid                stable Riot identifier, {@code null} until a synchronization
 *                                 resolves it
 * @param hasCampaignContribution  whether the player took part in the campaign, which is what
 *                                 decides between deletion and archiving
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
    boolean hasCampaignContribution
) {
}
