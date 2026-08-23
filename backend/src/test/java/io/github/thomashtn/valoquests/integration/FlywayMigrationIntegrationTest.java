package io.github.thomashtn.valoquests.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

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
        assertThat(challenges).isEqualTo(62);
        assertThat(players).isGreaterThanOrEqualTo(6);
    }

    /**
     * Verifies that no challenge survives filtered on a mode that is no longer imported.
     *
     * <p>Such a challenge could be drawn into a weekly pack and would then stay at zero for every
     * player, wasting one of the difficulty slots of that week.
     */
    @Test
    void shouldRemoveChallengesFilteredOnAnUnimportedGameMode() {
        Integer unreachableChallenges = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM challenge c
                WHERE EXISTS (SELECT 1
                              FROM jsonb_array_elements(c.conditions_json) AS condition
                              WHERE condition ->> 'gameMode' IN ('SWIFTPLAY', 'ESCALATION'))
                """,
            Integer.class
        );

        assertThat(unreachableChallenges).isZero();
    }

    /**
     * Verifies that the reset left no derived data behind.
     *
     * <p>Every one of these tables is rebuilt from the matches the next synchronization imports.
     * A surviving row would describe a history the database can no longer justify.
     */
    @Test
    void shouldLeaveEveryDerivedTableEmpty() {
        assertThat(countRows("valorant_match")).isZero();
        assertThat(countRows("player_match")).isZero();
        assertThat(countRows("season")).isZero();
        assertThat(countRows("weekly_challenge")).isZero();
        assertThat(countRows("player_challenge_progress")).isZero();
        assertThat(countRows("weekly_player_score")).isZero();
        assertThat(countRows("synchronization")).isZero();
        assertThat(countRows("synchronization_player_result")).isZero();
    }

    /**
     * Verifies that the season tracking table exists and starts empty.
     */
    @Test
    void shouldCreateAnEmptySeasonSynchronizationTable() {
        assertThat(countRows("player_season_synchronization")).isZero();
    }

    /**
     * Verifies that a player result can record why its match-history walk stopped.
     *
     * <p>Nullable on purpose: a player that failed never completed a walk, so requiring a value here
     * would make a failure impossible to persist.
     */
    @Test
    void shouldRecordTheWalkStopReasonAsAnOptionalColumn() {
        String nullable = jdbcTemplate.queryForObject(
            """
                SELECT is_nullable
                FROM information_schema.columns
                WHERE table_name = 'synchronization_player_result'
                  AND column_name = 'stop_reason'
                """,
            String.class
        );

        assertThat(nullable).isEqualTo("YES");
    }

    /**
     * Verifies that no player claims a synchronization watermark for a deleted history.
     */
    @Test
    void shouldResetEverySynchronizationWatermark() {
        Integer playersWithWatermark = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM player
                WHERE last_successful_synchronization_at IS NOT NULL
                """,
            Integer.class
        );

        assertThat(playersWithWatermark).isZero();
    }

    /**
     * Counts the rows of one table.
     *
     * @param table table name, never built from external input
     * @return current row count
     */
    private Integer countRows(String table) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + table,
            Integer.class
        );
    }
}
