package io.github.thomashtn.valoquests.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.github.thomashtn.valoquests.boss.repository.WeeklyBossEncounterRepository;
import io.github.thomashtn.valoquests.challenge.service.ChallengeQueryService;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

/**
 * Verifies that the current-challenges endpoint can open a week that has no boss encounter yet.
 *
 * <p>Deliberately <em>not</em> {@code @Transactional}, unlike every other integration test here. The
 * defect this covers only exists when the service starts the transaction itself: resolving a week's
 * ruleset lazily draws that week's boss encounter, and the query service's own read-only default made
 * PostgreSQL reject the insert with "cannot execute INSERT in a read-only transaction". A test wrapped
 * in its own writable transaction would have joined that one instead and passed against a broken
 * service, which is exactly what happened — the unit test mocks the resolver, so nothing caught it.
 *
 * <p>The week is fixed far from the fixtures the other integration tests pin, and its rows are removed
 * afterwards, since nothing rolls this test back.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "app.admin-api-key=test-admin-key-0123456789abcdef0",
        "app.scheduling.standard-synchronization-enabled=false",
        "app.scheduling.week-rollover-enabled=false"
    }
)
@Import(ChallengeQueryTransactionIntegrationTest.FixedClockConfiguration.class)
class ChallengeQueryTransactionIntegrationTest extends PostgreSqlIntegrationTest {

    /**
     * Instant inside the isolated test week, a Wednesday.
     */
    private static final Instant NOW = Instant.parse("2027-03-10T12:00:00Z");

    /**
     * Monday identifying that week.
     */
    private static final LocalDate WEEK_START = LocalDate.of(2027, 3, 8);

    /**
     * Service under test.
     */
    @Autowired
    private ChallengeQueryService challengeQueryService;

    /**
     * Repository used to assert the encounter was drawn, and to clean it up.
     */
    @Autowired
    private WeeklyBossEncounterRepository encounterRepository;

    /**
     * Calendar confirming the fixed clock resolves to the expected week.
     */
    @Autowired
    private WeekCalendar weekCalendar;

    /**
     * Removes the encounter this test causes to be created.
     */
    @AfterEach
    void removeCreatedEncounter() {
        encounterRepository.findByWeekStart(WEEK_START).ifPresent(encounterRepository::delete);
    }

    /**
     * Verifies that querying a week with no boss encounter draws one instead of failing.
     */
    @Test
    void shouldOpenTheWeeksBossEncounterWhenQueryingChallenges() {
        assertThat(weekCalendar.currentWeekStart()).isEqualTo(WEEK_START);
        assertThat(encounterRepository.findByWeekStart(WEEK_START)).isEmpty();

        assertThatCode(() -> challengeQueryService.findCurrent()).doesNotThrowAnyException();

        assertThat(encounterRepository.findByWeekStart(WEEK_START)).isPresent();
    }

    /**
     * Pins the clock so the "current" week is one no other integration test writes to.
     */
    @TestConfiguration
    static class FixedClockConfiguration {

        /**
         * Provides the clock every week resolution reads.
         *
         * @return fixed UTC clock
         */
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
