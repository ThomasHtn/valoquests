package io.github.thomashtn.valorant.tracker.player.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Carries the editable identity of an already tracked player.
 *
 * <p>The status is deliberately absent: it is changed through its own route, so an identity
 * correction can never move a player in or out of the competition as a side effect.
 */
@Schema(description = "Editable identity of a tracked player.")
public record PlayerUpdateRequest(

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
    String portrait
) {
}
