package io.github.thomashtn.valorant.tracker.challenge.controller;

import io.github.thomashtn.valorant.tracker.challenge.dto.CurrentChallengesResponse;
import io.github.thomashtn.valorant.tracker.challenge.service.ChallengeQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes read-only operations for active weekly challenges.
 */
@RestController
@RequestMapping(value = "/api/challenges", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Challenges", description = "Active weekly challenge consultation.")
public class ChallengeController {

    /**
     * Application service resolving the current week's challenges.
     */
    private final ChallengeQueryService service;

    /**
     * Creates the challenge controller.
     *
     * @param service challenge query service
     */
    public ChallengeController(ChallengeQueryService service) {
        this.service = service;
    }

    /**
     * Returns current-week challenges with collective completion information.
     *
     * @return active week boundaries, last synchronization time and challenge progress
     */
    @GetMapping("/current")
    @Operation(
        summary = "Get current weekly challenges",
        description = """
            Returns the challenges selected for the active calendar week with collective completion
            values. Individual player progress is deliberately excluded and is available from the
            current ranking endpoint.
            """
    )
    @ApiResponse(responseCode = "200", description = "Current challenges returned successfully.")
        public CurrentChallengesResponse getCurrentChallenges() {
        return service.findCurrent();
    }
}
