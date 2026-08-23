package io.github.thomashtn.valoquests;

import io.github.thomashtn.valoquests.henrik.config.HenrikApiProperties;
import io.github.thomashtn.valoquests.shared.config.ApplicationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;

/**
 * Main entry point for the ValoQuests backend application.
 */
@SpringBootApplication
@EnableConfigurationProperties({ApplicationProperties.class, HenrikApiProperties.class})
@PropertySource(value = "file:.env", ignoreResourceNotFound = true)
public class ValoQuestsApplication {

    /**
     * Starts the Spring Boot application.
     *
     * @param args command-line arguments passed to Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(ValoQuestsApplication.class, args);
    }
}
