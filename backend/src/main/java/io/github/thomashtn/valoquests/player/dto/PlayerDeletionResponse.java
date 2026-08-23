package io.github.thomashtn.valoquests.player.dto;

import io.github.thomashtn.valoquests.player.model.PlayerDeletionOutcome;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Reports what a deletion request did to a player.
 *
 * <p>The two outcomes are not interchangeable, and the caller cannot predict which one it will get:
 * a player that fought a boss is archived rather than deleted. Saying so explicitly is what lets
 * the administration screen tell the operator the roster entry is gone but recoverable, instead of
 * claiming a deletion that did not happen.
 */
@Schema(description = "Outcome of a player deletion request.")
public record PlayerDeletionResponse(

    Long playerId,
    PlayerDeletionOutcome outcome
) {
}
