package io.github.thomashtn.valoquests.henrik.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration used by every Henrik API client.
 *
 * <p>Validated when bound at startup: an invalid value fails the application before any request.
 *
 * @param baseUrl               Henrik API base URL
 * @param key                   Henrik API access token
 * @param region                Valorant region
 * @param platform              Valorant platform
 * @param connectTimeout        maximum connection duration
 * @param readTimeout           maximum response duration
 * @param maxAttempts           maximum number of HTTP attempts for a non-rate-limit failure
 * @param retryDelay            fallback delay before retrying a request
 * @param rateLimitMaxAttempts  maximum number of HTTP attempts when Henrik responds with a rate limit
 * @param requestsPerMinute     maximum number of Henrik requests per minute
 * @param rateLimitSafetyMargin additional delay added between two requests
 */
@Validated
@ConfigurationProperties(prefix = "henrik.api")
public record HenrikApiProperties(

    @NotBlank String baseUrl,
    @NotBlank String key,
    @NotBlank String region,
    @NotBlank String platform,
    @NotNull @DurationMin(nanos = 0, inclusive = false) Duration connectTimeout,
    @NotNull @DurationMin(nanos = 0, inclusive = false) Duration readTimeout,
    @Min(1)
    @Max(10)
    int maxAttempts,
    @NotNull @DurationMin(nanos = 0, inclusive = false) Duration retryDelay,
    @Min(1)
    @Max(50)
    @DefaultValue("25")
    int rateLimitMaxAttempts,
    @Min(1)
    @DefaultValue("30")
    int requestsPerMinute,
    @NotNull
    @DurationMin(nanos = 0)
    @DefaultValue("PT0.1S")
    Duration rateLimitSafetyMargin
) {
}
