package io.github.thomashtn.valoquests.ranking.controller;

import static io.github.thomashtn.valoquests.shared.config.OpenApiConfig.ADMIN_KEY_SECURITY_SCHEME;

import io.github.thomashtn.valoquests.ranking.service.RankingRecalculationService;
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
 * Exposes the protected ranking-only recalculation operation.
 */
@RestController
@RequestMapping("/api/admin/rankings")
@Tag(name = "Administration - Rankings", description = "Manual weekly-ranking maintenance.")
@SecurityRequirement(name = ADMIN_KEY_SECURITY_SCHEME)
public class RankingAdminController {

    /**
     * Service used to rebuild the current weekly ranking.
     */
    private final RankingRecalculationService rankingRecalculationService;

    /**
     * Lock keeping the recalculation from overlapping another job writing the match history.
     */
    private final MatchHistoryLock matchHistoryLock;

    /**
     * Creates the ranking administration controller.
     *
     * @param rankingRecalculationService ranking recalculation service
     * @param matchHistoryLock            lock shared by every job writing the match history
     */
    public RankingAdminController(
        RankingRecalculationService rankingRecalculationService,
        MatchHistoryLock matchHistoryLock
    ) {
        this.rankingRecalculationService = rankingRecalculationService;
        this.matchHistoryLock = matchHistoryLock;
    }

    /**
     * Recalculates scores and positions without recalculating challenge progress.
     */
    @PostMapping("/recalculation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Recalculate the current weekly ranking",
        description = """
            Rebuilds damage, positions, completed-challenge counters and position variations from
            stored challenge progress. This operation never contacts the Henrik API.

            Refused with a 409 while another job writing the match history runs.
            """
    )
    @ApiResponse(responseCode = "204", description = "Ranking recalculated successfully.")
    @ApiResponse(responseCode = "401", description = "X-Admin-Key header is missing.")
    @ApiResponse(responseCode = "403", description = "X-Admin-Key value is invalid.")
    @ApiResponse(
        responseCode = "409",
        description = "Another job writing the match history is already running."
    )
    public void recalculateRanking() {
        // Taken outside the recalculation transaction so it is only released once it committed.
        matchHistoryLock.runOrReject(rankingRecalculationService::recalculateCurrentRanking);
    }
}
