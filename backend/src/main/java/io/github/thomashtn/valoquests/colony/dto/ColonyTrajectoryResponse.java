package io.github.thomashtn.valoquests.colony.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * Exposes the population curve of the run in progress.
 *
 * <p>The average is what separates a run that was held from a hollow one that happened to end at the
 * same place.
 *
 * @param runNumber         sequential number of the run
 * @param runDayCount       days a run spans, settlement day included
 * @param peakPopulation    highest population the run reached
 * @param peakDay           day it reached it, {@code null} while the run has no day yet
 * @param averagePopulation mean population over the days played so far
 * @param points            one point per day played, oldest first
 * @param milestones        the days buildings went up
 */
@Schema(description = "Population curve of the run in progress.")
public record ColonyTrajectoryResponse(
    int runNumber,
    int runDayCount,
    int peakPopulation,
    LocalDate peakDay,
    int averagePopulation,
    List<ColonyTrajectoryPointResponse> points,
    List<ColonyMilestoneResponse> milestones
) {
    /**
     * Creates an immutable trajectory response.
     */
    public ColonyTrajectoryResponse {
        points = List.copyOf(points);
        milestones = List.copyOf(milestones);
    }
}
