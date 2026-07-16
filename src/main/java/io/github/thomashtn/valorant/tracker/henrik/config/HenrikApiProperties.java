package io.github.thomashtn.valorant.tracker.henrik.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Defines the configuration required to communicate with the HenrikDev API.
 *
 * <p>The region and platform are application-wide settings because the current
 * version of the tracker follows European PC players only. Keeping these values
 * configurable avoids hard-coding them in the HTTP client.</p>
 *
 * @param baseUrl base URL of the HenrikDev API
 * @param key API authentication key
 * @param region Riot shard used for all tracked players
 * @param platform Valorant platform used for all tracked players
 * @param connectTimeout maximum duration allowed to establish a connection
 * @param readTimeout maximum duration allowed to receive a response
 * @param maxAttempts maximum number of attempts, including the initial request
 * @param retryDelay waiting duration between retry attempts
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
    @Min(1) @Max(5) int maxAttempts,
    @NotNull Duration retryDelay
) {

    /**
     * Validates configuration values requiring rules more complex than standard
     * Jakarta Validation annotations.
     *
     * @throws IllegalArgumentException when a duration is zero or negative
     */
    public HenrikApiProperties {
        validatePositiveDuration(connectTimeout, "henrik.api.connect-timeout");
        validatePositiveDuration(readTimeout, "henrik.api.read-timeout");
        validatePositiveDuration(retryDelay, "henrik.api.retry-delay");
    }

    /**
     * Ensures that a configuration duration is strictly positive.
     *
     * @param duration configured duration
     * @param propertyName property name displayed in the error message
     * @throws IllegalArgumentException when the duration is zero or negative
     */
    private static void validatePositiveDuration(
        Duration duration,
        String propertyName
    ) {
        if (duration != null && (duration.isZero() || duration.isNegative())) {
            throw new IllegalArgumentException(
                propertyName + " must be greater than zero"
            );
        }
    }
}
