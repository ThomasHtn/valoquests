package io.github.thomashtn.valoquests.synchronization.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.henrik.client.HenrikMmrClient;
import io.github.thomashtn.valoquests.henrik.dto.mmr.HenrikMmrResponse;
import io.github.thomashtn.valoquests.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.exception.PlayerNotFoundException;
import io.github.thomashtn.valoquests.player.mapper.HenrikMmrMapper;
import io.github.thomashtn.valoquests.player.model.CompetitiveTier;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.player.service.PlayerAccountResolutionService;
import io.github.thomashtn.valoquests.synchronization.model.MatchHistoryWalkResult;
import io.github.thomashtn.valoquests.synchronization.model.PlayerSynchronizationResult;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationStopReason;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Unit tests for {@link PlayerSynchronizationService}.
 *
 * <p>Only orchestration is covered here. Pagination, season scope and stop conditions belong to
 * {@link SeasonMatchHistoryWalker} and are asserted in its own test.
 */
@ExtendWith(MockitoExtension.class)
class PlayerSynchronizationServiceTest {

    /**
     * Fixed synchronization completion time used by the tests.
     */
    private static final Instant SYNCHRONIZED_AT =
        Instant.parse("2026-07-18T10:00:00Z");

    /**
     * Riot identifier of the synchronized player.
     */
    private static final String PUUID = "puuid-1";

    /**
     * End of the player's previous successful synchronization.
     */
    private static final Instant PREVIOUS_SYNCHRONIZATION_AT =
        Instant.parse("2026-07-18T09:55:00Z");

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private PlayerAccountResolutionService accountResolutionService;

    @Mock
    private HenrikMmrClient mmrClient;

    @Mock
    private HenrikMmrMapper mmrMapper;

    @Mock
    private SeasonMatchHistoryWalker matchHistoryWalker;

    @Mock
    private PlayerMatchRepository playerMatchRepository;

    /**
     * Service under test.
     */
    private PlayerSynchronizationService service;

    /**
     * Creates the service under test before each test.
     */
    @BeforeEach
    void setUp() {
        service = new PlayerSynchronizationService(
            playerRepository,
            accountResolutionService,
            mmrClient,
            mmrMapper,
            matchHistoryWalker,
            playerMatchRepository,
            Clock.fixed(SYNCHRONIZED_AT, ZoneOffset.UTC)
        );
    }

    /**
     * Verifies the full successful orchestration, in order.
     */
    @Test
    void shouldResolveAccountWalkMatchHistoryThenRefreshRank() {
        Player player = player();
        HenrikMmrResponse mmrResponse = new HenrikMmrResponse(null);

        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(accountResolutionService.resolvePuuid(player)).thenReturn(player);
        when(mmrClient.getCurrentMmr(PUUID)).thenReturn(mmrResponse);
        when(matchHistoryWalker.walk(player)).thenReturn(
            new MatchHistoryWalkResult(4, 12, SynchronizationStopReason.SEASON_BOUNDARY)
        );

        PlayerSynchronizationResult result = service.synchronize(1L);

        assertThat(result.player()).isSameAs(player);
        assertThat(result.pagesFetched()).isEqualTo(4);
        assertThat(result.matchesImported()).isEqualTo(12);
        assertThat(result.stopReason())
            .isEqualTo(SynchronizationStopReason.SEASON_BOUNDARY);

        InOrder ordered = inOrder(accountResolutionService, matchHistoryWalker, mmrMapper);
        ordered.verify(accountResolutionService).resolvePuuid(player);
        ordered.verify(matchHistoryWalker).walk(player);
        ordered.verify(mmrMapper).updatePlayer(mmrResponse, player);
    }

    /**
     * Verifies that a quiet pass spares the Henrik rank call.
     */
    @Test
    void shouldKeepTheStoredRankWhenNoMatchArrived() {
        Player player = player();
        player.setLastSuccessfulSynchronizationAt(PREVIOUS_SYNCHRONIZATION_AT);

        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(accountResolutionService.resolvePuuid(player)).thenReturn(player);
        when(matchHistoryWalker.walk(player)).thenReturn(
            new MatchHistoryWalkResult(1, 0, SynchronizationStopReason.KNOWN_HISTORY_REACHED)
        );
        when(playerMatchRepository.existsByPlayerIdAndCreatedAtAfter(1L, PREVIOUS_SYNCHRONIZATION_AT))
            .thenReturn(false);

        service.synchronize(1L);

        verifyNoInteractions(mmrClient, mmrMapper);
        verify(playerRepository).recordSuccessfulSynchronization(
            1L,
            CompetitiveTier.UNRANKED,
            null,
            SYNCHRONIZED_AT
        );
    }

