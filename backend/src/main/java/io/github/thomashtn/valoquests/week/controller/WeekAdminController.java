package io.github.thomashtn.valoquests.week.controller;

import static io.github.thomashtn.valoquests.shared.config.OpenApiConfig.ADMIN_KEY_SECURITY_SCHEME;

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
     * Service executing the whole weekly rollover.
     */
    private final WeeklyRolloverService weeklyRolloverService;

    /**
     * Creates the administrative week controller.
     *
     * @param weeklyRolloverService weekly rollover service
     */
    public WeekAdminController(WeeklyRolloverService weeklyRolloverService) {
        this.weeklyRolloverService = weeklyRolloverService;
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
}
