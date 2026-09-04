package io.github.thomashtn.valoquests.campaign.controller;

import io.github.thomashtn.valoquests.campaign.dto.CampaignHistoryResponse;
import io.github.thomashtn.valoquests.campaign.dto.CampaignResponse;
import io.github.thomashtn.valoquests.campaign.dto.CampaignTodayResponse;
import io.github.thomashtn.valoquests.campaign.service.CampaignQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the public reading of the rescue campaign.
 */
@RestController
@RequestMapping("/api/campaign")
@Tag(name = "Campaign", description = "The squad's ten-week rescue campaign.")
public class CampaignController {

    /**
     * Service reading the campaign.
     */
    private final CampaignQueryService queryService;

    /**
     * Creates the campaign controller.
     *
     * @param queryService campaign query service
     */
    public CampaignController(CampaignQueryService queryService) {
        this.queryService = queryService;
    }

    /**
     * Returns the campaign in force, the last closed one, or an empty answer.
     *
     * @return the campaign
     */
    @GetMapping
    @Operation(
        summary = "Read the campaign",
        description = """
            Returns the campaign that is opened or running, the last closed one when there is none,
            and an answer with a null status on a database that never had one.

            Every figure comes from the last replay: the base, the two stocks, each week's guardian
            and each settled Sunday. Nothing here is computed on the fly, so refreshing the page can
            never move a total.
            """
    )
    @ApiResponse(responseCode = "200", description = "Campaign returned successfully.")
    public CampaignResponse currentCampaign() {
        return queryService.currentCampaign();
    }

    /**
     * Returns the day in progress.
     *
     * @return today
     */
    @GetMapping("/today")
    @Operation(
        summary = "Read the day in progress",
        description = """
            Returns what the roster has brought in today, operator by operator, with both
            multipliers reported and the week's honours as they stand.

            Provisional until midnight: a match imported later can push a cheaper one into a reduced
            tier and move a total that was already on screen.
            """
    )
    @ApiResponse(responseCode = "200", description = "Day returned successfully.")
    public CampaignTodayResponse today() {
        return queryService.today();
    }

    /**
     * Returns the closed campaigns.
     *
     * @return the campaign history
     */
    @GetMapping("/history")
    @Operation(
        summary = "List the closed campaigns",
        description = """
            Returns every closed campaign, most recent first, with the tier that makes two of them
            comparable and the base at the close of each settled week.
            """
    )
    @ApiResponse(responseCode = "200", description = "History returned successfully.")
    public List<CampaignHistoryResponse> history() {
        return queryService.history();
    }
}
