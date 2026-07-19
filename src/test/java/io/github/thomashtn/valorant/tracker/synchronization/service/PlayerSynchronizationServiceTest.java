package io.github.thomashtn.valorant.tracker.synchronization.service;

import io.github.thomashtn.valorant.tracker.henrik.client.HenrikMatchClient;
import io.github.thomashtn.valorant.tracker.henrik.client.HenrikMmrClient;
import io.github.thomashtn.valorant.tracker.henrik.dto.mmr.HenrikMmrResponse;
import io.github.thomashtn.valorant.tracker.henrik.mapper.HenrikMmrMapper;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchHistoryResponse;
import io.github.thomashtn.valorant.tracker.match.service.MatchImportService;
import io.github.thomashtn.valorant.tracker.player.entity.Player;
import io.github.thomashtn.valorant.tracker.player.exception.PlayerNotFoundException;
import io.github.thomashtn.valorant.tracker.player.repository.PlayerRepository;
import io.github.thomashtn.valorant.tracker.player.service.PlayerAccountResolutionService;
import io.github.thomashtn.valorant.tracker.synchronization.model.PlayerSynchronizationResult;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PlayerSynchronizationService}.
 */
@ExtendWith(MockitoExtension.class)
class PlayerSynchronizationServiceTest {

    /**
     * Expected number of recent matches requested during a standard
     * synchronization.
     */
    private static final int RECENT_MATCH_PAGE_SIZE = 10;

    /**
     * Fixed synchronization completion time used by the tests.
     */
    private static final Instant SYNCHRONIZED_AT =
        Instant.parse("2026-07-18T10:00:00Z");

    /**
     * Number of matches reported as newly imported.
     */
    private static final int IMPORTED_MATCH_COUNT = 3;

    /**
     * Mocked player repository.
     */
    @Mock
    private PlayerRepository playerRepository;

    /**
     * Mocked Riot account resolution service.
     */
    @Mock
    private PlayerAccountResolutionService accountResolutionService;

    /**
     * Mocked Henrik match-history client.
     */
    @Mock
    private HenrikMmrClient mmrClient;

    @Mock
    private HenrikMmrMapper mmrMapper;

    @Mock
    private HenrikMatchClient matchClient;

    /**
     * Mocked match import service.
     */
    @Mock
    private MatchImportService matchImportService;

    /**
     * Fixed clock used to make time assertions deterministic.
     */
    private Clock clock;

    /**
     * Service under test.
     */
    private PlayerSynchronizationService service;

    /**
     * Creates the service under test before each test.
     */
    @BeforeEach
    void setUp() {
        clock = Clock.fixed(
            SYNCHRONIZED_AT,
            ZoneOffset.UTC
        );

        service = new PlayerSynchronizationService(
            playerRepository,
            accountResolutionService,
            mmrClient,
            mmrMapper,
            matchClient,
            matchImportService,
            clock
        );
    }

    /**
     * Verifies the complete synchronization pipeline for an existing player.
     */
    @Test
    void shouldSynchronizeExistingPlayer() {
        Long playerId = 1L;

        Player player = createPlayer(playerId);
        Player resolvedPlayer = createPlayer(playerId);
        resolvedPlayer.setRiotPuuid("resolved-puuid");

        HenrikMatchHistoryResponse response =
            new HenrikMatchHistoryResponse(
                200,
                List.of()
            );

        when(playerRepository.findById(playerId))
            .thenReturn(Optional.of(player));

        when(accountResolutionService.resolvePuuid(player))
            .thenReturn(resolvedPlayer);

        HenrikMmrResponse mmrResponse = new HenrikMmrResponse(
            200,
            null
        );

        when(mmrClient.getCurrentMmr("resolved-puuid"))
            .thenReturn(mmrResponse);

        when(
            matchClient.getMatches(
                "resolved-puuid",
                0,
                RECENT_MATCH_PAGE_SIZE
            )
        ).thenReturn(response);

        when(
            matchImportService.importMatches(
                resolvedPlayer,
                response
            )
        ).thenReturn(IMPORTED_MATCH_COUNT);

        when(playerRepository.save(resolvedPlayer))
            .thenReturn(resolvedPlayer);

        PlayerSynchronizationResult result =
            service.synchronize(playerId);

        assertThat(result.player())
            .isSameAs(resolvedPlayer);

        assertThat(result.player().getRiotPuuid())
            .isEqualTo("resolved-puuid");

        assertThat(result.matchesImported())
            .isEqualTo(IMPORTED_MATCH_COUNT);

        assertThat(result.completedAt())
            .isEqualTo(SYNCHRONIZED_AT);

        assertThat(
            result.player()
                .getLastSuccessfulSynchronizationAt()
        ).isEqualTo(SYNCHRONIZED_AT);

        verify(playerRepository)
            .findById(playerId);

        verify(accountResolutionService)
            .resolvePuuid(player);

        verify(mmrClient).getCurrentMmr("resolved-puuid");
        verify(mmrMapper).updatePlayer(
            org.mockito.ArgumentMatchers.any(HenrikMmrResponse.class),
            org.mockito.ArgumentMatchers.same(resolvedPlayer)
        );

        verify(matchClient).getMatches(
            "resolved-puuid",
            0,
            RECENT_MATCH_PAGE_SIZE
        );

        verify(matchImportService)
            .importMatches(resolvedPlayer, response);

        verify(playerRepository)
            .save(resolvedPlayer);
    }

