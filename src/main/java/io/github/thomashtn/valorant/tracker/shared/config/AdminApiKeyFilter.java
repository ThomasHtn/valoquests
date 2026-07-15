package io.github.thomashtn.valorant.tracker.shared.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Protects administrative routes with a static API key supplied through an HTTP header.
 */
public class AdminApiKeyFilter extends OncePerRequestFilter {

    /** Header expected on every administrative request. */
    public static final String HEADER_NAME = "X-Admin-Key";

    private final String expectedApiKey;

    /**
     * Creates an administrative API key filter.
     *
     * @param expectedApiKey configured key used to validate incoming requests
     */
    public AdminApiKeyFilter(String expectedApiKey) {
        this.expectedApiKey = expectedApiKey;
    }

    /**
     * Determines whether the current request is outside the administrative API.
     *
     * @param request current HTTP request
     * @return {@code true} when the filter must be skipped
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/admin");
    }

    /**
     * Validates the administrative key before continuing the filter chain.
     *
     * @param request current HTTP request
     * @param response current HTTP response
     * @param filterChain remaining servlet filter chain
     * @throws ServletException when the filter chain fails
     * @throws IOException when the error response cannot be written
     */
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String providedApiKey = request.getHeader(HEADER_NAME);

        if (providedApiKey == null) {
            writeProblemResponse(
                response,
                HttpServletResponse.SC_UNAUTHORIZED,
                "ADMIN_KEY_MISSING",
                "The X-Admin-Key header is required."
            );
            return;
        }

        if (!keysMatch(providedApiKey, expectedApiKey)) {
            writeProblemResponse(
                response,
                HttpServletResponse.SC_FORBIDDEN,
                "ADMIN_KEY_INVALID",
                "The administrator key is invalid."
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Compares two API keys using a constant-time byte comparison.
     *
     * @param providedApiKey key supplied by the caller
     * @param expectedApiKey key configured by the application
     * @return {@code true} when both keys are equal
     */
    private boolean keysMatch(String providedApiKey, String expectedApiKey) {
        return MessageDigest.isEqual(
            providedApiKey.getBytes(StandardCharsets.UTF_8),
            expectedApiKey.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Writes a minimal RFC 7807-compatible problem response.
     *
     * @param response current HTTP response
     * @param status HTTP status code
     * @param code application error code
     * @param detail human-readable error detail
     * @throws IOException when the response body cannot be written
     */
    private void writeProblemResponse(
        HttpServletResponse response,
        int status,
        String code,
        String detail
    ) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("""
            {
              "type": "about:blank",
              "title": "Unauthorized administration request",
              "status": %d,
              "code": "%s",
              "detail": "%s"
            }
            """.formatted(status, code, detail));
    }
}
