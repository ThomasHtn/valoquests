package io.github.thomashtn.valoquests.challenge.controller;

import static io.github.thomashtn.valoquests.shared.config.OpenApiConfig.ADMIN_KEY_SECURITY_SCHEME;

import io.github.thomashtn.valoquests.challenge.service.ChallengeRecalculationService;
import io.github.thomashtn.valoquests.challenge.service.WeeklyChallengeSelectionService;
import io.github.thomashtn.valoquests.week.WeekCalendar;
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
     * Service drawing the weekly packs and the daily challenges.
     */
    private final WeeklyChallengeSelectionService selectionService;

    /**
     * Calendar resolving the current day.
     */
    private final WeekCalendar weekCalendar;

    /**
     * @param recalculationService challenge progress recalculation service
     * @param selectionService     challenge selection service
     * @param weekCalendar         calendar resolving the current day
     */
    public ChallengeAdminController(
        ChallengeRecalculationService recalculationService,
        WeeklyChallengeSelectionService selectionService,
        WeekCalendar weekCalendar
    ) {
        this.recalculationService = recalculationService;
        this.selectionService = selectionService;
        this.weekCalendar = weekCalendar;
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

            The week's daily challenges are not part of the pack and keep their progress.
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

    /**
     * Draws today's daily challenge if the day has none yet, then rebuilds progress against it.
     */
    @PostMapping("/daily/selection")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Draw today's daily challenge",
        description = """
            Draws the daily challenge of the current day from the daily pool when the day has none
            yet, then rebuilds the week's progress and ranking. Idempotent: a day keeps the
            challenge it was given.

            The scheduled synchronization draws it on its own within half an hour of midnight; this
            route is for the operator who does not want to wait, or whose scheduler did not run.
            """
    )
    @ApiResponse(responseCode = "204", description = "Today's challenge is drawn and progress rebuilt.")
    @ApiResponse(responseCode = "401", description = "X-Admin-Key header is missing.")
    @ApiResponse(responseCode = "403", description = "X-Admin-Key value is invalid.")
    public void drawDailyChallenge() {
        selectionService.selectDailyChallenge(weekCalendar.today());
        recalculationService.recalculateCurrentWeekProgress();
    }
}
