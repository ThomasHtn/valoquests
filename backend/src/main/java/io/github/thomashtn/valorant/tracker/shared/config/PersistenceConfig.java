package io.github.thomashtn.valorant.tracker.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Configures persistence infrastructure components.
 */
@Configuration
@EnableJpaAuditing
public class PersistenceConfig {
}
