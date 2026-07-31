package io.github.thomashtn.valorant.tracker.challenge.controller;

import static io.github.thomashtn.valorant.tracker.shared.config.OpenApiConfig.ADMIN_KEY_SECURITY_SCHEME;

import io.github.thomashtn.valorant.tracker.challenge.service.ChallengeRecalculationService;
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
 * Exposes the protected challenge-progress maintenance operation.
 */
@RestController
@RequestMapping("/api/admin/challenges/progress")
@Tag(name = "Administration - Challenges", description = "Manual challenge-progress maintenance.")
@SecurityRequirement(name = ADMIN_KEY_SECURITY_SCHEME)
public class ChallengeAdminController {

    /**
     * Service rebuilding the active-week progress and the weekly ranking.
     */
    private final ChallengeRecalculationService recalculationService;

    /**
     * @param recalculationService challenge progress recalculation service
     */
    public ChallengeAdminController(ChallengeRecalculationService recalculationService) {
        this.recalculationService = recalculationService;
    }

    /**
     * Recalculates progress exclusively from matches already stored in PostgreSQL.
     */
    @PostMapping("/recalculation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Recalculate current challenge progress",
        description = """
            Rebuilds the active-week challenge progress from matches already stored in PostgreSQL.
            This operation does not call the Henrik API and refreshes the weekly ranking after the
            progress calculation completes.

            Synchronization already runs this recalculation whenever it imports a match, so this
            route is a repair tool: it is what replays the calculation after a challenge definition
            changed, or after a recalculation failed at the end of a synchronization.
            """
    )
    @ApiResponse(responseCode = "204", description = "Progress and ranking recalculated successfully.")
    public void recalculateChallengeProgress() {
        recalculationService.recalculateCurrentWeekProgress();
    }
}
