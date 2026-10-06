package io.github.thomashtn.valoquests.maintenance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
 * Unit tests for {@link DefaultCampaignResetService}.
 *
 * <p>{@code AdminBackofficeIntegrationTest} runs the SQL; this pins the table list so none is silently dropped.
 */
@ExtendWith(MockitoExtension.class)
class DefaultCampaignResetServiceTest {

    /**
     * Tables the reset must empty.
     */
    private static final List<String> DERIVED_TABLES = List.of(
        "player_challenge_progress",
        "weekly_player_score",
        "weekly_challenge",
        "campaign_player_day",
        "campaign_daily_snapshot",
        "campaign_week",
        "campaign_player",
        "campaign",
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
        service = new DefaultCampaignResetService(entityManager);
    }

    /**
     * Verifies that every derived table is emptied and the roster's watermark rewound.
     */
    @Test
    void shouldEmptyEveryDerivedTableAndRewindTheRoster() {
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
     * Verifies that the roster, the challenge catalogue and the guardian catalogue survive.
     */
    @Test
    void shouldKeepTheRosterAndTheCatalogues() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);

        service.resetCampaign();

        verify(entityManager, org.mockito.Mockito.times(2))
            .createNativeQuery(statementCaptor.capture());

        String truncate = statementCaptor.getAllValues().get(0);

        assertThat(truncate)
            .doesNotContain("guardian")
            .doesNotContain("TRUNCATE TABLE player,");
        assertThat(truncate.lines().map(String::strip))
            .doesNotContain("challenge,", "player,");
    }
}
