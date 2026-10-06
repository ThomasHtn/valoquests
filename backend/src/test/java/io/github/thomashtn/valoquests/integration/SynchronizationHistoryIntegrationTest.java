package io.github.thomashtn.valoquests.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.synchronization.entity.Synchronization;
import io.github.thomashtn.valoquests.synchronization.entity.SynchronizationPlayerResult;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationStatus;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationTrigger;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationType;
import io.github.thomashtn.valoquests.synchronization.repository.SynchronizationPlayerResultRepository;
import io.github.thomashtn.valoquests.synchronization.repository.SynchronizationRepository;
import io.github.thomashtn.valoquests.synchronization.service.SynchronizationHistoryPurger;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies the synchronization history queries against a real PostgreSQL.
 *
 * <p>Same context configuration as {@link AdminBackofficeIntegrationTest}, so the cached context is reused.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "app.admin-api-key=test-admin-key-0123456789abcdef0",
        "app.scheduling.standard-synchronization-enabled=false",
        "app.scheduling.week-rollover-enabled=false"
    }
)
@Transactional
class SynchronizationHistoryIntegrationTest extends PostgreSqlIntegrationTest {

    /**
     * Instant the test runs at, truncated to the precision PostgreSQL stores.
     */
    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    @Autowired
    private SynchronizationHistoryPurger historyPurger;

    @Autowired
    private SynchronizationRepository synchronizationRepository;

    @Autowired
    private SynchronizationPlayerResultRepository playerResultRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private EntityManager entityManager;

    /**
     * Verifies that only old quiet executions and their outcomes are deleted.
     */
    @Test
    @DisplayName("Deletes old quiet executions and keeps imports, failures and recent passes")
    void shouldPurgeOnlyOldQuietExecutions() {
        Player player = createPlayer();
        Synchronization oldQuiet = createExecution(SynchronizationStatus.COMPLETED, 0, Duration.ofDays(8));
        createResult(oldQuiet, player);
        Synchronization oldImport = createExecution(SynchronizationStatus.COMPLETED, 2, Duration.ofDays(8));
        Synchronization oldFailure = createExecution(SynchronizationStatus.FAILED, 0, Duration.ofDays(8));
        Synchronization recentQuiet = createExecution(SynchronizationStatus.COMPLETED, 0, Duration.ofDays(1));
        entityManager.flush();

        int deleted = historyPurger.purgeQuietExecutions();
        entityManager.clear();

        assertThat(deleted).isEqualTo(1);
        assertThat(synchronizationRepository.findAll())
            .extracting(Synchronization::getId)
            .containsExactlyInAnyOrder(oldImport.getId(), oldFailure.getId(), recentQuiet.getId());
        assertThat(playerResultRepository.findAll()).isEmpty();
    }

    /**
     * Verifies that the last import ignores quiet and failed executions.
     */
    @Test
    @DisplayName("Reports the end of the last successful execution that imported matches")
    void shouldFindTheLastImport() {
        Synchronization lastImport = createExecution(SynchronizationStatus.PARTIAL, 1, Duration.ofHours(2));
        createExecution(SynchronizationStatus.COMPLETED, 3, Duration.ofHours(5));
        createExecution(SynchronizationStatus.COMPLETED, 0, Duration.ofHours(1));
        createExecution(SynchronizationStatus.FAILED, 4, Duration.ofMinutes(30));
        entityManager.flush();

        assertThat(synchronizationRepository.findLastImportFinishedAt(
            List.of(SynchronizationStatus.COMPLETED, SynchronizationStatus.PARTIAL)
        )).contains(lastImport.getFinishedAt());
    }

    /**
     * Creates a finished scheduled execution.
     */
    private Synchronization createExecution(SynchronizationStatus status, int matchesImported, Duration age) {
        Synchronization synchronization = new Synchronization();
        synchronization.setType(SynchronizationType.STANDARD);
        synchronization.setTrigger(SynchronizationTrigger.SCHEDULED);
        synchronization.setStatus(status);
        synchronization.setStartedAt(NOW.minus(age).minusSeconds(30));
        synchronization.setFinishedAt(NOW.minus(age));
        synchronization.setMatchesImported(matchesImported);
        return synchronizationRepository.save(synchronization);
    }

    /**
     * Creates the outcome of one player in an execution.
     */
    private void createResult(Synchronization synchronization, Player player) {
        SynchronizationPlayerResult result = new SynchronizationPlayerResult();
        result.setSynchronization(synchronization);
        result.setPlayer(player);
        result.setStatus(SynchronizationStatus.COMPLETED);
        playerResultRepository.save(result);
    }

    /**
     * Creates a tracked player.
     */
    private Player createPlayer() {
        Player player = new Player();
        player.setGameName("Purged");
        player.setTagLine("EUW");
        player.setDisplayName("Purged");
        return playerRepository.save(player);
    }
}
