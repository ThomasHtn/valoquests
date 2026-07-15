package io.github.thomashtn.valorant.tracker.shared.config;

import jakarta.validation.constraints.*;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Contains configuration properties for application.
 */
@Validated
@ConfigurationProperties("app")
public record ApplicationProperties(
    @NotBlank String frontendOrigin,
    @NotBlank String adminApiKey,
    Scheduling scheduling
) {
    public record Scheduling(
        Duration standardSynchronizationDelay,
        Duration deepSynchronizationDelay
    ) {
    }
}
