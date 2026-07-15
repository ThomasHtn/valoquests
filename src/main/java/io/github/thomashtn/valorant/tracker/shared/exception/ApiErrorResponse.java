package io.github.thomashtn.valorant.tracker.shared.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

/**
 * Represents the API response payload for api error response.
 */
@Schema(description = "API response model documented by the Valorant Tracker OpenAPI specification.")
public record ApiErrorResponse(
    URI type,
    String title,
    int status,
    String code,
    String detail,
    URI instance,
    Instant timestamp,
    Map<String, String> errors
) {
}
