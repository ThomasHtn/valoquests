package io.github.thomashtn.valorant.tracker.ranking.controller;

import static io.github.thomashtn.valorant.tracker.shared.config.OpenApiConfig.ADMIN_KEY_SECURITY_SCHEME;

import io.github.thomashtn.valorant.tracker.ranking.service.RankingRecalculationService;
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
     * @param rankingRecalculationService ranking recalculation service
     */
    public RankingAdminController(
        RankingRecalculationService rankingRecalculationService
    ) {
        this.rankingRecalculationService = rankingRecalculationService;
    }

    /**
     * Recalculates scores and positions without recalculating challenge progress.
     */
    @PostMapping("/recalculation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Recalculate the current weekly ranking",
        description = """
            Rebuilds points, positions, completed-challenge counters and position variations from
            stored challenge progress. This operation never contacts the Henrik API.
            """
    )
    @ApiResponse(responseCode = "204", description = "Ranking recalculated successfully.")
    public void recalculateRanking() {
        rankingRecalculationService.recalculateCurrentRanking();
    }
}
