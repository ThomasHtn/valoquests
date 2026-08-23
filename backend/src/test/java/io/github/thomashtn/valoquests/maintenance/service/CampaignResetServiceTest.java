package io.github.thomashtn.valoquests.maintenance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.shared.exception.ConflictException;
import io.github.thomashtn.valoquests.synchronization.service.SynchronizationLaunchService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link CampaignResetService}.
 *
 * <p>The statements themselves are Postgres-specific and are exercised for real by
 * {@code AdminBackofficeIntegrationTest}. What is pinned here is what the reset refuses to do, and
 * which tables it names — a table silently dropped from the list would leave derived data behind
 * and is exactly the regression these assertions catch.
 */
@ExtendWith(MockitoExtension.class)
class CampaignResetServiceTest {

    /**
     * Tables the reset must empty.
     */
    private static final List<String> DERIVED_TABLES = List.of(
        "player_challenge_progress",
        "weekly_player_score",
        "weekly_challenge",
        "weekly_boss_encounter",
        "player_season_synchronization",
        "synchronization_player_result",
        "synchronization",
        "player_match",
        "valorant_match",
        "season"
    );

    /**
     * Mocked entity manager.
     */
    @Mock
    private EntityManager entityManager;

    /**
     * Mocked native query.
     */
    @Mock
    private Query query;

    /**
     * Mocked synchronization launch service.
     */
    @Mock
    private SynchronizationLaunchService synchronizationLaunchService;

    /**
     * Captures the executed statements.
     */
    @Captor
    private ArgumentCaptor<String> statementCaptor;

    /**
     * Service under test.
     */
    private CampaignResetService service;

    /**
     * Creates the service under test before each test.
     */
    @BeforeEach
    void setUp() {
        service = new CampaignResetService(entityManager, synchronizationLaunchService);
    }

    /**
     * Verifies that every derived table is emptied and the roster's watermark rewound.
     */
    @Test
    void shouldEmptyEveryDerivedTableAndRewindTheRoster() {
        when(synchronizationLaunchService.isSynchronizationInProgress()).thenReturn(false);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);

        service.resetCampaign();

        verify(entityManager, org.mockito.Mockito.times(2))
            .createNativeQuery(statementCaptor.capture());

        String truncate = statementCaptor.getAllValues().get(0);
        String rewind = statementCaptor.getAllValues().get(1);

        assertThat(truncate).contains("TRUNCATE TABLE").contains("RESTART IDENTITY");
        assertThat(DERIVED_TABLES).allSatisfy(table -> assertThat(truncate).contains(table));
        assertThat(truncate).doesNotContain("CASCADE");
        assertThat(rewind).contains("last_successful_synchronization_at = NULL");

        verify(query, org.mockito.Mockito.times(2)).executeUpdate();
        verify(entityManager).clear();
    }

    /**
     * Verifies that the roster, the challenge catalogue and the boss catalogue survive.
     */
    @Test
    void shouldKeepTheRosterAndTheCatalogues() {
        when(synchronizationLaunchService.isSynchronizationInProgress()).thenReturn(false);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);

        service.resetCampaign();

        verify(entityManager, org.mockito.Mockito.times(2))
            .createNativeQuery(statementCaptor.capture());

        String truncate = statementCaptor.getAllValues().get(0);

        assertThat(truncate)
            .doesNotContain("boss_catalog_entry")
            .doesNotContain("TRUNCATE TABLE player,");
        assertThat(truncate.lines().map(String::strip))
            .doesNotContain("challenge,", "player,");
    }

    /**
     * Verifies that a running synchronization blocks the reset.
     *
     * <p>It would otherwise be importing matches straight into the base being emptied, leaving a
     * half-populated campaign nothing can explain.
     */
    @Test
    void shouldRefuseToResetWhileASynchronizationRuns() {
        when(synchronizationLaunchService.isSynchronizationInProgress()).thenReturn(true);

        assertThatThrownBy(() -> service.resetCampaign())
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("synchronization is in progress");

        verifyNoInteractions(entityManager);
    }
}
