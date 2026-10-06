package io.github.thomashtn.valoquests.roster.dto;

import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Carries the lifecycle status a tracked player must move to.
 *
 * <p>Also how an archived player is restored: moving it back to {@code ACTIVE} or {@code INACTIVE}
 * returns it to the roster with the history it kept while archived.
 *
 * @param status status to apply
 */
@Schema(description = "Lifecycle status to apply to a tracked player.")
public record PlayerStatusUpdateRequest(

    @NotNull
    PlayerStatus status
) {
}
