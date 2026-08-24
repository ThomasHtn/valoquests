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
 */
@Schema(description = "Active weekly boss confrontation.")
public record CurrentBossResponse(
    LocalDate weekStart,
    LocalDate weekEnd,
    BossResponse boss,
    int effectiveHp,
    int totalDamageDealt
) {
}
