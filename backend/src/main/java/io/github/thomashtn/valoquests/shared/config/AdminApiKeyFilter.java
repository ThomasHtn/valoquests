package io.github.thomashtn.valoquests.shared.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.shared.exception.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Protects administrative routes with a static API key supplied through an
 * HTTP header.
 *
 * <p>This filter only <em>authenticates</em>: a valid key grants
 * {@link #ADMIN_ROLE} in the security context, and the actual access decision is left to
 * {@link SecurityConfig}, which requires that role on {@link #ADMIN_PATH_PATTERN}. Keeping the two
 * concerns apart is what lets the filter answer with the precise problem responses this API
 * documents (401 for a missing header, 403 for a wrong key, 429 once an address is locked out)
 * while Spring Security still refuses anything that reaches an administrative handler without
 * having passed here.</p>
 */
public class AdminApiKeyFilter extends OncePerRequestFilter {

    /**
     * Header expected on every administrative request.
     */
    public static final String HEADER_NAME = "X-Admin-Key";

    /**
     * Path pattern covering every administrative route.
     *
     * <p>Shared with {@link SecurityConfig} on purpose: the filter and the authorization rules must
     * agree on what "administrative" means, or one of them ends up guarding a different set of
     * routes than the other.</p>
     */
    public static final String ADMIN_PATH_PATTERN = "/api/admin/**";

    /**
     * Authority granted to a request carrying a valid administrator key.
     */
    public static final String ADMIN_ROLE = "ROLE_ADMIN";

    /**
     * Matches administrative routes the way Spring MVC resolves handler mappings.
     *
     * <p>Deliberately not a {@code getRequestURI().startsWith(...)} test. The request URI is the
     * raw, still percent-encoded target, while both Spring MVC and Spring Security match on the
     * decoded path — so {@code /api/%61dmin/players} reaches the administrative controller yet
     * fails a raw prefix comparison, which would skip this filter entirely.</p>
     */
    private static final RequestMatcher ADMIN_ROUTES =
        PathPatternRequestMatcher.withDefaults().matcher(ADMIN_PATH_PATTERN);

    /**
     * Administrative API key expected in protected requests.
     */
    private final String expectedApiKey;

    /**
     * Throttles repeated invalid-key attempts per remote address.
     */
    private final AdminAuthRateLimiter rateLimiter;

    /**
     * Mapper writing refusals in the same format as every other API error.
     */
    private final ObjectMapper objectMapper;

    /**
     * Clock stamping each refusal.
     */
    private final Clock clock;

    /**
     * Creates an administrative API key filter.
     *
     * @param expectedApiKey configured key used to validate incoming requests
     * @param rateLimiter    throttle applied to repeated invalid-key attempts
     * @param objectMapper   application JSON mapper
     * @param clock          application clock
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public AdminApiKeyFilter(
        String expectedApiKey,
        AdminAuthRateLimiter rateLimiter,
        ObjectMapper objectMapper,
        Clock clock
    ) {
        this.expectedApiKey = expectedApiKey;
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * Determines whether the current request is outside the administrative API.
     *
     * @param request current HTTP request
     * @return {@code true} when the filter must be skipped
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !ADMIN_ROUTES.matches(request);
    }

    /**
     * Prevents the filter from executing again during an ERROR dispatch.
     *
     * <p>
     * Once the request has been authenticated, the filter must not run again
     * while Spring renders an error response. Otherwise the original HTTP
     * status (for example 404) may be replaced by a new authentication error.
     * </p>
     *
     * @return always {@code true}
     */
    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return true;
    }

    /**
     * Validates the administrative key before continuing the filter chain.
     *
     * @param request     current HTTP request
     * @param response    current HTTP response
     * @param filterChain remaining servlet filter chain
     * @throws ServletException when the filter chain fails
     * @throws IOException      when the error response cannot be written
     */
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String remoteAddress = request.getRemoteAddr();

        if (rateLimiter.isLockedOut(remoteAddress)) {
            writeProblemResponse(
                request,
                response,
                HttpStatus.TOO_MANY_REQUESTS,
                "ADMIN_KEY_RATE_LIMITED",
                "Too many invalid administrator key attempts. Try again later."
            );
            return;
        }

        String providedApiKey = request.getHeader(HEADER_NAME);

        if (providedApiKey == null) {
            writeProblemResponse(
                request,
                response,
                HttpStatus.UNAUTHORIZED,
                "ADMIN_KEY_MISSING",
                "The X-Admin-Key header is required."
            );
            return;
        }

        if (!matchesExpectedKey(providedApiKey)) {
            rateLimiter.recordFailure(remoteAddress);
            writeProblemResponse(
                request,
                response,
                HttpStatus.FORBIDDEN,
                "ADMIN_KEY_INVALID",
                "The administrator key is invalid."
            );
            return;
        }

        rateLimiter.recordSuccess(remoteAddress);
        authenticateAsAdministrator();
        filterChain.doFilter(request, response);
    }

    /**
     * Marks the current request as coming from the administrator.
     *
     * <p>The key itself is never stored as credentials: the security context is exposed to
     * downstream components and to error reporting, and the administrator key is the single secret
     * protecting every write operation in this API.</p>
     */
    private void authenticateAsAdministrator() {
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
            "administrator",
            null,
            List.of(new SimpleGrantedAuthority(ADMIN_ROLE))
        );

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    /**
     * Compares the supplied key against the configured one in constant time.
     *
     * <p>A plain {@code equals} would return as soon as two bytes differ, letting a caller time
     * repeated requests to recover the key one character at a time.
     *
     * @param providedApiKey key supplied by the caller
     * @return {@code true} when both keys are equal
     */
    private boolean matchesExpectedKey(String providedApiKey) {
        return MessageDigest.isEqual(
            providedApiKey.getBytes(StandardCharsets.UTF_8),
            expectedApiKey.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Writes the refusal as an {@link ApiErrorResponse}, like any other API error.
     *
     * @param request  current HTTP request
     * @param response current HTTP response
     * @param status   HTTP status
     * @param code     application error code
     * @param detail   human-readable error detail
     * @throws IOException when the response body cannot be written
     */
    private void writeProblemResponse(
        HttpServletRequest request,
        HttpServletResponse response,
        HttpStatus status,
        String code,
        String detail
    ) throws IOException {
        ApiErrorResponse body = new ApiErrorResponse(
            URI.create("about:blank"),
            status.getReasonPhrase(),
            status.value(),
            code,
            detail,
            URI.create(request.getRequestURI()),
            clock.instant(),
            Map.of()
        );

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
