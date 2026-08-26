package io.github.thomashtn.valoquests.colony.model;

/**
 * One step of the town's ladder: the housing it opens on, and the name it wears there.
 *
 * @param step      index of the step, {@code capacity / 500}, so it also orders the ladder
 * @param threshold housing the step opens at, a multiple of five hundred
 * @param name      name the town wears from this step on
 * @param level     citadel number when {@code name} is {@link ColonyTierName#CITADEL}, zero otherwise
 */
public record ColonyTier(int step, int threshold, ColonyTierName name, int level) {
}
