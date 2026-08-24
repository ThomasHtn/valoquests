package io.github.thomashtn.valoquests.boss.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * Exposes the active week's boss confrontation.
 *
 * @param weekStart               Monday beginning the active week
 * @param weekEnd                 Sunday ending the active week
 * @param boss                    drawn boss identity
 * @param effectiveHp             hit points the boss must lose to be defeated this week
 * @param totalDamageDealt        cumulative damage dealt so far by every active player
 * @param campaignSeasonName      raw name of the Valorant act the campaign runs in, {@code null}
 *     while no match has been imported and no act can be resolved
 */
@Schema(description = "Active weekly boss confrontation.")
public record CurrentBossResponse(
    LocalDate weekStart,
    LocalDate weekEnd,
    BossResponse boss,
    int effectiveHp,
    int totalDamageDealt,
    String campaignSeasonName
) {
}
