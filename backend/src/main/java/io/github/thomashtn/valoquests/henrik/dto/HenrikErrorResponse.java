package io.github.thomashtn.valoquests.henrik.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents the common error payload returned by the HenrikDev API.
 *
 * <p>Unknown fields are ignored because the external API may add properties
 * without requiring an application update.</p>
 *
 * @param message human-readable external error description
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HenrikErrorResponse(

    String message
) {
}
