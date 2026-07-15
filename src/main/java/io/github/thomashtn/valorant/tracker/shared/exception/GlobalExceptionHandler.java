package io.github.thomashtn.valorant.tracker.shared.exception;

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

/**
 * Converts application exceptions into consistent HTTP problem responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles requests targeting an unknown application resource.
     *
     * @param exception raised resource-not-found exception
     * @param request current HTTP request
     * @return a standardized HTTP 404 response
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
     * @return a standardized HTTP 400 response
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
     * @return a complete error response entity
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

    /**
     * Converts an intentionally unimplemented application feature into HTTP 501.
     *
     * @param exception exception raised by a controller without a service implementation
     * @param request current HTTP request
     * @return structured API error response
     */
    @ExceptionHandler(FeatureNotImplementedException.class)
    public ResponseEntity<ApiErrorResponse> handleFeatureNotImplemented(
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

}
