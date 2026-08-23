package io.github.thomashtn.valoquests.integration;

import org.junit.jupiter.api.Tag;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
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
     *
     * <p>Deliberately <em>not</em> annotated {@code @Container}: that annotation ties the
     * container's lifecycle to the JUnit 5 {@code Testcontainers} extension, which stops it once
     * the class currently using it finishes - including this static field shared across every
     * subclass. Whichever integration test class happens to run last would then find the container
     * already stopped and fail with a connection refused error. This is the "singleton container"
     * pattern Testcontainers itself documents for this exact case: started once, in a static
     * initializer, and left to the JVM shutdown hook (Ryuk) to reap (see "singleton containers" in
     * the Testcontainers manual lifecycle control documentation).
     */
    protected static final PostgreSQLContainer<?> POSTGRESQL =
        new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("valo_quests")
            .withUsername("valorant")
            .withPassword("valorant");

    static {
        POSTGRESQL.start();
    }

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
