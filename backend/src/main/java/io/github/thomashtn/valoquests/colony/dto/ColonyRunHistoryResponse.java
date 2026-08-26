package io.github.thomashtn.valoquests.colony.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * Exposes one closed run and how it ended.
 *
 * <p>The score is the population of the settlement day, once the tenth week's materials and boss have
 * been credited. Settling that way makes the last fight a finale: its materials have only one night of
 * migration left to turn into inhabitants, but they are still the last lever on efficiency.
 *
 * <p>There is deliberately no share of a maximum here. Efficiency has no ceiling — every challenge
 * validated raises it, on the last Monday exactly as much as on the first — so any percentage would be
 * measured against a number the model does not have.
 *
 * @param runNumber         sequential number of the run
 * @param firstDay          first day of the run
 * @param settlementDay     the run's seventy-first day, which carries its score
 * @param finalPopulation   the run's score
 * @param peakPopulation    highest population the run reached
 * @param averagePopulation mean population over the run
 * @param efficiency        efficiency the run finished on
 * @param materials         materials the run finished on
 * @param tier              step of the ladder the run finished on
 * @param defeatedBosses    bosses put down
 * @param bossCount         bosses a run holds
 */
@Schema(description = "One closed run and how it ended.")
public record ColonyRunHistoryResponse(
    int runNumber,
    LocalDate firstDay,
    LocalDate settlementDay,
    int finalPopulation,
    int peakPopulation,
    int averagePopulation,
    double efficiency,
    int materials,
    ColonyTierResponse tier,
    int defeatedBosses,
    int bossCount
) {
}
