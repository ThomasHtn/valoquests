package io.github.thomashtn.valoquests.roster.dto;

import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Carries the identity of a player being added to the roster.
 *
 * <p>Sizes mirror the {@code player} table so an over-long value fails validation, not in the database.
 *
 * @param gameName    Riot game name
 * @param tagLine     Riot tag line
 * @param displayName name shown in the application
 * @param portrait    agent portrait, {@code null} for none
 * @param status      initial lifecycle status
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
