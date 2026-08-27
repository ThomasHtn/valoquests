package io.github.thomashtn.valoquests.colony.dto;

import io.github.thomashtn.valoquests.colony.model.ColonyWeekOutcomeState;
import io.github.thomashtn.valoquests.scoring.model.BossCategory;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Exposes what one week of the run's ten fights was worth to the colony.
 *
 * <p>{@code materials} is the figure the map writes on the week's own territory. Materials rather than
 * the efficiency they buy: the ladder prices every one of its steps in materials, so the two panels read
 * in one currency and a tile's figure can be compared straight against the step it helps pay for.
 * {@code efficiencyGain} is that same amount of materials read the other way, as the fraction of an
 * efficiency point they buy back — it is what the hover card answers when a reader has already read the
 * materials figure and asks what it is actually worth. Morale does not fit on a tile and lives in the
 * tile's title instead, with the full sentence.
 *
 * @param weekIndex      week of the run, from one
 * @param state          how the week ended
 * @param category       category the week's boss was drawn at, {@code null} while none has been drawn
 * @param materials      materials the fight paid, or would pay if it is still open
 * @param efficiencyGain efficiency those materials buy back, on top of the base rate, zero on a week
 *                        that settled nothing
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
