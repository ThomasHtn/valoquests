package io.github.thomashtn.valoquests.colony.dto;

import io.github.thomashtn.valoquests.colony.model.ColonyTierName;
import io.github.thomashtn.valoquests.colony.model.ColonyTierState;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Exposes one step of the town's ladder.
 *
 * <p>Purely decorative: crossing a step changes no rule at all, it only gives the run a milestone
 * roughly once a week. There is no threshold to save up for either — efficiency is continuous, so a
 * challenge validated on a Monday makes the town's food carry further that same Monday.
 *
 * @param name      name the town wears from this step on
 * @param level     citadel number once the ladder starts repeating, zero on every named step
 * @param threshold efficiency the step opens at
 * @param state     where the step stands relative to the town
 */
@Schema(description = "One step of the town's ladder.")
public record ColonyTierResponse(
    ColonyTierName name,
    int level,
    double threshold,
    ColonyTierState state
) {
}
