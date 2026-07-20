package io.github.thomashtn.valorant.tracker.shared.exception;

import io.github.thomashtn.valorant.tracker.henrik.exception.HenrikApiException;
import io.github.thomashtn.valorant.tracker.henrik.exception.HenrikRateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts application exceptions into consistent HTTP problem responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Logger used to report operational and diagnostic information.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles requests targeting an unknown application resource.
     *
     * @param exception raised resource-not-found exception
     * @param request current HTTP request
     * @return standardized HTTP 404 response
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleResourceNotFound(
        ResourceNotFoundException exception,
        HttpServletRequest request
    ) {
        return buildResponse(
            HttpStatus.NOT_FOUND,
            "RESOURCE_NOT_FOUND",
            exception.getMessage(),
            request,
            Map.of()
        );
    }

    /**
     * Handles validation failures produced while binding request data.
     *
     * @param exception validation exception containing field errors
     * @param request current HTTP request
     * @return standardized HTTP 400 response
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidationFailure(
        MethodArgumentNotValidException exception,
        HttpServletRequest request
    ) {
        Map<String, String> errors = new LinkedHashMap<>();

        exception.getBindingResult()
            .getFieldErrors()
            .forEach(fieldError -> errors.putIfAbsent(
                fieldError.getField(),
                fieldError.getDefaultMessage()
            ));

        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_FAILED",
            "One or more fields are invalid.",
            request,
            errors
        );
    }

    /**
     * Handles invalid request parameters detected by application services.
     *
     * @param exception invalid-argument exception
     * @param request current HTTP request
     * @return standardized HTTP 400 response
     */
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiErrorResponse> handleIllegalArgument(
        IllegalArgumentException exception,
        HttpServletRequest request
    ) {
        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "INVALID_ARGUMENT",
            exception.getMessage(),
            request,
            Map.of()
        );
    }

    /**
     * Handles Henrik rate-limit failures.
     *
     * @param exception rate-limit exception
     * @param request current HTTP request
     * @return HTTP 429 response
     */
    @ExceptionHandler(HenrikRateLimitException.class)
    ResponseEntity<ApiErrorResponse> handleHenrikRateLimit(
        HenrikRateLimitException exception,
        HttpServletRequest request
    ) {
        LOGGER.warn(
            "Henrik rate limit reached while processing {} {}: {}",
            request.getMethod(),
            request.getRequestURI(),
            exception.getMessage()
        );

        return buildResponse(
            HttpStatus.TOO_MANY_REQUESTS,
            "HENRIK_RATE_LIMIT_EXCEEDED",
            exception.getMessage(),
            request,
            Map.of()
        );
    }

    /**
     * Handles Henrik API communication failures.
     *
     * @param exception Henrik API exception
     * @param request current HTTP request
     * @return HTTP 502 response
     */
    @ExceptionHandler(HenrikApiException.class)
    ResponseEntity<ApiErrorResponse> handleHenrikApiFailure(
        HenrikApiException exception,
        HttpServletRequest request
    ) {
        LOGGER.error(
            "Henrik API failure while processing {} {}",
            request.getMethod(),
            request.getRequestURI(),
            exception
        );

        return buildResponse(
            HttpStatus.BAD_GATEWAY,
            "HENRIK_API_ERROR",
            exception.getMessage(),
            request,
            Map.of()
        );
    }

    /**
     * Converts an intentionally unimplemented application feature into HTTP
     * 501.
     *
     * @param exception exception raised for an unfinished feature
     * @param request current HTTP request
     * @return standardized HTTP 501 response
     */
    @ExceptionHandler(FeatureNotImplementedException.class)
    ResponseEntity<ApiErrorResponse> handleFeatureNotImplemented(
        FeatureNotImplementedException exception,
        HttpServletRequest request
    ) {
        return buildResponse(
            HttpStatus.NOT_IMPLEMENTED,
            "FEATURE_NOT_IMPLEMENTED",
            exception.getMessage(),
            request,
            Map.of()
        );
    }

    /**
     * Handles unexpected exceptions not covered by a more specific handler.
     *
     * @param exception unexpected exception
     * @param request current HTTP request
     * @return a standardized HTTP 500 response
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnexpectedException(
        Exception exception,
        HttpServletRequest request
    ) {
        LOGGER.error(
            "Unexpected error while processing {} {}",
            request.getMethod(),
            request.getRequestURI(),
            exception
        );

        return buildResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_ERROR",
            "An unexpected error occurred.",
            request,
            Map.of()
        );
    }

    /**
     * Builds the common API error payload returned by exception handlers.
     *
     * @param status HTTP status
     * @param code application-specific error code
     * @param detail human-readable error detail
     * @param request current HTTP request
     * @param errors optional validation errors indexed by field name
     * @return complete error response entity
     */
    private ResponseEntity<ApiErrorResponse> buildResponse(
        HttpStatus status,
        String code,
        String detail,
        HttpServletRequest request,
        Map<String, String> errors
    ) {
        ApiErrorResponse body = new ApiErrorResponse(
            URI.create("about:blank"),
            status.getReasonPhrase(),
            status.value(),
            code,
            detail,
            URI.create(request.getRequestURI()),
            Instant.now(),
            errors
        );

        return ResponseEntity.status(status).body(body);
    }
}
