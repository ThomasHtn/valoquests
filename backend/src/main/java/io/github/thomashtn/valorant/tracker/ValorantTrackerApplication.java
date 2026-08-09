package io.github.thomashtn.valorant.tracker;

import io.github.thomashtn.valorant.tracker.henrik.config.HenrikApiProperties;
import io.github.thomashtn.valorant.tracker.shared.config.ApplicationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;

/**
 * Main entry point for the Valorant Tracker backend application.
 */
@SpringBootApplication
@EnableConfigurationProperties({ApplicationProperties.class, HenrikApiProperties.class})
@PropertySource(value = "file:.env", ignoreResourceNotFound = true)
public class ValorantTrackerApplication {

    /**
     * Starts the Spring Boot application.
     *
     * @param args command-line arguments passed to Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(ValorantTrackerApplication.class, args);
    }
}
