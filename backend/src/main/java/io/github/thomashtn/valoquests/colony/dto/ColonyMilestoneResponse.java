package io.github.thomashtn.valoquests.colony.dto;

import io.github.thomashtn.valoquests.colony.model.ColonyTierName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * Exposes the day the town crossed one step of its ladder, for the curve to be read against.
 *
 * @param name      name the town took on that day
 * @param level     citadel number once the ladder starts repeating, zero on every named step
 * @param day       calendar day the step was crossed
 * @param runDay    that day's one-based position in the run
 * @param threshold housing the step opens at
 */
@Schema(description = "The day the town crossed one step of its ladder.")
public record ColonyMilestoneResponse(
    ColonyTierName name,
    int level,
    LocalDate day,
    int runDay,
    int threshold
) {
}
