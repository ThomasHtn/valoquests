package io.github.thomashtn.valoquests.match.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * Groups the optional filters a caller may apply to a player's match history.
 *
 * <p>Values arrive exactly as the caller wrote them, so this carries raw request text rather than
 * parsed types: rejecting an unknown map or game mode stays the query service's job, and keeping
 * the parsing there is what lets an invalid value answer 400 instead of surfacing as a binding
 * failure the API cannot describe.
 *
 * <p>A {@code null} component means "no filter on this field", never "match nothing".
 *
 * @param seasonId internal season identifier, or {@code null} for every season
 * @param map      map name, or {@code null} for every map
 * @param agent    agent name, or {@code null} for every agent
 * @param result    match outcome name, or {@code null} for every outcome
 * @param gameMode  game mode name, or {@code null} for every mode
 * @param weekStart Monday restricting matches to that calendar week, or {@code null} for every week
 */
public record MatchHistoryFilter(

    @Schema(description = "Optional internal season identifier.", example = "8")
    Long seasonId,

    @Schema(description = "Optional exact map name.", example = "Ascent")
    String map,

    @Schema(description = "Optional exact agent name.", example = "Omen")
    String agent,

    @Schema(description = "Optional result filter: WIN, LOSS or DRAW.", example = "WIN")
    String result,

    @Schema(description = "Optional game mode filter.", example = "COMPETITIVE")
    String gameMode,

    @Schema(description = "Optional Monday restricting matches to that calendar week.", example = "2026-07-27")
    LocalDate weekStart
) {

    /**
     * A filter that excludes nothing.
     */
    public static final MatchHistoryFilter NONE =
        new MatchHistoryFilter(null, null, null, null, null, null);
}
