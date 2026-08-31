package io.github.thomashtn.valoquests.week.controller;

import static io.github.thomashtn.valoquests.shared.config.OpenApiConfig.ADMIN_KEY_SECURITY_SCHEME;

import io.github.thomashtn.valoquests.week.WeekCalendar;
import io.github.thomashtn.valoquests.week.service.WeeklyLifecycleCoordinator;
import io.github.thomashtn.valoquests.week.service.WeeklyRolloverService;
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
 * Exposes the protected weekly-setup maintenance operations.
 */
@RestController
@RequestMapping("/api/admin/weeks")
@Tag(name = "Administration - Weeks", description = "Manual weekly-setup maintenance.")
@SecurityRequirement(name = ADMIN_KEY_SECURITY_SCHEME)
public class WeekAdminController {

    /**
     * Service preparing a week's challenge pack and boss encounter.
     */
    private final WeeklyLifecycleCoordinator weeklyLifecycleCoordinator;

    /**
     * Service executing the whole weekly rollover.
     */
    private final WeeklyRolloverService weeklyRolloverService;

    /**
     * Calendar resolving which week is currently in progress.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the administrative week controller.
     *
     * @param weeklyLifecycleCoordinator weekly lifecycle coordinator
     * @param weeklyRolloverService      weekly rollover service
     * @param weekCalendar               week calendar
     */
    public WeekAdminController(
        WeeklyLifecycleCoordinator weeklyLifecycleCoordinator,
        WeeklyRolloverService weeklyRolloverService,
        WeekCalendar weekCalendar
    ) {
        this.weeklyLifecycleCoordinator = weeklyLifecycleCoordinator;
        this.weeklyRolloverService = weeklyRolloverService;
        this.weekCalendar = weekCalendar;
    }

    /**
     * Runs the weekly rollover now, instead of waiting for the next Monday.
     */
    @PostMapping("/rollover")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Run the weekly rollover now",
        description = """
            Runs the exact rollover the Monday schedule runs: it finalizes every past week still
            open, resolves every past boss encounter whose fight was never settled, and opens the
            week currently in progress with its challenge pack, its boss and its ranking at zero.

            This is a repair tool for a rollover that did not run or failed halfway. It is
            idempotent and safe to call mid-week: only weeks strictly before the one in progress
            are finalized, so the running week is never closed early.

            Unlike the scheduled job it does not synchronize first. Trigger a synchronization
            before this one if the closing week's very last matches have not been imported yet,
            or they will count for nothing.
            """
    )
    @ApiResponse(responseCode = "204", description = "The rollover completed.")
    @ApiResponse(responseCode = "401", description = "X-Admin-Key header is missing.")
    @ApiResponse(responseCode = "403", description = "X-Admin-Key value is invalid.")
    @ApiResponse(
        responseCode = "500",
        description = "A week's challenge pack is only partially finalized. Nothing was changed."
    )
    public void rolloverNow() {
        weeklyRolloverService.rolloverIfNeeded();
    }

    /**
     * Draws the current week's challenge pack and boss encounter if the rollover did not.
     */
    @PostMapping("/current/selection")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Select the current week's challenges and boss",
        description = """
            Runs the exact selection the Monday rollover runs, applied to the week currently in
            progress: it draws the missing challenges, one per difficulty, and the week's boss
            encounter.

            This is a repair tool for a rollover that did not run or failed halfway. It is
            idempotent and never redraws: challenges already selected for the week are kept, only
            the missing difficulties are completed, and the boss drawn for the week is preserved.

            An unfinalized encounter is re-sized against the roster as it currently stands, so this
            is also how a week is re-sized after a player was activated or deactivated mid-week.
            It does not rebuild challenge progress or the ranking: use the challenge progress
            recalculation for that.
            """
    )
    @ApiResponse(responseCode = "204", description = "The current week is fully set up.")
    @ApiResponse(responseCode = "401", description = "X-Admin-Key header is missing.")
    @ApiResponse(responseCode = "403", description = "X-Admin-Key value is invalid.")
    public void selectCurrentWeek() {
        weeklyLifecycleCoordinator.openWeek(weekCalendar.currentWeekStart());
    }
}
