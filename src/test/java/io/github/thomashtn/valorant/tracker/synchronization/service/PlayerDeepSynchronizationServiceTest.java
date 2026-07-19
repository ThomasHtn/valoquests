package io.github.thomashtn.valorant.tracker.synchronization.service;

import io.github.thomashtn.valorant.tracker.henrik.client.HenrikMatchClient;
import io.github.thomashtn.valorant.tracker.henrik.config.HenrikApiProperties;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchHistoryResponse;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchHistoryResponse.HenrikMatchData;
import io.github.thomashtn.valorant.tracker.match.service.MatchImportService;
import io.github.thomashtn.valorant.tracker.player.entity.Player;
import io.github.thomashtn.valorant.tracker.player.repository.PlayerRepository;
import io.github.thomashtn.valorant.tracker.player.service.PlayerAccountResolutionService;
import io.github.thomashtn.valorant.tracker.synchronization.model.PlayerDeepSynchronizationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PlayerDeepSynchronizationService}.
 */
@ExtendWith(MockitoExtension.class)
class PlayerDeepSynchronizationServiceTest {

    private static final long PLAYER_ID = 3L;

    private static final String RIOT_PUUID =
        "test-riot-puuid";

    private static final Instant COMPLETED_AT =
        Instant.parse("2026-07-18T15:00:00Z");

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private PlayerAccountResolutionService accountResolutionService;

    @Mock
    private HenrikMatchClient matchClient;

    @Mock
    private MatchImportService matchImportService;

    private PlayerDeepSynchronizationService service;

    private Player player;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
            COMPLETED_AT,
            ZoneOffset.UTC
        );

        HenrikApiProperties properties =
            new HenrikApiProperties(
                "https://api.henrikdev.xyz",
                "test-api-key",
                "eu",
                "pc",
                Duration.ofSeconds(2),
                Duration.ofSeconds(2),
                1,
                Duration.ofMillis(1),
                60_000,
                Duration.ZERO
            );

        service = new PlayerDeepSynchronizationService(
            playerRepository,
            accountResolutionService,
            matchClient,
            matchImportService,
            clock
        );

        player = new Player();
        player.setId(PLAYER_ID);
        player.setRiotPuuid(RIOT_PUUID);
    }

    /**
     * Verifies that pagination uses item offsets and stops on a short page.
     */
    @Test
    void shouldImportEveryAvailablePage() {
        HenrikMatchHistoryResponse firstPage =
            responseWithSize(10);

        HenrikMatchHistoryResponse secondPage =
            responseWithSize(10);

        HenrikMatchHistoryResponse thirdPage =
            responseWithSize(4);

        when(playerRepository.findById(PLAYER_ID))
            .thenReturn(java.util.Optional.of(player));

        when(accountResolutionService.resolvePuuid(player))
            .thenReturn(player);

        when(matchClient.getMatches(RIOT_PUUID, 0, 10))
            .thenReturn(firstPage);

        when(matchClient.getMatches(RIOT_PUUID, 10, 10))
            .thenReturn(secondPage);

        when(matchClient.getMatches(RIOT_PUUID, 20, 10))
            .thenReturn(thirdPage);

        when(
            matchImportService.importMatches(
                same(player),
                same(firstPage)
            )
        ).thenReturn(0);

        when(
            matchImportService.importMatches(
                same(player),
                same(secondPage)
            )
        ).thenReturn(8);

        when(
            matchImportService.importMatches(
                same(player),
                same(thirdPage)
            )
        ).thenReturn(3);

        when(playerRepository.save(player))
            .thenReturn(player);

        PlayerDeepSynchronizationResult result =
            service.synchronize(PLAYER_ID);

        assertThat(result.pagesFetched()).isEqualTo(3);
        assertThat(result.matchesImported()).isEqualTo(11);
        assertThat(result.completedAt()).isEqualTo(COMPLETED_AT);

        /*
         * The first page imported zero matches, but pagination continued.
         */
        InOrder requests = inOrder(matchClient);

        requests.verify(matchClient)
            .getMatches(RIOT_PUUID, 0, 10);

        requests.verify(matchClient)
            .getMatches(RIOT_PUUID, 10, 10);

        requests.verify(matchClient)
            .getMatches(RIOT_PUUID, 20, 10);

        assertThat(player.getLastSuccessfulSynchronizationAt())
            .isEqualTo(COMPLETED_AT);

        verify(playerRepository).save(player);
    }

    /**
     * Verifies that an immediately empty history completes normally.
     */
    @Test
    void shouldCompleteWhenHistoryIsEmpty() {
        HenrikMatchHistoryResponse emptyResponse =
            new HenrikMatchHistoryResponse(
                200,
                List.of()
            );

        when(playerRepository.findById(PLAYER_ID))
            .thenReturn(java.util.Optional.of(player));

        when(accountResolutionService.resolvePuuid(player))
            .thenReturn(player);

        when(matchClient.getMatches(RIOT_PUUID, 0, 10))
            .thenReturn(emptyResponse);

        when(playerRepository.save(player))
            .thenReturn(player);

        PlayerDeepSynchronizationResult result =
            service.synchronize(PLAYER_ID);

        assertThat(result.pagesFetched()).isZero();
        assertThat(result.matchesImported()).isZero();
        assertThat(result.completedAt()).isEqualTo(COMPLETED_AT);
    }

    /**
     * Creates a response containing the requested number of placeholder
     * matches. Their content is irrelevant because the import service is
     * mocked in this unit test.
     *
     * @param size response size
     * @return Henrik match-history response
     */
    private HenrikMatchHistoryResponse responseWithSize(int size) {
        List<HenrikMatchData> matches =
            Collections.nCopies(
                size,
                new HenrikMatchData(
                    null,
                    List.of(),
                    List.of()
                )
            );

        return new HenrikMatchHistoryResponse(
            200,
            matches
        );
    }
}
