package io.github.thomashtn.valoquests.run.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * Exposes the campaign's lifecycle to an operator: every run, current or closed, and whether the
 * weekly rollover may open the next one on its own.
 */
@Schema(description = "The campaign's lifecycle: every run and the automatic-renewal setting.")
public record CampaignAdminResponse(

    boolean autoRenewEnabled,
    List<CampaignRunSummary> runs
) {
    /**
     * One run of the campaign, current or closed.
     *
     * @param id          internal run identifier
     * @param firstDay    first day of the run
     * @param finalDay    the run's own settlement day while it is running or ran its full course, or
     *                    the day it was stopped early
     * @param rosterSize  active players the run was frozen with
     * @param status      the run's own place in its lifecycle
     * @param score       population as of {@code finalDay} — the run's score once closed, its
     *                    standing so far while still running
     */
    @Schema(description = "One run of the campaign, current or closed.")
    public record CampaignRunSummary(

        Long id,
        LocalDate firstDay,
        LocalDate finalDay,
        int rosterSize,
        CampaignRunStatus status,
        int score
    ) {
    }

    /**
     * Creates an immutable campaign admin response.
     */
    public CampaignAdminResponse {
        runs = List.copyOf(runs);
    }
}
