package io.github.thomashtn.valoquests.colony.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * Exposes one closed run and how it ended.
 *
 * <p>The score is the population of the settlement day, once the tenth week's materials and boss have
 * been credited. Settling that way makes the last fight a finale: its materials have only one day of
 * migration left to turn into inhabitants, but they are still the last lever on capacity.
 *
 * @param runNumber          sequential number of the run
 * @param firstDay           first day of the run
 * @param settlementDay      the run's seventy-first day, which carries its score
 * @param finalPopulation    the run's score
 * @param maximumPercentage  score as a share of the theoretical maximum, in {@code [0, 100]}
 * @param peakPopulation     highest population the run reached
 * @param averagePopulation  mean population over the run
 * @param erectedBuildings   buildings that went up
 * @param buildingCount      buildings a run can put up
 * @param defeatedBosses     bosses put down
 * @param bossCount          bosses a run holds
 */
@Schema(description = "One closed run and how it ended.")
public record ColonyRunHistoryResponse(
    int runNumber,
    LocalDate firstDay,
    LocalDate settlementDay,
    int finalPopulation,
    double maximumPercentage,
    int peakPopulation,
    int averagePopulation,
    int erectedBuildings,
    int buildingCount,
    int defeatedBosses,
    int bossCount
) {
}
