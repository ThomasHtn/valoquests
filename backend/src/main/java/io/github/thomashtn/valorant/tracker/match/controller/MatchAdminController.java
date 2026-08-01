package io.github.thomashtn.valorant.tracker.match.controller;

import static io.github.thomashtn.valorant.tracker.shared.config.OpenApiConfig.ADMIN_KEY_SECURITY_SCHEME;

import io.github.thomashtn.valorant.tracker.match.dto.GameModeCorrectionRequest;
import io.github.thomashtn.valorant.tracker.match.dto.MatchCorrectionResponse;
import io.github.thomashtn.valorant.tracker.match.entity.ValorantMatch;
import io.github.thomashtn.valorant.tracker.match.service.MatchCorrectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes protected manual match corrections.
 */
@RestController
@RequestMapping(value = "/api/admin/matches", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Administration - Matches", description = "Manual match corrections.")
@SecurityRequirement(name = ADMIN_KEY_SECURITY_SCHEME)
public class MatchAdminController {

    /**
     * Service applying manual match corrections.
     */
    private final MatchCorrectionService correctionService;

    /**
     * Creates the administrative match controller.
     *
     * @param correctionService match correction service
     */
    public MatchAdminController(MatchCorrectionService correctionService) {
        this.correctionService = correctionService;
    }

    /**
     * Overrides the game mode Henrik resolved for one match.
     *
     * @param matchId internal match identifier
     * @param request requested game mode
     * @return the corrected match
     */
    @PatchMapping("/{matchId}/game-mode")
    @Operation(
        summary = "Manually correct a match's game mode",
        description = """
            Overrides the game mode resolved for one match and records the correction as
            MANUALLY_CORRECTED, the highest-priority source. Synchronization never overwrites a
            manually corrected match, regardless of what Henrik reports for it afterwards.
            """
    )
    @ApiResponse(responseCode = "200", description = "Game mode corrected successfully.")
    @ApiResponse(responseCode = "400", description = "The requested game mode is missing or invalid.")
    @ApiResponse(responseCode = "401", description = "X-Admin-Key header is missing.")
    @ApiResponse(responseCode = "403", description = "X-Admin-Key value is invalid.")
    @ApiResponse(responseCode = "404", description = "The requested match does not exist.")
    public MatchCorrectionResponse correctGameMode(
        @Parameter(description = "Internal match identifier.", example = "1204", required = true)
        @PathVariable long matchId,

        @Valid @RequestBody GameModeCorrectionRequest request
    ) {
        ValorantMatch corrected =
            correctionService.correctGameMode(matchId, request.gameMode());

        return new MatchCorrectionResponse(
            corrected.getId(),
            corrected.getGameMode(),
            corrected.getGameModeSource()
        );
    }
}
