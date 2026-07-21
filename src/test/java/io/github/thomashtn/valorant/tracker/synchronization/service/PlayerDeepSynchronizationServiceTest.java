package io.github.thomashtn.valorant.tracker.synchronization.service;

import io.github.thomashtn.valorant.tracker.henrik.client.HenrikMatchClient;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchHistoryResponse;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchHistoryResponse.HenrikMatchData;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchMetadata;
import io.github.thomashtn.valorant.tracker.match.service.MatchImportService;
import io.github.thomashtn.valorant.tracker.player.entity.Player;
import io.github.thomashtn.valorant.tracker.player.repository.PlayerRepository;
import io.github.thomashtn.valorant.tracker.player.service.PlayerAccountResolutionService;
import io.github.thomashtn.valorant.tracker.shared.config.ApplicationProperties;
import io.github.thomashtn.valorant.tracker.synchronization.model.DeepSynchronizationScope;
import io.github.thomashtn.valorant.tracker.synchronization.model.PlayerDeepSynchronizationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    private static final String CURRENT_SEASON_ID =
        "current-season";

    private static final String PREVIOUS_SEASON_ID =
        "previous-season";

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

    /**
     * Creates the service and common test data before each test.
     */
    @BeforeEach
    void setUp() {
        service = createService(
            DeepSynchronizationScope.CURRENT_SEASON
        );

        player = new Player();
        player.setId(PLAYER_ID);
        player.setRiotPuuid(RIOT_PUUID);
    }

    /**
     * Verifies that current-season pagination uses item offsets and stops on
     * a short page.
     */
    @Test
    void shouldImportEveryAvailableCurrentSeasonPage() {
        HenrikMatchHistoryResponse firstPage =
            responseWithSize(
                10,
                CURRENT_SEASON_ID,
                0
            );

        HenrikMatchHistoryResponse secondPage =
            responseWithSize(
                10,
                CURRENT_SEASON_ID,
                10
            );

        HenrikMatchHistoryResponse thirdPage =
            responseWithSize(
                4,
                CURRENT_SEASON_ID,
                20
            );

        preparePlayerResolution();

        when(matchClient.getMatches(RIOT_PUUID, 0, 10))
            .thenReturn(firstPage);

        when(matchClient.getMatches(RIOT_PUUID, 10, 10))
            .thenReturn(secondPage);

        when(matchClient.getMatches(RIOT_PUUID, 20, 10))
            .thenReturn(thirdPage);

        when(
            matchImportService.importMatches(
                same(player),
                any(HenrikMatchHistoryResponse.class)
            )
        ).thenReturn(0, 8, 3);

        when(playerRepository.save(player))
            .thenReturn(player);

        PlayerDeepSynchronizationResult result =
            service.synchronize(PLAYER_ID);

        assertThat(result.pagesFetched())
            .isEqualTo(3);

        assertThat(result.matchesImported())
            .isEqualTo(11);

        assertThat(result.completedAt())
            .isEqualTo(COMPLETED_AT);

        assertThat(result.player())
            .isSameAs(player);

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
     * Verifies that the current-season mode stops when a previous-season match
     * is encountered.
     */
    @Test
    void shouldStopWhenPreviousSeasonIsReached() {
        HenrikMatchHistoryResponse firstPage =
            responseWithSize(
                10,
                CURRENT_SEASON_ID,
                0
            );

        List<HenrikMatchData> boundaryMatches =
            new ArrayList<>();

        boundaryMatches.addAll(
            responseWithSize(
                4,
                CURRENT_SEASON_ID,
                10
            ).data()
        );

        boundaryMatches.addAll(
            responseWithSize(
                6,
                PREVIOUS_SEASON_ID,
                14
            ).data()
        );

        HenrikMatchHistoryResponse boundaryPage =
            new HenrikMatchHistoryResponse(
                200,
                boundaryMatches
            );

        preparePlayerResolution();

        when(matchClient.getMatches(RIOT_PUUID, 0, 10))
            .thenReturn(firstPage);

        when(matchClient.getMatches(RIOT_PUUID, 10, 10))
            .thenReturn(boundaryPage);

        when(
            matchImportService.importMatches(
                same(player),
                any(HenrikMatchHistoryResponse.class)
            )
        ).thenReturn(10, 4);

        when(playerRepository.save(player))
            .thenReturn(player);

        PlayerDeepSynchronizationResult result =
            service.synchronize(PLAYER_ID);

        assertThat(result.pagesFetched())
            .isEqualTo(2);

        assertThat(result.matchesImported())
            .isEqualTo(14);

        verify(matchClient, never())
            .getMatches(RIOT_PUUID, 20, 10);

        ArgumentCaptor<HenrikMatchHistoryResponse> responseCaptor =
            ArgumentCaptor.forClass(
                HenrikMatchHistoryResponse.class
            );

        verify(matchImportService, times(2))
            .importMatches(
                same(player),
                responseCaptor.capture()
            );

        List<HenrikMatchHistoryResponse> importedResponses =
            responseCaptor.getAllValues();

        assertThat(importedResponses)
            .hasSize(2);

        HenrikMatchHistoryResponse filteredBoundaryResponse =
            importedResponses.get(1);

        assertThat(filteredBoundaryResponse.data())
            .hasSize(4)
            .allSatisfy(match ->
                assertThat(
                    match.metadata()
                        .season()
                        .id()
                ).isEqualTo(CURRENT_SEASON_ID)
            );

        verify(playerRepository).save(player);
    }

    /**
     * Verifies that no previous-season match is imported when the season
     * boundary occurs on the first page.
     */
    @Test
    void shouldImportOnlyCurrentSeasonMatchesFromFirstPage() {
        List<HenrikMatchData> matches =
            new ArrayList<>();

        matches.addAll(
            responseWithSize(
                3,
                CURRENT_SEASON_ID,
                0
            ).data()
        );

        matches.addAll(
            responseWithSize(
                7,
                PREVIOUS_SEASON_ID,
                3
            ).data()
        );

        HenrikMatchHistoryResponse response =
            new HenrikMatchHistoryResponse(
                200,
                matches
            );

        preparePlayerResolution();

        when(matchClient.getMatches(RIOT_PUUID, 0, 10))
            .thenReturn(response);

        when(
            matchImportService.importMatches(
                same(player),
                any(
                    HenrikMatchHistoryResponse.class
                )
            )
        ).thenReturn(3);

        when(playerRepository.save(player))
            .thenReturn(player);

        PlayerDeepSynchronizationResult result =
            service.synchronize(PLAYER_ID);

        assertThat(result.pagesFetched())
            .isEqualTo(1);

        assertThat(result.matchesImported())
            .isEqualTo(3);

        ArgumentCaptor<HenrikMatchHistoryResponse> responseCaptor =
            ArgumentCaptor.forClass(
                HenrikMatchHistoryResponse.class
            );

        verify(matchImportService)
            .importMatches(
                same(player),
                responseCaptor.capture()
            );

        HenrikMatchHistoryResponse importedResponse =
            responseCaptor.getValue();

        assertThat(importedResponse.data())
            .hasSize(3)
            .allSatisfy(match ->
                assertThat(
                    match.metadata()
                        .season()
                        .id()
                ).isEqualTo(CURRENT_SEASON_ID)
            );

        verify(matchClient, never())
            .getMatches(RIOT_PUUID, 10, 10);
    }

    /**
     * Verifies that the all-history mode continues beyond the current-season
     * boundary.
     */
    @Test
    void shouldContinueBeyondCurrentSeasonWhenScopeIsAllHistory() {
        service = createService(
            DeepSynchronizationScope.ALL_HISTORY
        );

        HenrikMatchHistoryResponse currentSeasonPage =
            responseWithSize(
                10,
                CURRENT_SEASON_ID,
                0
            );

        HenrikMatchHistoryResponse previousSeasonPage =
            responseWithSize(
                10,
                PREVIOUS_SEASON_ID,
                10
            );

        HenrikMatchHistoryResponse lastPage =
            responseWithSize(
                3,
                PREVIOUS_SEASON_ID,
                20
            );

        preparePlayerResolution();

        when(matchClient.getMatches(RIOT_PUUID, 0, 10))
            .thenReturn(currentSeasonPage);

        when(matchClient.getMatches(RIOT_PUUID, 10, 10))
            .thenReturn(previousSeasonPage);

        when(matchClient.getMatches(RIOT_PUUID, 20, 10))
            .thenReturn(lastPage);

        when(
            matchImportService.importMatches(
                same(player),
                any(HenrikMatchHistoryResponse.class)
            )
        ).thenReturn(10, 10, 3);

        when(playerRepository.save(player))
            .thenReturn(player);

        PlayerDeepSynchronizationResult result =
            service.synchronize(PLAYER_ID);

        assertThat(result.pagesFetched())
            .isEqualTo(3);

        assertThat(result.matchesImported())
            .isEqualTo(23);

        verify(matchClient)
            .getMatches(RIOT_PUUID, 20, 10);

        verify(matchImportService, times(3))
            .importMatches(
                same(player),
                any(
                    HenrikMatchHistoryResponse.class
                )
            );
    }

    /**
     * Verifies that current-season synchronization fails clearly when Henrik
     * does not expose any season identifier.
     */
    @Test
    void shouldFailWhenCurrentSeasonCannotBeResolved() {
        HenrikMatchData matchWithoutSeason = new HenrikMatchData(
            new HenrikMatchMetadata(
                "match-without-season",
                null,
                1_000L,
                Instant.parse("2026-07-18T10:00:00Z"),
                true,
                null,
                null
            ),
            List.of(),
            List.of()
        );

        preparePlayerResolution();

        when(matchClient.getMatches(RIOT_PUUID, 0, 10))
            .thenReturn(new HenrikMatchHistoryResponse(200, List.of(matchWithoutSeason)));

        assertThatThrownBy(() -> service.synchronize(PLAYER_ID))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Unable to determine the current season from Henrik matches");

        verify(matchImportService, never())
            .importMatches(same(player), any(HenrikMatchHistoryResponse.class));
        verify(playerRepository, never()).save(player);
    }

    /**
     * Verifies that all-history synchronization does not require season
     * metadata to import a page.
     */
    @Test
    void shouldImportMatchesWithoutSeasonInAllHistoryMode() {
        service = createService(DeepSynchronizationScope.ALL_HISTORY);

        HenrikMatchData matchWithoutSeason = new HenrikMatchData(
            new HenrikMatchMetadata(
                "match-without-season",
                null,
                1_000L,
                Instant.parse("2026-07-18T10:00:00Z"),
                true,
                null,
                null
            ),
            List.of(),
            List.of()
        );
        HenrikMatchHistoryResponse response = new HenrikMatchHistoryResponse(
            200,
            List.of(matchWithoutSeason)
        );

        preparePlayerResolution();

        when(matchClient.getMatches(RIOT_PUUID, 0, 10)).thenReturn(response);
        when(matchImportService.importMatches(player, response)).thenReturn(1);
        when(playerRepository.save(player)).thenReturn(player);

        PlayerDeepSynchronizationResult result = service.synchronize(PLAYER_ID);

        assertThat(result.pagesFetched()).isEqualTo(1);
        assertThat(result.matchesImported()).isEqualTo(1);
        verify(matchImportService).importMatches(player, response);
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

        preparePlayerResolution();

        when(matchClient.getMatches(RIOT_PUUID, 0, 10))
            .thenReturn(emptyResponse);

        when(playerRepository.save(player))
            .thenReturn(player);

        PlayerDeepSynchronizationResult result =
            service.synchronize(PLAYER_ID);

        assertThat(result.pagesFetched())
            .isZero();

        assertThat(result.matchesImported())
            .isZero();

        assertThat(result.completedAt())
            .isEqualTo(COMPLETED_AT);

        verify(
            matchImportService,
            never()
        ).importMatches(
            same(player),
            any(
                HenrikMatchHistoryResponse.class
            )
        );

        verify(playerRepository).save(player);
    }

    /**
     * Creates a service configured with the requested deep-synchronization
     * scope.
     *
     * @param scope history range used by the service
     * @return configured service
     */
    private PlayerDeepSynchronizationService createService(
        DeepSynchronizationScope scope
    ) {
        Clock clock = Clock.fixed(
            COMPLETED_AT,
            ZoneOffset.UTC
        );

        ApplicationProperties applicationProperties =
            createApplicationProperties(scope);

        return new PlayerDeepSynchronizationService(
            playerRepository,
            accountResolutionService,
            matchClient,
            matchImportService,
            applicationProperties,
            clock
        );
    }

    /**
     * Creates the application properties used by unit tests.
     *
     * @param scope history range used during deep synchronization
     * @return application properties
     */
    private ApplicationProperties createApplicationProperties(
        DeepSynchronizationScope scope
    ) {
        return new ApplicationProperties(
            "http://localhost:4200",
            "test-admin-key",
            new ApplicationProperties.DeepSynchronization(
                scope
            )
        );
    }

    /**
     * Configures the common player lookup and PUUID-resolution behavior.
     */
    private void preparePlayerResolution() {
        when(playerRepository.findById(PLAYER_ID))
            .thenReturn(Optional.of(player));

        when(accountResolutionService.resolvePuuid(player))
            .thenReturn(player);
    }

    /**
     * Creates a Henrik response containing matches from one season.
     *
     * @param size            number of matches to create
     * @param seasonId        Henrik season identifier
     * @param firstMatchIndex index used to generate unique match identifiers
     * @return Henrik match-history response
     */
    private HenrikMatchHistoryResponse responseWithSize(
        int size,
        String seasonId,
        int firstMatchIndex
    ) {
        List<HenrikMatchData> matches =
            java.util.stream.IntStream.range(0, size)
                .mapToObj(index ->
                    createMatch(
                        "match-" + (firstMatchIndex + index),
                        seasonId
                    )
                )
                .toList();

        return new HenrikMatchHistoryResponse(
            200,
            matches
        );
    }

    /**
     * Creates one Henrik match with valid season metadata.
     *
     * @param matchId  external match identifier
     * @param seasonId Henrik season identifier
     * @return Henrik match
     */
    private HenrikMatchData createMatch(
        String matchId,
        String seasonId
    ) {
        HenrikMatchMetadata metadata =
            new HenrikMatchMetadata(
                matchId,
                null,
                1_000L,
                Instant.parse("2026-07-18T10:00:00Z"),
                true,
                null,
                new HenrikMatchMetadata.HenrikSeason(
                    seasonId,
                    seasonId
                )
            );

        return new HenrikMatchData(
            metadata,
            List.of(),
            List.of()
        );
    }
}
