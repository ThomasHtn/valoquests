package io.github.thomashtn.valoquests.henrik.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents the common error payload returned by the HenrikDev API.
 *
 * @param message human-readable external error description
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HenrikErrorResponse(

    String message
) {
}
