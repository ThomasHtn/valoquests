package io.github.thomashtn.valoquests.boss.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Exposes the finalized boss confrontation for one historical week.
 *
 * <p>A snapshot, not a live projection: a finalized week is immutable, so these values never move again.
 *
 * @param weekStart                Monday beginning the week
 * @param weekEnd                  Sunday ending the week
 * @param runWeekIndex             the week's one-based position in its run, which is what places it on
 *                                 the campaign map. Without it the map can only join fights to the
 *                                 colony's weeks by list position, and a single week that closed
 *                                 without a fight shifts every reward label after it by one.
 * @param finalizedAt              instant the week's outcome became immutable
 * @param boss                     drawn boss identity
 * @param effectiveHp              hit points the boss had to lose to be defeated that week
 * @param totalDamageDealt         cumulative damage dealt that week by every active player
 * @param defeated                 whether the boss was defeated
 * @param defeatedByPlayerId       internal identifier of the player who dealt the finishing blow, when
 *                                 defeated
 * @param defeatedByPlayerDisplayName display name of the player who dealt the finishing blow, when
 *                                 defeated
 */
@Schema(description = "Finalized boss confrontation for one week.")
public record BossHistoryWeekResponse(
    LocalDate weekStart,
    LocalDate weekEnd,
    int runWeekIndex,
    Instant finalizedAt,
    BossResponse boss,
    int effectiveHp,
    int totalDamageDealt,
    boolean defeated,
    Long defeatedByPlayerId,
    String defeatedByPlayerDisplayName
) {
}
