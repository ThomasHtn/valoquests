package io.github.thomashtn.valoquests.colony.dto;

import io.github.thomashtn.valoquests.colony.model.ColonyBuilding;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Exposes the building the run is working towards.
 *
 * @param building           the next structure
 * @param materialsThreshold cumulative materials it goes up at
 * @param capacity           population capacity it will open
 * @param missingMaterials   materials still to gather
 * @param progressPercentage share of the threshold already gathered, in {@code [0, 100]}
 */
@Schema(description = "The building tier a run is working towards.")
public record ColonyNextTierResponse(
    ColonyBuilding building,
    int materialsThreshold,
    int capacity,
    int missingMaterials,
    double progressPercentage
) {
}
