package io.github.thomashtn.valorant.tracker.henrik.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the common error payload returned by the HenrikDev API.
 *
 * <p>Unknown fields are ignored because the external API may add properties
 * without requiring an application update.</p>
 *
 * @param status external HTTP-like status, when available
 * @param message human-readable external error description
 * @param errors additional external error details
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HenrikErrorResponse(

    Integer status,
    String message,
    @JsonProperty("errors") Object errors
) {
}
