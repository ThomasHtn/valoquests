package io.github.thomashtn.valoquests.challenge.controller;

import static io.github.thomashtn.valoquests.shared.config.OpenApiConfig.ADMIN_KEY_SECURITY_SCHEME;

import io.github.thomashtn.valoquests.challenge.service.ChallengeRecalculationService;
import io.github.thomashtn.valoquests.challenge.service.WeeklyChallengeSelectionService;
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
@RequestMapping("/api/admin/challenges")
@Tag(name = "Administration - Challenges", description = "Manual challenge maintenance.")
@SecurityRequirement(name = ADMIN_KEY_SECURITY_SCHEME)
public class ChallengeAdminController {

    /**
     * Service rebuilding the active-week progress and the weekly ranking.
     */
    private final ChallengeRecalculationService recalculationService;

    /**
     * Service drawing the weekly challenge packs.
     */
    private final WeeklyChallengeSelectionService selectionService;

    /**
     * @param recalculationService challenge progress recalculation service
     * @param selectionService     weekly challenge selection service
     */
    public ChallengeAdminController(
        ChallengeRecalculationService recalculationService,
        WeeklyChallengeSelectionService selectionService
    ) {
        this.recalculationService = recalculationService;
        this.selectionService = selectionService;
    }

    /**
     * Recalculates progress exclusively from matches already stored in PostgreSQL.
     */
    @PostMapping("/progress/recalculation")
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

    /**
     * Throws away the current week's challenge pack and draws a new one in its place.
     */
    @PostMapping("/current/redraw")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Redraw the current week's challenges",
        description = """
            Discards the challenge pack the week in progress is holding and draws a new one from
            the catalogue as it currently stands, then rebuilds the week's progress and ranking
            against it.

            This is what the other selection routes deliberately cannot do: the draw is idempotent
            everywhere else, and a week keeps whatever it was given. Use this when the pack itself
            became wrong — a challenge disabled or removed from the catalogue after the week
            opened, or one whose definition changed under it.

            Destructive, and the one admin operation that is not idempotent. The progress recorded
            against the discarded challenges is deleted and cannot be recovered: a player who had
            completed one loses that completion, and the damage it dealt to the week's boss with
            it. Only the week in progress is ever touched; past weeks keep the packs their frozen
            rankings were earned against.

            The colony is not replayed here. Challenge materials are read from the progress this
            deletes, so run the colony recompute afterwards if the run's population matters before
            the next synchronization.
            """
    )
    @ApiResponse(responseCode = "204", description = "A new pack was drawn and progress rebuilt.")
    @ApiResponse(responseCode = "401", description = "X-Admin-Key header is missing.")
    @ApiResponse(responseCode = "403", description = "X-Admin-Key value is invalid.")
    @ApiResponse(
        responseCode = "409",
        description = "The current week's pack is finalized. Nothing was changed."
    )
    public void redrawCurrentWeekChallenges() {
        selectionService.redrawCurrentWeekChallenges();
        recalculationService.recalculateCurrentWeekProgress();
    }
}
