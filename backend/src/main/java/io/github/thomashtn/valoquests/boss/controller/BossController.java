package io.github.thomashtn.valoquests.boss.controller;

import io.github.thomashtn.valoquests.boss.dto.BossHistoryWeekResponse;
import io.github.thomashtn.valoquests.boss.dto.CurrentBossResponse;
import io.github.thomashtn.valoquests.boss.service.BossQueryService;
import io.github.thomashtn.valoquests.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the active weekly boss confrontation and finalized confrontation history.
 */
@RestController
@RequestMapping(value = "/api/boss", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Boss", description = "Weekly boss confrontation and its finalized history.")
public class BossController {

    /**
     * Service used to query current and historical boss confrontations.
     */
    private final BossQueryService bossQueryService;

    /**
     * @param bossQueryService boss query service
     */
    public BossController(BossQueryService bossQueryService) {
        this.bossQueryService = bossQueryService;
    }

    /**
     * @return active-week boss confrontation
     */
    @GetMapping("/current")
    @Operation(
        summary = "Get the current weekly boss",
        description = """
            Returns the boss drawn for the active calendar week, its effective hit points, the
            cumulative damage dealt so far by every active player, and the win streak entering this
            week's fight.
            """
    )
    @ApiResponse(responseCode = "200", description = "Current boss confrontation returned successfully.")
    public CurrentBossResponse getCurrentBoss() {
        return bossQueryService.findCurrent();
    }

    /**
     * @return one page of finalized weekly boss confrontations
     *
     * @param page zero-based page index
     * @param size number of finalized weeks returned per page
     */
    @GetMapping("/history")
    @Operation(
        summary = "Get finalized weekly boss confrontations",
        description = """
            Returns a page of immutable boss confrontations for completed calendar weeks, ordered from
            the most recent week to the oldest one. Pagination applies to weeks rather than players.
            """
    )
    @ApiResponse(responseCode = "200", description = "Boss history returned successfully.")
    @ApiResponse(responseCode = "400", description = "Pagination values are invalid.")
    public PageResponse<BossHistoryWeekResponse> getBossHistory(
        @Parameter(description = "Zero-based page index.", example = "0")
        @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Number of finalized weeks returned per page.", example = "10")
        @RequestParam(defaultValue = "10") int size
    ) {
        return bossQueryService.findHistory(page, size);
    }
}
