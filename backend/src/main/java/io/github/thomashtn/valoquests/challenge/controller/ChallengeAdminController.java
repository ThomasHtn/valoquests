package io.github.thomashtn.valoquests.challenge.controller;

import static io.github.thomashtn.valoquests.shared.config.OpenApiConfig.ADMIN_KEY_SECURITY_SCHEME;

import io.github.thomashtn.valoquests.challenge.service.ChallengeRecalculationService;
import io.github.thomashtn.valoquests.challenge.service.WeeklyChallengeDrawService;
import io.github.thomashtn.valoquests.shared.concurrency.MatchHistoryLock;
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
 * Exposes the protected challenge-pack maintenance operation.
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
     * Service drawing the weekly packs.
     */
    private final WeeklyChallengeDrawService weeklyDrawService;

    /**
     * Lock keeping the redraw from overlapping another job writing the match history.
     */
    private final MatchHistoryLock matchHistoryLock;

    /**
     * Creates the challenge administration controller.
     *
     * @param recalculationService challenge progress recalculation service
     * @param weeklyDrawService    weekly challenge draw service
     * @param matchHistoryLock     lock shared by every job writing the match history
     */
    public ChallengeAdminController(
        ChallengeRecalculationService recalculationService,
        WeeklyChallengeDrawService weeklyDrawService,
        MatchHistoryLock matchHistoryLock
    ) {
        this.recalculationService = recalculationService;
        this.weeklyDrawService = weeklyDrawService;
        this.matchHistoryLock = matchHistoryLock;
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
            completed one loses that completion, and the rescues and ranking points it earned with
            it. Only the week in progress is ever touched; past weeks keep the packs their frozen
            rankings were earned against.

            The week's daily challenges are not part of the pack and keep their progress.

            Refused with a 409 while another job writing the match history runs.
            """
    )
    @ApiResponse(responseCode = "204", description = "A new pack was drawn and progress rebuilt.")
    @ApiResponse(responseCode = "401", description = "X-Admin-Key header is missing.")
    @ApiResponse(responseCode = "403", description = "X-Admin-Key value is invalid.")
    @ApiResponse(
        responseCode = "409",
        description = """
            The current week's pack is finalized, or another job writing the match history is
            already running. Nothing was changed.
            """
    )
    public void redrawCurrentWeekChallenges() {
        matchHistoryLock.runOrReject(() -> {
            weeklyDrawService.redrawCurrentWeekChallenges();
            recalculationService.drawAndRecalculateCurrentWeek();
        });
    }
}
