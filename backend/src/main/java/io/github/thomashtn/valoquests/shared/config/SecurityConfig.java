package io.github.thomashtn.valoquests.shared.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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

/**
 * Configures stateless HTTP security and CORS rules for the application.
 *
 * <p>The public site is read-only, so every {@code GET /api/**} is open while anything else is
 * denied unless a rule allows it. Administrative routes are authenticated by
 * {@link AdminApiKeyFilter} and authorized here through {@link AdminApiKeyFilter#ADMIN_ROLE}: the
 * filter reports <em>why</em> a key was refused, this chain decides <em>whether</em> a request may
 * proceed. Both steps must agree, hence the shared
 * {@link AdminApiKeyFilter#ADMIN_PATH_PATTERN}.</p>
 */
@Configuration
public class SecurityConfig {

    /**
     * Builds the application security filter chain.
     *
     * @param http                 Spring Security HTTP configuration
     * @param properties           application-level configuration properties
     * @param adminAuthRateLimiter throttle applied to repeated invalid admin-key attempts
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
        AdminAuthRateLimiter adminAuthRateLimiter
    ) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(
                corsConfigurationSource(properties.frontendOrigin())
            ))
            .sessionManagement(session -> session.sessionCreationPolicy(
                SessionCreationPolicy.STATELESS
            ))
            .authorizeHttpRequests(authorize -> authorize
                // Must stay ahead of the public GET rule below, which would otherwise open every
                // administrative read endpoint.
                .requestMatchers(AdminApiKeyFilter.ADMIN_PATH_PATTERN)
                .hasAuthority(AdminApiKeyFilter.ADMIN_ROLE)
                .requestMatchers(
                    "/actuator/health",
                    "/actuator/info",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/api-docs",
                    "/api-docs/**"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                .anyRequest().denyAll()
            )
            .addFilterBefore(
                new AdminApiKeyFilter(properties.adminApiKey(), adminAuthRateLimiter),
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    /**
     * Creates the CORS configuration used by the Angular frontend.
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
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
