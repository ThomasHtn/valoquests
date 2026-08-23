package io.github.thomashtn.valoquests.shared.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Contains the global application configuration.
 *
 * @param frontendOrigin Angular frontend origin allowed by CORS
 * @param adminApiKey    secret protecting administrative endpoints
 */
@Validated
@ConfigurationProperties("app")
public record ApplicationProperties(

    @NotBlank String frontendOrigin,
    @NotBlank
    @Size(min = 32)
    String adminApiKey
) {
}
