package io.github.thomashtn.valoquests.shared.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;
import java.time.Instant;
import java.util.Map;

/**
 * Represents the problem-details payload returned for API failures.
 *
 * @param type      problem type, always {@code about:blank}
 * @param title     HTTP reason phrase
 * @param status    HTTP status code
 * @param code      application error code
 * @param detail    human-readable detail, safe to show the caller
 * @param instance  path of the failed request
 * @param timestamp instant the error was produced
 * @param errors    validation messages indexed by field name, empty when none
 */
@Schema(description = "Standard API problem response.")
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
    /**
     * Creates an immutable API error response.
     */
    public ApiErrorResponse {
        errors = Map.copyOf(errors);
    }

}
