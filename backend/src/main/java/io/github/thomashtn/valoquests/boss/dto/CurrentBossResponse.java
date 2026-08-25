package io.github.thomashtn.valoquests.boss.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * Exposes the active week's boss confrontation.
 *
 * <p>The three run fields replace the Valorant act the campaign used to be scoped to. They are what
 * lets the campaign map draw a fixed number of hexagons: unlike an act, a run has a known length from
 * the moment it opens.
 *
 * @param weekStart        Monday beginning the active week
 * @param weekEnd          Sunday ending the active week
 * @param boss             drawn boss identity
 * @param effectiveHp      hit points the boss must lose to be defeated this week
 * @param totalDamageDealt cumulative damage dealt so far by every active player
 * @param runNumber        sequential number of the run the campaign is in
 * @param runWeekIndex     position of the active week inside that run, from one
 * @param runWeekCount     number of weeks a run spans
 */
@Schema(description = "Active weekly boss confrontation.")
public record CurrentBossResponse(
    LocalDate weekStart,
    LocalDate weekEnd,
    BossResponse boss,
    int effectiveHp,
    int totalDamageDealt,
    int runNumber,
    int runWeekIndex,
    int runWeekCount
) {
}