    /**
     * Verifies that a missing player produces a clear domain exception.
     */
    @Test
    void shouldThrowExceptionWhenPlayerDoesNotExist() {
        Long playerId = 99L;

        when(playerRepository.findById(playerId))
            .thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> service.synchronize(playerId)
        )
            .isInstanceOf(PlayerNotFoundException.class)
            .hasMessage("Player not found with id: 99");

        verify(playerRepository)
            .findById(playerId);

        verifyNoInteractions(
            accountResolutionService,
            mmrClient,
            mmrMapper,
            matchClient,
            matchImportService
        );

        verify(
            playerRepository,
            never()
        ).save(
            org.mockito.ArgumentMatchers.any(Player.class)
        );
    }

    /**
     * Verifies that exceptions raised by account resolution are propagated
     * and stop the synchronization pipeline.
     */
    @Test
    void shouldPropagateAccountResolutionFailure() {
        Long playerId = 1L;
        Player player = createPlayer(playerId);

        IllegalStateException resolutionFailure =
            new IllegalStateException(
                "Henrik account resolution failed"
            );

        when(playerRepository.findById(playerId))
            .thenReturn(Optional.of(player));

        when(accountResolutionService.resolvePuuid(player))
            .thenThrow(resolutionFailure);

        assertThatThrownBy(
            () -> service.synchronize(playerId)
        ).isSameAs(resolutionFailure);

        verify(playerRepository)
            .findById(playerId);

        verify(accountResolutionService)
            .resolvePuuid(player);

        verifyNoInteractions(
            mmrClient,
            mmrMapper,
            matchClient,
            matchImportService
        );

        verify(
            playerRepository,
            never()
        ).save(
            org.mockito.ArgumentMatchers.any(Player.class)
        );
    }

    /**
     * Verifies that a Henrik match retrieval failure is propagated and
     * prevents match import and final player persistence.
     */
    @Test
    void shouldPropagateMatchRetrievalFailure() {
        Long playerId = 1L;

        Player player = createPlayer(playerId);
        Player resolvedPlayer = createPlayer(playerId);
        resolvedPlayer.setRiotPuuid("resolved-puuid");

        IllegalStateException retrievalFailure =
            new IllegalStateException(
                "Henrik match retrieval failed"
            );

        when(playerRepository.findById(playerId))
            .thenReturn(Optional.of(player));

        when(accountResolutionService.resolvePuuid(player))
            .thenReturn(resolvedPlayer);

        HenrikMmrResponse mmrResponse = new HenrikMmrResponse(
            200,
            null
        );

        when(mmrClient.getCurrentMmr("resolved-puuid"))
            .thenReturn(mmrResponse);

        when(
            matchClient.getMatches(
                "resolved-puuid",
                0,
                RECENT_MATCH_PAGE_SIZE
            )
        ).thenThrow(retrievalFailure);

        assertThatThrownBy(
            () -> service.synchronize(playerId)
        ).isSameAs(retrievalFailure);

        assertThat(
            resolvedPlayer
                .getLastSuccessfulSynchronizationAt()
        ).isNull();

        verify(playerRepository)
            .findById(playerId);

        verify(accountResolutionService)
            .resolvePuuid(player);

        verify(matchClient).getMatches(
            "resolved-puuid",
            0,
            RECENT_MATCH_PAGE_SIZE
        );

        verifyNoInteractions(matchImportService);

        verify(
            playerRepository,
            never()
        ).save(
            org.mockito.ArgumentMatchers.any(Player.class)
        );
    }

    /**
     * Verifies that a match import failure is propagated and prevents the
     * synchronization date from being persisted.
     */
    @Test
    void shouldPropagateMatchImportFailure() {
        Long playerId = 1L;

        Player player = createPlayer(playerId);
        Player resolvedPlayer = createPlayer(playerId);
        resolvedPlayer.setRiotPuuid("resolved-puuid");

        HenrikMatchHistoryResponse response =
            new HenrikMatchHistoryResponse(
                200,
                List.of()
            );

        IllegalStateException importFailure =
            new IllegalStateException(
                "Match import failed"
            );

        when(playerRepository.findById(playerId))
            .thenReturn(Optional.of(player));

        when(accountResolutionService.resolvePuuid(player))
            .thenReturn(resolvedPlayer);

        when(
            matchClient.getMatches(
                "resolved-puuid",
                0,
                RECENT_MATCH_PAGE_SIZE
            )
        ).thenReturn(response);

        when(
            matchImportService.importMatches(
                resolvedPlayer,
                response
            )
        ).thenThrow(importFailure);

        assertThatThrownBy(
            () -> service.synchronize(playerId)
        ).isSameAs(importFailure);

        assertThat(
            resolvedPlayer
                .getLastSuccessfulSynchronizationAt()
        ).isNull();

        verify(playerRepository)
            .findById(playerId);

        verify(accountResolutionService)
            .resolvePuuid(player);

        verify(matchClient).getMatches(
            "resolved-puuid",
            0,
            RECENT_MATCH_PAGE_SIZE
        );

        verify(matchImportService)
            .importMatches(resolvedPlayer, response);

        verify(
            playerRepository,
            never()
        ).save(
            org.mockito.ArgumentMatchers.any(Player.class)
        );
    }

    /**
     * Creates a player used by the tests.
     *
     * @param playerId internal player identifier
     * @return configured player
     */
    private Player createPlayer(Long playerId) {
        Player player = new Player();
        player.setId(playerId);
        player.setGameName("Psilonnix");
        player.setTagLine("EUW");
        player.setDisplayName("Psilonnix");

        return player;
    }
}
