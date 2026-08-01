package io.github.thomashtn.valorant.tracker.henrik.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration used by every Henrik API client.
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
    @NotNull Duration connectTimeout,
    @NotNull Duration readTimeout,
    @Min(1)
    @Max(10)
    int maxAttempts,
    @NotNull Duration retryDelay,
    @Min(1)
    @Max(50)
    @DefaultValue("25")
    int rateLimitMaxAttempts,
    @Min(1)
    @DefaultValue("30")
    int requestsPerMinute,
    @NotNull
    @DefaultValue("PT0.1S")
    Duration rateLimitSafetyMargin
) {

    /**
     * Validates duration-based configuration values.
     */
    public HenrikApiProperties {
        validatePositiveDuration(
            connectTimeout,
            "henrik.api.connect-timeout"
        );
        validatePositiveDuration(
            readTimeout,
            "henrik.api.read-timeout"
        );
        validatePositiveDuration(
            retryDelay,
            "henrik.api.retry-delay"
        );
        validateNonNegativeDuration(
            rateLimitSafetyMargin,
            "henrik.api.rate-limit-safety-margin"
        );
    }

    /**
     * Ensures that a duration is strictly positive.
     *
     * @param duration     duration to validate
     * @param propertyName related configuration-property name
     */
    private static void validatePositiveDuration(
        Duration duration,
        String propertyName
    ) {
        if (duration == null
            || duration.isZero()
            || duration.isNegative()) {
            throw new IllegalArgumentException(
                propertyName + " must be greater than zero"
            );
        }
    }

    /**
     * Ensures that a duration is present and positive or zero.
     *
     * @param duration     duration to validate
     * @param propertyName related configuration-property name
     */
    private static void validateNonNegativeDuration(
        Duration duration,
        String propertyName
    ) {
        if (duration == null) {
            throw new IllegalArgumentException(
                propertyName + " must be configured"
            );
        }

        if (duration.isNegative()) {
            throw new IllegalArgumentException(
                propertyName + " must not be negative"
            );
        }
    }
}
