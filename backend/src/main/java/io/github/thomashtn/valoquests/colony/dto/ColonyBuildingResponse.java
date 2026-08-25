package io.github.thomashtn.valoquests.colony.dto;

import io.github.thomashtn.valoquests.colony.model.ColonyBuilding;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Exposes one building tier and whether the run has reached it.
 *
 * @param building           the structure
 * @param materialsThreshold cumulative materials it goes up at
 * @param capacity           population capacity it opens
 * @param erected            whether the run has reached it
 * @param erectedOnRunDay    day of the run it went up on, {@code null} while it has not
 */
@Schema(description = "One colony building tier.")
public record ColonyBuildingResponse(
    ColonyBuilding building,
    int materialsThreshold,
    int capacity,
    boolean erected,
    Integer erectedOnRunDay
) {
}
