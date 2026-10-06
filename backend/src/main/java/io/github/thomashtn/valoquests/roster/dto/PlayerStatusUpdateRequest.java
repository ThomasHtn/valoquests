package io.github.thomashtn.valoquests.roster.dto;

import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Carries the lifecycle status a tracked player must move to.
 *
 * <p>Moving an archived player back to {@code ACTIVE} or {@code INACTIVE} restores it with its history.
 *
 * @param status status to apply
 */
@Schema(description = "Lifecycle status to apply to a tracked player.")
public record PlayerStatusUpdateRequest(

    @NotNull
    PlayerStatus status
) {
}
