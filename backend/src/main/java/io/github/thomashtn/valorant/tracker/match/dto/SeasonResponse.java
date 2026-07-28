package io.github.thomashtn.valorant.tracker.match.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Exposes one season available for filtering a player's match history.
 */
@Schema(description = "Season available for filtering match history.")
public record SeasonResponse(

    Long id,
    String name,
    boolean active
) {
}
