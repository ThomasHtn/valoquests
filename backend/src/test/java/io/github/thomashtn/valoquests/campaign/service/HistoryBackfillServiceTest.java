package io.github.thomashtn.valoquests.campaign.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.campaign.CampaignFixtures;
import io.github.thomashtn.valoquests.henrik.client.HenrikMatchClient;
import io.github.thomashtn.valoquests.henrik.dto.match.HenrikMatchHistoryResponse;
import io.github.thomashtn.valoquests.henrik.dto.match.HenrikMatchHistoryResponse.HenrikMatchData;
import io.github.thomashtn.valoquests.henrik.dto.match.HenrikMatchMetadata;
import io.github.thomashtn.valoquests.henrik.exception.HenrikServiceUnavailableException;
import io.github.thomashtn.valoquests.match.model.MatchImportResult;
import io.github.thomashtn.valoquests.match.service.MatchImportService;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.synchronization.entity.Synchronization;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationStatus;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationType;
import io.github.thomashtn.valoquests.synchronization.repository.SynchronizationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifies the one-off walk that fills a calibration window, and where it stops.
 */
@ExtendWith(MockitoExtension.class)
class HistoryBackfillServiceTest {

    /**
     * Instant the walk runs at.
     */
    private static final Instant NOW = Instant.parse("2026-09-04T10:00:00Z");

    /**
     * An instant comfortably inside the nine-month window.
     */
    private static final Instant RECENT = Instant.parse("2026-08-01T10:00:00Z");

    /**
     * An instant older than the nine-month window.
     */
    private static final Instant ANCIENT = Instant.parse("2025-01-01T10:00:00Z");

    @Mock
    private HenrikMatchClient matchClient;

    @Mock
    private MatchImportService matchImportService;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private SynchronizationRepository synchronizationRepository;

    private HistoryBackfillService service;

    private Player operator;

    @BeforeEach
    void setUp() {
        service = new HistoryBackfillService(
            matchClient,
            matchImportService,
            playerRepository,
            synchronizationRepository,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );
        operator = CampaignFixtures.player(1, "Alpha");
        operator.setRiotPuuid("puuid-1");
        when(synchronizationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Stops as soon as a page reaches past the calibration window")
    void shouldStopOnceThePageReachesPastTheWindow() {
        when(playerRepository.findAllByStatusOrderByIdAsc(PlayerStatus.ACTIVE)).thenReturn(List.of(operator));
        when(matchClient.getMatches(eq("puuid-1"), anyInt(), anyInt()))
            .thenReturn(page(RECENT), page(ANCIENT));
        when(matchImportService.importMatchesWithSummary(eq(operator), any()))
            .thenReturn(new MatchImportResult(1, 1, 0, 0, 0));

        Synchronization execution = service.backfill();

        verify(matchClient, times(2)).getMatches(eq("puuid-1"), anyInt(), anyInt());
        assertThat(execution.getType()).isEqualTo(SynchronizationType.HISTORY_BACKFILL);
        assertThat(execution.getStatus()).isEqualTo(SynchronizationStatus.COMPLETED);
        assertThat(execution.getMatchesImported()).isEqualTo(2);
        assertThat(execution.getPlayersProcessed()).isEqualTo(1);
        assertThat(execution.getFinishedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("Stops when Henrik runs out of history")
    void shouldStopOnAnEmptyPage() {
        when(playerRepository.findAllByStatusOrderByIdAsc(PlayerStatus.ACTIVE)).thenReturn(List.of(operator));
        when(matchClient.getMatches(eq("puuid-1"), anyInt(), anyInt()))
            .thenReturn(page(RECENT), new HenrikMatchHistoryResponse(200, List.of()));
        when(matchImportService.importMatchesWithSummary(eq(operator), any()))
            .thenReturn(new MatchImportResult(1, 1, 0, 0, 0));

        Synchronization execution = service.backfill();

        assertThat(execution.getMatchesImported()).isEqualTo(1);
        assertThat(execution.getStatus()).isEqualTo(SynchronizationStatus.COMPLETED);
    }

    @Test
    @DisplayName("Walks every other operator when one of them fails")
    void shouldCarryOnAfterAFailure() {
        Player failing = CampaignFixtures.player(2, "Bravo");
        failing.setRiotPuuid("puuid-2");

        when(playerRepository.findAllByStatusOrderByIdAsc(PlayerStatus.ACTIVE))
            .thenReturn(List.of(failing, operator));
        when(matchClient.getMatches(eq("puuid-2"), anyInt(), anyInt()))
            .thenThrow(new HenrikServiceUnavailableException("Henrik is down"));
        when(matchClient.getMatches(eq("puuid-1"), anyInt(), anyInt())).thenReturn(page(ANCIENT));
        when(matchImportService.importMatchesWithSummary(eq(operator), any()))
            .thenReturn(new MatchImportResult(1, 1, 0, 0, 0));

        Synchronization execution = service.backfill();

        assertThat(execution.getStatus()).isEqualTo(SynchronizationStatus.FAILED);
        assertThat(execution.getFailureCount()).isEqualTo(1);
        assertThat(execution.getMatchesImported()).isEqualTo(1);
        assertThat(execution.getErrorMessage()).contains("1 player(s)");
    }

    @Test
    @DisplayName("Records an execution even when no operator is active")
    void shouldRecordAnEmptyWalk() {
        when(playerRepository.findAllByStatusOrderByIdAsc(PlayerStatus.ACTIVE)).thenReturn(List.of());

        Synchronization execution = service.backfill();

        assertThat(execution.getPlayersProcessed()).isZero();
        assertThat(execution.getStatus()).isEqualTo(SynchronizationStatus.COMPLETED);
    }

    /**
     * Builds a Henrik page holding one match started at an instant.
     *
     * @param startedAt instant the match started
     * @return the page
     */
    private HenrikMatchHistoryResponse page(Instant startedAt) {
        HenrikMatchMetadata metadata = new HenrikMatchMetadata(
            "match-" + startedAt,
            null,
            1_800_000L,
            startedAt,
            true,
            null,
            null
        );

        return new HenrikMatchHistoryResponse(200, List.of(new HenrikMatchData(metadata, List.of(), List.of())));
    }
}
