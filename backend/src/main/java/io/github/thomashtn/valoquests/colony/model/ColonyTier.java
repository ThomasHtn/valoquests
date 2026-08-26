package io.github.thomashtn.valoquests.colony.model;

/**
 * One step of the town's ladder: the efficiency it opens on, and the name it wears there.
 *
 * @param step      index of the step, counted from the opening efficiency, so it also orders the ladder
 * @param threshold efficiency the step opens at
 * @param name      name the town wears from this step on
 * @param level     citadel number when {@code name} is {@link ColonyTierName#CITADEL}, zero otherwise
 */
public record ColonyTier(int step, double threshold, ColonyTierName name, int level) {
}
