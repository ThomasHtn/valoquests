package io.github.thomashtn.valoquests.colony.controller;

import io.github.thomashtn.valoquests.colony.dto.ColonyResponse;
import io.github.thomashtn.valoquests.colony.dto.ColonyRunHistoryResponse;
import io.github.thomashtn.valoquests.colony.dto.ColonyTrajectoryResponse;
import io.github.thomashtn.valoquests.colony.service.ColonyQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the squad's shared colony, read-only.
 *
 * <p>There is no decision to make here and nothing to spend: buildings go up on their own, and the whole
 * state is derived from matches, challenges and boss outcomes the squad has already produced.
 */
@RestController
@RequestMapping(value = "/api/colony", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Colony", description = "The squad's shared colony over the ten-week run.")
public class ColonyController {

    /**
     * Service reading the colony off its snapshots.
     */
    private final ColonyQueryService service;

    /**
     * Creates the colony controller.
     *
     * @param service colony query service
     */
    public ColonyController(ColonyQueryService service) {
        this.service = service;
    }

    /**
     * Returns the colony as it stands today.
     *
     * @return today's colony
     */
    @GetMapping
    @Operation(
        summary = "Read the colony",
        description = "Gauges, population, efficiency, materials, next building tier and the gauge "
            + "currently setting the equilibrium population."
    )
    @ApiResponse(responseCode = "200", description = "Colony returned successfully.")
    public ColonyResponse getColony() {
        return service.findCurrent();
    }

    /**
     * Returns the population curve of the run in progress.
     *
     * @return the run's curve, with its peak, its average and its building milestones
     */
    @GetMapping("/trajectory")
    @Operation(
        summary = "Read the run's population curve",
        description = "One point per day played, plus the run's peak, its average and the days its "
            + "buildings went up."
    )
    @ApiResponse(responseCode = "200", description = "Trajectory returned successfully.")
    public ColonyTrajectoryResponse getTrajectory() {
        return service.findTrajectory();
    }

    /**
     * Returns every closed run and how it ended.
     *
     * @return closed runs, most recent first
     */
    @GetMapping("/history")
    @Operation(
        summary = "List closed runs",
        description = "Each closed run's final population, its share of the theoretical maximum, its "
            + "peak, its average, its buildings and its defeated bosses."
    )
    @ApiResponse(responseCode = "200", description = "History returned successfully.")
    public List<ColonyRunHistoryResponse> getHistory() {
        return service.findHistory();
    }
}
