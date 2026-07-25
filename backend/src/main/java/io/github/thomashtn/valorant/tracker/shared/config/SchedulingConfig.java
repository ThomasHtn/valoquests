package io.github.thomashtn.valorant.tracker.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's scheduling infrastructure for automated background jobs.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
