package io.github.thomashtn.valoquests.shared.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.DispatcherType;
import java.time.Clock;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

/**
 * Configures stateless HTTP security and CORS rules for the application.
 *
 * <p>Every {@code GET /api/**} is public, anything else is denied; API docs are off by default. Admin
 * routes are authenticated by {@link AdminApiKeyFilter} and authorized here on the shared path pattern.
 */
@Configuration
public class SecurityConfig {

    /**
     * Builds the application security filter chain.
     *
     * @param http                 Spring Security HTTP configuration
     * @param properties           application-level configuration properties
     * @param adminAuthRateLimiter throttle applied to repeated invalid admin-key attempts
     * @param objectMapper         application JSON mapper, used for admin-key refusals
     * @param clock                application clock
     * @return the configured security filter chain
     * @throws Exception when Spring Security cannot build the chain
     */
    @Bean
    @SuppressFBWarnings(
        value = "THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION",
        justification = """
            Spring Security's HttpSecurity configuration and build API declares
            Exception. Narrowing the checked exception is not possible without
            wrapping framework exceptions and losing their original semantics.
            """
    )
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        ApplicationProperties properties,
        AdminAuthRateLimiter adminAuthRateLimiter,
        ObjectMapper objectMapper,
        Clock clock
    ) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(
                corsConfigurationSource(properties.frontendOrigin())
            ))
            .sessionManagement(session -> session.sessionCreationPolicy(
                SessionCreationPolicy.STATELESS
            ))
            .authorizeHttpRequests(authorize -> {
                // Denying the /error dispatch would turn container errors into an empty 403.
                authorize.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll();

                authorize
                    // Must stay ahead of the public GET rule, which would open admin reads.
                    .requestMatchers(AdminApiKeyFilter.ADMIN_PATH_PATTERN)
                    .hasAuthority(AdminApiKeyFilter.ADMIN_ROLE)
                    .requestMatchers("/actuator/health", "/actuator/info").permitAll();

                // Same flag as springdoc, so no standing exception remains when the docs are off.
                if (properties.apiDocsEnabled()) {
                    authorize.requestMatchers(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/api-docs",
                        "/api-docs/**"
                    ).permitAll();
                }

                authorize
                    .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                    .anyRequest().denyAll();
            })
            .addFilterBefore(
                new AdminApiKeyFilter(
                    properties.adminApiKey(),
                    adminAuthRateLimiter,
                    objectMapper,
                    clock
                ),
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    /**
     * Creates the CORS configuration used by the Angular frontend.
     *
     * <p>Credentials are deliberately not allowed: the key travels in a header and the API sets no cookie.
     *
     * @param frontendOrigin allowed frontend origin
     * @return a URL-based CORS configuration source
     */
    private CorsConfigurationSource corsConfigurationSource(String frontendOrigin) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendOrigin));
        configuration.setAllowedMethods(
            List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );
        configuration.setAllowedHeaders(
            List.of("Content-Type", "Authorization", AdminApiKeyFilter.HEADER_NAME)
        );

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
