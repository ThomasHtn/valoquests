package io.github.thomashtn.valoquests.roster.dto;

import io.github.thomashtn.valoquests.roster.model.PlayerDeletionOutcome;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Reports what a deletion request did to a player.
 *
 * <p>The two outcomes are not interchangeable, and the caller cannot predict which one it will get:
 * a player that was on any campaign roster is archived rather than deleted. Saying so explicitly lets
 * the administration screen tell the admin the roster entry is gone but recoverable, instead of
 * claiming a deletion that did not happen.
 *
 * @param playerId internal player identifier
 * @param outcome  whether the player was deleted or archived
 */
@Schema(description = "Outcome of a player deletion request.")
public record PlayerDeletionResponse(

    Long playerId,
    PlayerDeletionOutcome outcome
) {
}