    /**
     * Verifies that matches left by a failed pass still trigger the rank refresh.
     */
    @Test
    void shouldRefreshTheRankWhenAFailedPassImportedMatches() {
        Player player = player();
        player.setLastSuccessfulSynchronizationAt(PREVIOUS_SYNCHRONIZATION_AT);
        HenrikMmrResponse mmrResponse = new HenrikMmrResponse(null);

        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(accountResolutionService.resolvePuuid(player)).thenReturn(player);
        when(matchHistoryWalker.walk(player)).thenReturn(
            new MatchHistoryWalkResult(1, 0, SynchronizationStopReason.KNOWN_HISTORY_REACHED)
        );
        when(playerMatchRepository.existsByPlayerIdAndCreatedAtAfter(1L, PREVIOUS_SYNCHRONIZATION_AT))
            .thenReturn(true);
        when(mmrClient.getCurrentMmr(PUUID)).thenReturn(mmrResponse);

        service.synchronize(1L);

        verify(mmrMapper).updatePlayer(mmrResponse, player);
    }

    /**
     * Verifies that the incremental watermark is stored once the walk succeeded.
     */
    @Test
    void shouldRecordTheSuccessfulSynchronizationInstant() {
        Player player = player();

        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(accountResolutionService.resolvePuuid(player)).thenReturn(player);
        when(mmrClient.getCurrentMmr(PUUID)).thenReturn(new HenrikMmrResponse(null));
        when(matchHistoryWalker.walk(player)).thenReturn(
            new MatchHistoryWalkResult(1, 0, SynchronizationStopReason.KNOWN_HISTORY_REACHED)
        );

        service.synchronize(1L);

        assertThat(player.getLastSuccessfulSynchronizationAt())
            .isEqualTo(SYNCHRONIZED_AT);
        verify(playerRepository).recordSuccessfulSynchronization(
            1L,
            CompetitiveTier.UNRANKED,
            null,
            SYNCHRONIZED_AT
        );
        verify(playerRepository, never()).save(any());
    }

    /**
     * Verifies that a player with no match is reported rather than failed.
     */
    @Test
    void shouldReportAPlayerWithoutAnyMatch() {
        Player player = player();

        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(accountResolutionService.resolvePuuid(player)).thenReturn(player);
        when(mmrClient.getCurrentMmr(PUUID)).thenReturn(new HenrikMmrResponse(null));
        when(matchHistoryWalker.walk(player)).thenReturn(
            new MatchHistoryWalkResult(1, 0, SynchronizationStopReason.EMPTY_PAGE)
        );

        PlayerSynchronizationResult result = service.synchronize(1L);

        assertThat(result.matchesImported()).isZero();
        assertThat(result.stopReason())
            .isEqualTo(SynchronizationStopReason.EMPTY_PAGE);
    }

    /**
     * Verifies that an unknown player fails before any Henrik call is made.
     */
    @Test
    void shouldRejectAnUnknownPlayer() {
        when(playerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.synchronize(99L))
            .isInstanceOf(PlayerNotFoundException.class);

        verifyNoInteractions(accountResolutionService, mmrClient, matchHistoryWalker);
    }

    /**
     * Verifies that synchronizing a player fails fast when invoked inside a database transaction,
     * instead of silently defeating the walk's per-step checkpoint commits.
     */
    @Test
    void shouldRejectSynchronizationInsideAnActiveTransaction() {
        TransactionSynchronizationManager.setActualTransactionActive(true);

        try {
            assertThatThrownBy(() -> service.synchronize(1L))
                .isInstanceOf(IllegalStateException.class);

            verifyNoInteractions(playerRepository, accountResolutionService, mmrClient, matchHistoryWalker);
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    /**
     * Creates the synchronized player.
     */
    private Player player() {
        Player player = new Player();
        player.setId(1L);
        player.setDisplayName("Player One");
        player.setRiotPuuid(PUUID);
        return player;
    }
}
