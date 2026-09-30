package io.github.thomashtn.valoquests.synchronization.controller;

import io.github.thomashtn.valoquests.synchronization.dto.SynchronizationStatusResponse;
import io.github.thomashtn.valoquests.synchronization.service.SynchronizationQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the public synchronization status.
 */
@RestController
@RequestMapping(value = "/api/synchronization", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Synchronization", description = "Public synchronization status.")
public class SynchronizationController {

    /**
     * Application service resolving synchronization executions.
     */
    private final SynchronizationQueryService service;

    /**
     * Creates the synchronization controller.
     *
     * @param service synchronization query service
     */
    public SynchronizationController(SynchronizationQueryService service) {
        this.service = service;
    }

    /**
     * Returns whether a synchronization is in progress and when the last one finished.
     *
     * @return the public synchronization status
     */
    @GetMapping("/status")
    @Operation(
        summary = "Get the synchronization status",
        description = """
            Returns whether a synchronization is in progress and when the last successful one
            finished. An execution only finishes once challenge progress and the campaign have been
            rebuilt, so the instant marks when the public data became current.
            """
    )
    @ApiResponse(responseCode = "200", description = "Synchronization status returned successfully.")
    public SynchronizationStatusResponse getStatus() {
        return service.findStatus();
    }
}
