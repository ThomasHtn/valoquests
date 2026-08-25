package io.github.thomashtn.valoquests.colony.model;

/**
 * One building paired with the materials it costs and the capacity it opens.
 *
 * @param building            the structure this tier erects
 * @param materialsThreshold  cumulative materials at which the building goes up
 * @param capacity            population capacity the colony reaches once it is up
 */
public record ColonyBuildingTier(ColonyBuilding building, int materialsThreshold, int capacity) {
}
