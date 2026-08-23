package io.github.thomashtn.valoquests.match.controller;

import io.github.thomashtn.valoquests.match.dto.SeasonResponse;
import io.github.thomashtn.valoquests.match.service.SeasonQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes seasons available for filtering a player's match history.
 */
@RestController
@RequestMapping(value = "/api/seasons", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Seasons", description = "Seasons available for filtering player match history.")
public class SeasonController {

    /**
     * Service used to read persisted seasons.
     */
    private final SeasonQueryService service;

    /**
     * @param service service used to read persisted seasons
     */
    public SeasonController(SeasonQueryService service) {
        this.service = service;
    }

    /**
     * @return every known season, most recent first
     */
    @GetMapping
    @Operation(
        summary = "List known seasons",
        description = """
            Returns every season discovered from synchronized matches, most recent first, so
            clients can populate a match-history season filter.
            """
    )
    @ApiResponse(responseCode = "200", description = "Seasons returned successfully.")
        public List<SeasonResponse> getSeasons() {
        return service.findAll();
    }
}
