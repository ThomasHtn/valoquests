package io.github.thomashtn.valoquests.roster.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Carries the editable identity of an already tracked player.
 *
 * <p>The status has its own route, so an identity fix never moves a player in or out of the competition.
 *
 * @param gameName    Riot game name
 * @param tagLine     Riot tag line
 * @param displayName name shown in the application
 * @param portrait    agent portrait, {@code null} for none
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
