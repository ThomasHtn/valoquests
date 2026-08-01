package io.github.thomashtn.valorant.tracker.match.dto;

import io.github.thomashtn.valorant.tracker.match.model.GameMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Requests a manual override of a match's game mode.
 *
 * @param gameMode mode to apply, recorded as {@code MANUALLY_CORRECTED}
 */
@Schema(description = "Manual game-mode correction for one match.")
public record GameModeCorrectionRequest(

    @NotNull
    GameMode gameMode
) {
}
