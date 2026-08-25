package io.github.thomashtn.valoquests.colony.controller;

import static io.github.thomashtn.valoquests.shared.config.OpenApiConfig.ADMIN_KEY_SECURITY_SCHEME;

import io.github.thomashtn.valoquests.colony.service.ColonyReplayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the colony's repair operation.
 */
@RestController
@RequestMapping("/api/admin/colony")
@Tag(name = "Administration - Colony", description = "Colony repair operations.")
@SecurityRequirement(name = ADMIN_KEY_SECURITY_SCHEME)
public class ColonyAdminController {

    /**
     * Service replaying the run in progress.
     */
    private final ColonyReplayService replayService;

    /**
     * Creates the administrative colony controller.
     *
     * @param replayService colony replay service
     */
    public ColonyAdminController(ColonyReplayService replayService) {
        this.replayService = replayService;
    }

    /**
     * Rebuilds the run in progress from its first day.
     *
     * <p>Free of any double-application risk: the colony is never mutated incrementally, so this
     * recomputes exactly what a nightly tick would and rewrites the same rows.
     */
    @PostMapping("/recompute")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Replay the run in progress",
        description = "Rebuilds every day of the colony from the matches, challenges and boss "
            + "outcomes already stored. Idempotent."
    )
    @ApiResponse(responseCode = "204", description = "Colony replayed successfully.")
    public void recompute() {
        replayService.replayCurrentRun();
    }
}
