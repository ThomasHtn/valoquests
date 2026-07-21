package io.github.thomashtn.valorant.tracker.integration;

import org.junit.jupiter.api.Tag;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Provides a shared PostgreSQL Testcontainer for integration tests.
 *
 * <p>The container replaces the H2 database normally used by unit tests.
 * Flyway migrations are enabled and Hibernate validates the migrated schema.</p>
 */
@Tag("integration")
@Testcontainers(disabledWithoutDocker = true)
public abstract class PostgreSqlIntegrationTest {

    /**
     * PostgreSQL container shared by every integration test class.
     */
    @Container
    protected static final PostgreSQLContainer<?> POSTGRESQL =
        new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("valorant_tracker")
            .withUsername("valorant")
            .withPassword("valorant");

    /**
     * Overrides the standard test database configuration with the values
     * provided by the PostgreSQL container.
     *
     * @param registry Spring dynamic property registry
     */
    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add(
            "spring.datasource.url",
            POSTGRESQL::getJdbcUrl
        );
        registry.add(
            "spring.datasource.username",
            POSTGRESQL::getUsername
        );
        registry.add(
            "spring.datasource.password",
            POSTGRESQL::getPassword
        );
        registry.add(
            "spring.datasource.driver-class-name",
            () -> "org.postgresql.Driver"
        );
        registry.add(
            "spring.jpa.hibernate.ddl-auto",
            () -> "validate"
        );
        registry.add(
            "spring.flyway.enabled",
            () -> "true"
        );
    }
}
