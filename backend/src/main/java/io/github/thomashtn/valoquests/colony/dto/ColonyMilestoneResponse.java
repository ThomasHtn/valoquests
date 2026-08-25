package io.github.thomashtn.valoquests.colony.dto;

import io.github.thomashtn.valoquests.colony.model.ColonyBuilding;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * Exposes the day a building went up, so the curve can be read against it.
 *
 * @param building the structure
 * @param day      calendar day it went up
 * @param runDay   day of the run it went up on, from one
 * @param capacity population capacity it opened
 */
@Schema(description = "The day a colony building went up.")
public record ColonyMilestoneResponse(
    ColonyBuilding building,
    LocalDate day,
    int runDay,
    int capacity
) {
}
