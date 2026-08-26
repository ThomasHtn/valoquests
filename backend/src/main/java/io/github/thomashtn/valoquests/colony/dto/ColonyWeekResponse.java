package io.github.thomashtn.valoquests.colony.dto;

import io.github.thomashtn.valoquests.colony.model.ColonyWeekOutcomeState;
import io.github.thomashtn.valoquests.scoring.model.BossCategory;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Exposes what one week of the run's ten fights was worth to the colony.
 *
 * <p>{@code efficiencyGain} is the figure the map writes on the week's own territory, and it is
 * deliberately efficiency rather than materials or morale. Materials are an intermediate currency the
 * player never handles; efficiency is the axis the context bar and the ladder already display; and it is
 * the only part of a fight's reward still standing on settlement day. Morale does not fit on a tile and
 * lives in the tile's title instead, with the full sentence.
 *
 * @param weekIndex   week of the run, from one
 * @param state       how the week ended
 * @param category    category the week's boss was drawn at, {@code null} while none has been drawn
 * @param materials      materials the fight paid, or would pay if it is still open
 * @param efficiencyGain efficiency those materials are worth on the run's frozen roster
 * @param moraleDelta    morale the fight moved, or would move, negative when the boss holds
 */
@Schema(description = "What one week of the run's fights was worth to the colony.")
public record ColonyWeekResponse(
    int weekIndex,
    ColonyWeekOutcomeState state,
    BossCategory category,
    int materials,
    double efficiencyGain,
    double moraleDelta
) {
}
