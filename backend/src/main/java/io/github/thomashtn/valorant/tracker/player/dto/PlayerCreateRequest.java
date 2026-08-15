package io.github.thomashtn.valorant.tracker.player.dto;

import io.github.thomashtn.valorant.tracker.player.model.PlayerStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Carries the identity of a player being added to the roster.
 *
 * <p>Sizes mirror the {@code player} table so an over-long value is rejected as a validation error
 * naming the field, rather than as a constraint violation from the database.
 */
@Schema(description = "Identity of a player to start tracking.")
public record PlayerCreateRequest(

    @NotBlank
    @Size(max = 32)
    String gameName,

    @NotBlank
    @Size(max = 16)
    String tagLine,

    @NotBlank
    @Size(max = 64)
    String displayName,

    @Size(max = 255)
    String portrait,

    @NotNull
    PlayerStatus status
) {
}
