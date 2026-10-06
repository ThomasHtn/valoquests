package io.github.thomashtn.valoquests.integration;

import org.junit.jupiter.api.Tag;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Provides a shared PostgreSQL Testcontainer for integration tests.
 *
 * <p>It replaces the unit tests' H2 database; Flyway migrates it and Hibernate validates the schema.
 */
@Tag("integration")
@Testcontainers(disabledWithoutDocker = true)
public abstract class PostgreSqlIntegrationTest {

    /**
     * PostgreSQL container shared by every integration test class.
     *
     * <p>Not {@code @Container} on purpose: the extension would stop it after the first class, breaking the next.
     * Started once and reaped by Ryuk at JVM exit (Testcontainers "singleton container" pattern).
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
     * Overrides the test database configuration with the container's values.
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
