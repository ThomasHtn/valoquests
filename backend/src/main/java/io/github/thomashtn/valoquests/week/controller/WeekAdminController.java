package io.github.thomashtn.valoquests.week.controller;

import static io.github.thomashtn.valoquests.shared.config.OpenApiConfig.ADMIN_KEY_SECURITY_SCHEME;

import io.github.thomashtn.valoquests.week.WeekCalendar;
import io.github.thomashtn.valoquests.week.service.WeeklyLifecycleCoordinator;
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
 * Exposes the protected weekly-setup maintenance operation.
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
     * Calendar resolving which week is currently in progress.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the administrative week controller.
     *
     * @param weeklyLifecycleCoordinator weekly lifecycle coordinator
     * @param weekCalendar               week calendar
     */
    public WeekAdminController(
        WeeklyLifecycleCoordinator weeklyLifecycleCoordinator,
        WeekCalendar weekCalendar
    ) {
        this.weeklyLifecycleCoordinator = weeklyLifecycleCoordinator;
        this.weekCalendar = weekCalendar;
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
            the missing difficulties are completed, and an existing boss encounter is left exactly
            as it is. Running it on a healthy week therefore changes nothing.
            """
    )
    @ApiResponse(responseCode = "204", description = "The current week is fully set up.")
    @ApiResponse(responseCode = "401", description = "X-Admin-Key header is missing.")
    @ApiResponse(responseCode = "403", description = "X-Admin-Key value is invalid.")
    public void selectCurrentWeek() {
        weeklyLifecycleCoordinator.openWeek(weekCalendar.currentWeekStart());
    }
}
