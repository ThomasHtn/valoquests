package io.github.thomashtn.valorant.tracker.challenge.controller;

import static io.github.thomashtn.valorant.tracker.shared.web.RequiredService.get;

import io.github.thomashtn.valorant.tracker.challenge.dto.CurrentChallengesResponse;
import io.github.thomashtn.valorant.tracker.challenge.service.ChallengeQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes read-only operations for active weekly challenges. */
@RestController
@RequestMapping(value = "/api/challenges", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Challenges", description = "Active weekly challenge consultation.")
public class ChallengeController {

    private final ObjectProvider<ChallengeQueryService> serviceProvider;

    /**
     * Creates the controller with an optional business-service implementation.
     *
     * @param serviceProvider provider resolved when the business layer is implemented
     */
    public ChallengeController(ObjectProvider<ChallengeQueryService> serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    /**
     * Returns current-week challenges with collective completion information.
     *
     * @return active week boundaries, last synchronization time and challenge progress
     */
    @GetMapping("/current")
    @Operation(
        summary = "Get current weekly challenges",
        description = "Returns active challenges and global completion values. Individual player progress is intentionally excluded."
    )
    @ApiResponse(responseCode = "200", description = "Current challenges returned successfully.")
    @ApiResponse(responseCode = "501", description = "Challenge query service has not been implemented yet.")
    public CurrentChallengesResponse getCurrentChallenges() {
        return get(serviceProvider, "Current challenge consultation").findCurrent();
    }
}
