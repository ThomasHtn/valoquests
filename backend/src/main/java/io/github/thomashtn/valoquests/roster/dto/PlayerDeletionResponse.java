package io.github.thomashtn.valoquests.roster.dto;

import io.github.thomashtn.valoquests.roster.model.PlayerDeletionOutcome;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Reports what a deletion request did to a player.
 *
 * <p>The caller cannot predict the outcome: a player that was on any campaign roster is archived instead.
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
