package io.github.thomashtn.valorant.tracker.henrik.config;

import jakarta.validation.constraints.*;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Contains configuration properties for henrik api.
 */
@Validated
@ConfigurationProperties("henrik.api")
public record HenrikApiProperties(
    @NotBlank String baseUrl,
    String key,
    Duration connectTimeout,
    Duration readTimeout
) {
}
