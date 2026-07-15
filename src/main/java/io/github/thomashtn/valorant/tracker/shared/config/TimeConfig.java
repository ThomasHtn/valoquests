package io.github.thomashtn.valorant.tracker.shared.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the clock used by time-dependent application components.
 */
@Configuration
public class TimeConfig {

    /**
     * Creates a UTC system clock that can be replaced in tests.
     *
     * @return the application clock
     */
    @Bean
    Clock applicationClock() {
        return Clock.systemUTC();
    }
}
