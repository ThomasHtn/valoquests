package io.github.thomashtn.valoquests.shared.exception;

import io.github.thomashtn.valoquests.henrik.exception.HenrikApiException;
import io.github.thomashtn.valoquests.henrik.exception.HenrikRateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Converts application exceptions into consistent HTTP problem responses.
 *
 * <p>The standard Spring MVC exceptions keep the status Spring assigns them through
 * {@link ResponseEntityExceptionHandler}; only their body is rewritten into {@link ApiErrorResponse}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Logger used to report operational and diagnostic information.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Clock stamping each error response.
     */
    private final Clock clock;

    /**
     * Creates the exception handler.
     *
     * @param clock application clock
     */
    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

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
     * Handles a request value the API rejects.
     *
     * <p>Only this exception yields a 400 with its message; a bare {@link IllegalArgumentException} stays a 500.
     *
     * @param exception invalid-request exception
     * @param request current HTTP request
     * @return standardized HTTP 400 response
     */
    @ExceptionHandler(InvalidRequestException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidRequest(
        InvalidRequestException exception,
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
     * Handles a request refused because it conflicts with the application's current state.
     *
     * @param exception conflict exception
     * @param request current HTTP request
     * @return standardized HTTP 409 response
     */
    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ApiErrorResponse> handleConflict(
        ConflictException exception,
        HttpServletRequest request
    ) {
        return buildResponse(
            HttpStatus.CONFLICT,
            "CONFLICT",
            exception.getMessage(),
            request,
            Map.of()
        );
    }

    /**
     * Handles a background task refused because the administrative executor is already full.
     *
     * <p>Answered like a {@link ConflictException}: another run is in the way.
     *
     * @param exception rejection raised by the executor
     * @param request current HTTP request
     * @return standardized HTTP 409 response
     */
    @ExceptionHandler(TaskRejectedException.class)
    ResponseEntity<ApiErrorResponse> handleTaskRejected(
        TaskRejectedException exception,
        HttpServletRequest request
    ) {
        LOGGER.warn("Background task rejected for {} {}", request.getMethod(),
            request.getRequestURI(), exception);

        return buildResponse(
            HttpStatus.CONFLICT,
            "CONFLICT",
            "Another background task is already running. Retry once it has finished.",
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
            "The upstream provider rate limit was reached. Retry later.",
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
            "The upstream provider could not be reached.",
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
     * Renders the standard Spring MVC exceptions (unreadable body, unsupported media type, unknown
     * route, failed validation...) with the status Spring chose, in the API error format.
     *
     * <p>Server-side failures among them are logged and their detail is hidden, as in the catch-all.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
        Exception exception,
        @Nullable Object body,
        HttpHeaders headers,
        HttpStatusCode statusCode,
        WebRequest request
    ) {
        String requestUri = ((ServletWebRequest) request).getRequest().getRequestURI();
        ApiErrorResponse errorBody = statusCode.is5xxServerError()
            ? unexpectedErrorBody(exception, HttpStatus.valueOf(statusCode.value()), requestUri)
            : clientErrorBody(exception, body, statusCode, requestUri);

        return super.handleExceptionInternal(exception, errorBody, headers, statusCode, request);
    }

    /**
     * Describes a caller mistake detected by Spring MVC before reaching a controller.
     *
     * @param exception  standard MVC exception
     * @param body       problem detail Spring prepared for it, if any
     * @param statusCode status Spring chose
     * @param requestUri path of the current request
     * @return error payload
     */
    private ApiErrorResponse clientErrorBody(
        Exception exception,
        @Nullable Object body,
        HttpStatusCode statusCode,
        String requestUri
    ) {
        HttpStatus status = HttpStatus.valueOf(statusCode.value());

        return switch (exception) {
            case MethodArgumentNotValidException invalid -> buildBody(
                status,
                "VALIDATION_FAILED",
                "One or more fields are invalid.",
                requestUri,
                fieldErrors(invalid)
            );
            // The raw value is not echoed back, only the offending parameter is named.
            case MethodArgumentTypeMismatchException mismatch -> buildBody(
                status,
                "INVALID_ARGUMENT",
                "Parameter '" + mismatch.getName() + "' has an invalid value.",
                requestUri,
                Map.of()
            );
            case NoResourceFoundException ignored -> buildBody(
                status,
                "RESOURCE_NOT_FOUND",
                "The requested resource does not exist.",
                requestUri,
                Map.of()
            );
            case HttpRequestMethodNotSupportedException unsupported -> buildBody(
                status,
                "METHOD_NOT_ALLOWED",
                "The " + unsupported.getMethod() + " method is not supported by this resource.",
                requestUri,
                Map.of()
            );
            default -> buildBody(
                status,
                status == HttpStatus.BAD_REQUEST ? "INVALID_ARGUMENT" : status.name(),
                body instanceof ProblemDetail problem && problem.getDetail() != null
                    ? problem.getDetail()
                    : status.getReasonPhrase(),
                requestUri,
                Map.of()
            );
        };
    }

    /**
     * Collects the first validation message of each invalid field.
     *
     * @param exception validation exception containing field errors
     * @return messages indexed by field name, in binding order
     */
    private static Map<String, String> fieldErrors(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();

        exception.getBindingResult()
            .getFieldErrors()
            .forEach(fieldError -> errors.putIfAbsent(
                fieldError.getField(),
                fieldError.getDefaultMessage()
            ));

        return errors;
    }

    /**
     * Logs an unexpected failure and describes it without leaking its message.
     *
     * @param exception  unexpected exception
     * @param status     server-side status Spring chose
     * @param requestUri path of the current request
     * @return error payload carrying that status
     */
    private ApiErrorResponse unexpectedErrorBody(
        Exception exception,
        HttpStatus status,
        String requestUri
    ) {
        LOGGER.error("Unexpected error while processing {}", requestUri, exception);

        return buildBody(
            status,
            "INTERNAL_ERROR",
            "An unexpected error occurred.",
            requestUri,
            Map.of()
        );
    }

    /**
     * Builds the common API error response returned by exception handlers.
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
        return ResponseEntity.status(status)
            .body(buildBody(status, code, detail, request.getRequestURI(), errors));
    }

    /**
     * Builds the common API error payload.
     *
     * @param status HTTP status
     * @param code application-specific error code
     * @param detail human-readable error detail
     * @param requestUri path of the current request
     * @param errors optional validation errors indexed by field name
     * @return error payload
     */
    private ApiErrorResponse buildBody(
        HttpStatus status,
        String code,
        String detail,
        String requestUri,
        Map<String, String> errors
    ) {
        return new ApiErrorResponse(
            URI.create("about:blank"),
            status.getReasonPhrase(),
            status.value(),
            code,
            detail,
            URI.create(requestUri),
            clock.instant(),
            errors
        );
    }
}
