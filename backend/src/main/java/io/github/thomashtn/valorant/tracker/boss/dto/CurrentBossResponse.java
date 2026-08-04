package io.github.thomashtn.valorant.tracker.boss.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * Exposes the active week's boss confrontation.
 *
 * @param weekStart               Monday beginning the active week
 * @param weekEnd                 Sunday ending the active week
 * @param boss                    drawn boss identity
 * @param baseHp                  base hit points of the drawn boss's category
 * @param difficultyModifierPercent collective difficulty modifier applied this week, in percent
 * @param effectiveHp             hit points the boss must lose to be defeated this week
 * @param totalDamageDealt        cumulative damage dealt so far by every active player
 */
@Schema(description = "Active weekly boss confrontation.")
public record CurrentBossResponse(
    LocalDate weekStart,
    LocalDate weekEnd,
    BossResponse boss,
    int baseHp,
    int difficultyModifierPercent,
    int effectiveHp,
    int totalDamageDealt
) {
}
