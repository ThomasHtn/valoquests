package io.github.thomashtn.valoquests.henrik.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * Verifies that {@link HenrikApiProperties} rejects invalid durations when bound at startup.
 */
class HenrikApiPropertiesTest {

    /**
     * Context binding the Henrik properties from a valid baseline that each test overrides.
     */
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(PropertiesConfiguration.class)
        .withPropertyValues(
            "henrik.api.base-url=http://localhost",
            "henrik.api.key=test-api-key",
            "henrik.api.region=eu",
            "henrik.api.platform=pc",
            "henrik.api.connect-timeout=PT2S",
            "henrik.api.read-timeout=PT2S",
            "henrik.api.max-attempts=1",
            "henrik.api.retry-delay=PT1S"
        );

    @Test
    @DisplayName("Starts with valid durations and a zero safety margin")
    void shouldAcceptValidDurations() {
        contextRunner
            .withPropertyValues("henrik.api.rate-limit-safety-margin=PT0S")
            .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    @DisplayName("Fails to start when a timeout is zero")
    void shouldRejectZeroTimeout() {
        contextRunner
            .withPropertyValues("henrik.api.read-timeout=PT0S")
            .run(context -> assertThat(context).getFailure().hasStackTraceContaining("readTimeout"));
    }

    @Test
    @DisplayName("Fails to start when the retry delay is negative")
    void shouldRejectNegativeRetryDelay() {
        contextRunner
            .withPropertyValues("henrik.api.retry-delay=-PT1S")
            .run(context -> assertThat(context).getFailure().hasStackTraceContaining("retryDelay"));
    }

    @Test
    @DisplayName("Fails to start when the safety margin is negative")
    void shouldRejectNegativeSafetyMargin() {
        contextRunner
            .withPropertyValues("henrik.api.rate-limit-safety-margin=-PT0.1S")
            .run(context -> assertThat(context).getFailure().hasStackTraceContaining("rateLimitSafetyMargin"));
    }

    @Test
    @DisplayName("Fails to start when the connect timeout is missing")
    void shouldRejectMissingConnectTimeout() {
        new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class)
            .withPropertyValues(
                "henrik.api.base-url=http://localhost",
                "henrik.api.key=test-api-key",
                "henrik.api.region=eu",
                "henrik.api.platform=pc",
                "henrik.api.read-timeout=PT2S",
                "henrik.api.max-attempts=1",
                "henrik.api.retry-delay=PT1S"
            )
            .run(context -> assertThat(context).getFailure().hasStackTraceContaining("connectTimeout"));
    }

    /**
     * Binds the properties under test, nothing else.
     */
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(HenrikApiProperties.class)
    static class PropertiesConfiguration {
    }
}
