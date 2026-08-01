package io.github.thomashtn.valorant.tracker.match.dto;

import io.github.thomashtn.valorant.tracker.match.model.GameMode;
import io.github.thomashtn.valorant.tracker.match.model.GameModeSource;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Exposes the outcome of a manual match correction.
 *
 * @param id             internal match identifier
 * @param gameMode       game mode after the correction
 * @param gameModeSource always {@code MANUALLY_CORRECTED} after a successful correction
 */
@Schema(description = "Match state after a manual correction.")
public record MatchCorrectionResponse(

    Long id,
    GameMode gameMode,
    GameModeSource gameModeSource
) {
}
