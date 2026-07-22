package io.github.thomashtn.valorant.tracker.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that production Flyway migrations can be applied successfully
 * to a real PostgreSQL database.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "app.admin-api-key=test-admin-key-0123456789abcdef0"
    }
)
class FlywayMigrationIntegrationTest extends PostgreSqlIntegrationTest {

    /**
     * JDBC client used to inspect the migrated PostgreSQL database.
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Verifies that all Flyway migrations are applied and that the expected
     * reference data is available.
     */
    @Test
    void shouldApplyAllMigrationsAndLoadReferenceData() {
        Integer failedMigrations = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE success = false
                """,
            Integer.class
        );

        Integer challenges = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM challenge
                """,
            Integer.class
        );

        Integer players = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM player
                """,
            Integer.class
        );

        assertThat(failedMigrations).isZero();
        assertThat(challenges).isEqualTo(78);
        assertThat(players).isGreaterThanOrEqualTo(6);
    }
}
