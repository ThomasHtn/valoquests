package io.github.thomashtn.valorant.tracker.shared.config;

import io.github.thomashtn.valorant.tracker.synchronization.model.DeepSynchronizationScope;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Contains the global application configuration.
 *
 * @param frontendOrigin Angular frontend origin allowed by CORS
 * @param adminApiKey    secret protecting administrative endpoints
 * @param scheduling     synchronization scheduling configuration
 */
@Validated
@ConfigurationProperties("app")
public record ApplicationProperties(
    @NotBlank String frontendOrigin,
    @NotBlank String adminApiKey,
    @Valid
    @NotNull
    Scheduling scheduling
) {

    /**
     * Contains synchronization-related configuration.
     *
     * @param deepSynchronizationScope history range imported by deep synchronization
     */
    public record Scheduling(
        @NotNull DeepSynchronizationScope deepSynchronizationScope
    ) {
    }
}
