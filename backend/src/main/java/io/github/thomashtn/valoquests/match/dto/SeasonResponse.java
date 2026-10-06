package io.github.thomashtn.valoquests.match.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Exposes one season available for filtering a player's match history.
 *
 * @param id     internal season identifier
 * @param name   season short name, such as {@code e11a4} or {@code v26a4}
 * @param active whether this is the season currently in progress
 */
@Schema(description = "Season available for filtering match history.")
public record SeasonResponse(

    Long id,
    String name,
    boolean active
) {
}
