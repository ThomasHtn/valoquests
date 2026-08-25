package io.github.thomashtn.valoquests.synchronization.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.challenge.service.ChallengeRecalculationService;
import io.github.thomashtn.valoquests.colony.service.ColonyReplayService;
import io.github.thomashtn.valoquests.henrik.exception.HenrikServiceUnavailableException;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.synchronization.dto.SynchronizationResponse;
import io.github.thomashtn.valoquests.synchronization.entity.Synchronization;
import io.github.thomashtn.valoquests.synchronization.entity.SynchronizationPlayerResult;
import io.github.thomashtn.valoquests.synchronization.model.PlayerSynchronizationResult;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationStatus;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationStopReason;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationTrigger;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationType;
import io.github.thomashtn.valoquests.synchronization.repository.SynchronizationPlayerResultRepository;
import io.github.thomashtn.valoquests.synchronization.repository.SynchronizationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link DefaultSynchronizationCommandService}.
 */
@ExtendWith(MockitoExtension.class)
class DefaultSynchronizationCommandServiceTest {

    private static final Instant STARTED_AT =
        Instant.parse("2026-07-18T14:00:00Z");

    private static final Instant PLAYER_ONE_COMPLETED_AT =
        Instant.parse("2026-07-18T14:00:03Z");

    private static final Instant PLAYER_TWO_COMPLETED_AT =
        Instant.parse("2026-07-18T14:00:05Z");

    @Mock
    private PlayerSynchronizationService playerSynchronizationService;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private SynchronizationRepository synchronizationRepository;

    @Mock
    private SynchronizationPlayerResultRepository playerResultRepository;

    @Mock
    private ChallengeRecalculationService challengeRecalculationService;

    @Mock
    private ColonyReplayService colonyReplayService;

    private DefaultSynchronizationCommandService service;

    /**
     * Creates the service under test with a deterministic clock.
     */
    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
            STARTED_AT,
            ZoneOffset.UTC
        );

        service = new DefaultSynchronizationCommandService(
            playerSynchronizationService,
            playerRepository,
            synchronizationRepository,
            playerResultRepository,
            challengeRecalculationService,
            colonyReplayService,
            clock
        );

        when(synchronizationRepository.save(any(Synchronization.class)))
            .thenAnswer(invocation -> {
                Synchronization synchronization = invocation.getArgument(0);

                if (synchronization.getId() == null) {
                    synchronization.setId(10L);
                }

                return synchronization;
            });
    }

    /**
     * Verifies that all successful player executions produce a completed
     * global synchronization.
     */
    @Test
    void shouldSynchronizeAllActivePlayers() {
        Player firstPlayer = player(1L);
        Player secondPlayer = player(2L);

        when(playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED))
            .thenReturn(List.of(firstPlayer, secondPlayer));

        when(playerSynchronizationService.synchronize(1L))
            .thenReturn(
                result(
                    firstPlayer,
                    10,
                    PLAYER_ONE_COMPLETED_AT
                )
            );

        when(playerSynchronizationService.synchronize(2L))
            .thenReturn(
                result(
                    secondPlayer,
                    4,
                    PLAYER_TWO_COMPLETED_AT
                )
            );

        SynchronizationResponse response =
            service.synchronizeAllPlayers();

        assertThat(response.status())
            .isEqualTo(SynchronizationStatus.COMPLETED);
        assertThat(response.playersProcessed()).isEqualTo(2);
        assertThat(response.failureCount()).isZero();
        assertThat(response.matchesImported()).isEqualTo(14);
        assertThat(response.lastSuccessfulSynchronizationAt())
            .isEqualTo(PLAYER_TWO_COMPLETED_AT);
        assertThat(response.errorMessage()).isNull();

        InOrder orderedSynchronizations = inOrder(
            playerSynchronizationService
        );

        orderedSynchronizations
            .verify(playerSynchronizationService)
            .synchronize(1L);

        orderedSynchronizations
            .verify(playerSynchronizationService)
            .synchronize(2L);
    }

    /**
     * Verifies that an inactive player is still synchronized: only its ranking slot and boss
     * damage are excluded, never its match import.
     */
    @Test
    void shouldSynchronizeInactivePlayersToo() {
        Player activePlayer = player(1L);
        Player inactivePlayer = player(2L);
        inactivePlayer.setStatus(PlayerStatus.INACTIVE);

        when(playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED))
            .thenReturn(List.of(activePlayer, inactivePlayer));

        when(playerSynchronizationService.synchronize(1L))
            .thenReturn(result(activePlayer, 10, PLAYER_ONE_COMPLETED_AT));

        when(playerSynchronizationService.synchronize(2L))
            .thenReturn(result(inactivePlayer, 4, PLAYER_TWO_COMPLETED_AT));

        SynchronizationResponse response = service.synchronizeAllPlayers();

        assertThat(response.playersProcessed()).isEqualTo(2);
        verify(playerSynchronizationService).synchronize(1L);
        verify(playerSynchronizationService).synchronize(2L);
    }

    /**
     * Verifies that one player failure does not prevent later players from
     * being synchronized.
     */
    @Test
    void shouldReturnPartialStatusWhenOnePlayerFails() {
        Player firstPlayer = player(1L);
        Player secondPlayer = player(2L);

        when(playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED))
            .thenReturn(List.of(firstPlayer, secondPlayer));

        when(playerSynchronizationService.synchronize(1L))
            .thenThrow(
                new HenrikServiceUnavailableException(
                    "Henrik unavailable"
                )
            );

        when(playerSynchronizationService.synchronize(2L))
            .thenReturn(
                result(
                    secondPlayer,
                    5,
                    PLAYER_TWO_COMPLETED_AT
                )
            );

        SynchronizationResponse response =
            service.synchronizeAllPlayers();

        assertThat(response.status())
            .isEqualTo(SynchronizationStatus.PARTIAL);
        assertThat(response.playersProcessed()).isEqualTo(2);
        assertThat(response.failureCount()).isEqualTo(1);
        assertThat(response.matchesImported()).isEqualTo(5);
        assertThat(response.lastSuccessfulSynchronizationAt())
            .isEqualTo(PLAYER_TWO_COMPLETED_AT);
        assertThat(response.errorMessage())
            .contains("Player 1")
            .contains("Henrik unavailable");

        verify(playerSynchronizationService).synchronize(2L);
    }

    /**
     * Verifies that a total failure produces a failed global execution without
     * rethrowing individual player exceptions.
     */
    @Test
    void shouldReturnFailedStatusWhenEveryPlayerFails() {
        Player firstPlayer = player(1L);
        Player secondPlayer = player(2L);

        when(playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED))
            .thenReturn(List.of(firstPlayer, secondPlayer));

        when(playerSynchronizationService.synchronize(1L))
            .thenThrow(new IllegalStateException("First failure"));

        when(playerSynchronizationService.synchronize(2L))
            .thenThrow(new IllegalStateException("Second failure"));

        SynchronizationResponse response =
            service.synchronizeAllPlayers();

        assertThat(response.status())
            .isEqualTo(SynchronizationStatus.FAILED);
        assertThat(response.playersProcessed()).isEqualTo(2);
        assertThat(response.failureCount()).isEqualTo(2);
        assertThat(response.matchesImported()).isZero();
        assertThat(response.lastSuccessfulSynchronizationAt()).isNull();
        assertThat(response.errorMessage())
            .contains("Player 1")
            .contains("First failure")
            .contains("Player 2")
            .contains("Second failure");
    }

    /**
     * Verifies that an automatic batch execution keeps its scheduled origin.
     */
    @Test
    void shouldRecordScheduledSynchronizationTrigger() {
        when(playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED))
            .thenReturn(List.of());

        SynchronizationResponse response = service.synchronizeAllPlayers(
            SynchronizationTrigger.SCHEDULED
        );

        assertThat(response.trigger())
            .isEqualTo(SynchronizationTrigger.SCHEDULED);
    }

    /**
     * Verifies that an empty active-player list is treated as a successful
     * no-op.
     */
    @Test
    void shouldCompleteWhenNoActivePlayerExists() {
        when(playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED))
            .thenReturn(List.of());

        SynchronizationResponse response =
            service.synchronizeAllPlayers();

        assertThat(response.status())
            .isEqualTo(SynchronizationStatus.COMPLETED);
        assertThat(response.playersProcessed()).isZero();
        assertThat(response.failureCount()).isZero();
        assertThat(response.matchesImported()).isZero();
        assertThat(response.lastSuccessfulSynchronizationAt()).isNull();
    }

    /**
     * Verifies that a successful individual import is recorded as completed.
     */
    @Test
    void shouldRecordCompletedPlayerSynchronization() {
        Player player = player(3L);

        when(playerSynchronizationService.synchronize(3L))
            .thenReturn(
                result(
                    player,
                    7,
                    PLAYER_ONE_COMPLETED_AT
                )
            );

        SynchronizationResponse response =
            service.synchronizePlayer(3L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.type())
            .isEqualTo(SynchronizationType.STANDARD);
        assertThat(response.trigger())
            .isEqualTo(SynchronizationTrigger.MANUAL);
        assertThat(response.status())
            .isEqualTo(SynchronizationStatus.COMPLETED);
        assertThat(response.startedAt()).isEqualTo(STARTED_AT);
        assertThat(response.finishedAt())
            .isEqualTo(PLAYER_ONE_COMPLETED_AT);
        assertThat(response.playersProcessed()).isEqualTo(1);
        assertThat(response.failureCount()).isZero();
        assertThat(response.matchesImported()).isEqualTo(7);
        assertThat(response.errorMessage()).isNull();
    }

    /**
     * Verifies that an individual failure is recorded and propagated.
     */
    @Test
    void shouldRecordFailedPlayerSynchronization() {
        HenrikServiceUnavailableException exception =
            new HenrikServiceUnavailableException(
                "Henrik service is unavailable"
            );

        when(playerSynchronizationService.synchronize(3L))
            .thenThrow(exception);

        assertThatThrownBy(
            () -> service.synchronizePlayer(3L)
        ).isSameAs(exception);

        verify(
            synchronizationRepository,
            times(2)
        ).save(any(Synchronization.class));
    }

    /**
     * Verifies that each persisted player result records why its match-history walk stopped.
     *
     * <p>This is what makes a short import self-explanatory: without it, a run that simply reached
     * the end of the player's current season is indistinguishable from one that was truncated. A
     * failed player never completed a walk, so it reports no stop reason at all.
     */
    @Test
    void shouldRecordWhyEachPlayerWalkStopped() {
        Player firstPlayer = player(1L);
        Player secondPlayer = player(2L);

        when(playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED))
            .thenReturn(List.of(firstPlayer, secondPlayer));

        when(playerSynchronizationService.synchronize(1L))
            .thenThrow(
                new HenrikServiceUnavailableException(
                    "Henrik unavailable"
                )
            );

        when(playerSynchronizationService.synchronize(2L))
            .thenReturn(
                new PlayerSynchronizationResult(
                    secondPlayer,
                    1,
                    5,
                    PLAYER_TWO_COMPLETED_AT,
                    SynchronizationStopReason.PAGE_LIMIT_REACHED
                )
            );

        service.synchronizeAllPlayers();

        ArgumentCaptor<SynchronizationPlayerResult> results =
            ArgumentCaptor.forClass(SynchronizationPlayerResult.class);
        verify(playerResultRepository, times(2)).save(results.capture());

        assertThat(results.getAllValues())
            .extracting(
                result -> result.getPlayer().getId(),
                SynchronizationPlayerResult::getStopReason
            )
            .containsExactly(
                tuple(1L, null),
                tuple(2L, SynchronizationStopReason.PAGE_LIMIT_REACHED)
            );
    }

    /**
     * Verifies that importing matches rebuilds the current week's challenge progress.
     *
     * <p>Progress and the weekly ranking are derived from the stored matches, so without this step
     * a scheduled run would import matches the challenges never count.
     */
    @Test
    void shouldRecalculateChallengeProgressAfterImportingMatches() {
        Player firstPlayer = player(1L);

        when(playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED))
            .thenReturn(List.of(firstPlayer));

        when(playerSynchronizationService.synchronize(1L))
            .thenReturn(result(firstPlayer, 3, PLAYER_ONE_COMPLETED_AT));

        service.synchronizeAllPlayers();

        verify(challengeRecalculationService).recalculateCurrentWeekProgress();
    }

    /**
     * Verifies that a run importing nothing leaves challenge progress untouched.
     *
     * <p>Progress depends only on stored matches, so recalculating without a new one would burn a
     * full pass over every player and challenge to rewrite identical values.
     */
    @Test
    void shouldNotRecalculateChallengeProgressWhenNothingWasImported() {
        Player firstPlayer = player(1L);

        when(playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED))
            .thenReturn(List.of(firstPlayer));

        when(playerSynchronizationService.synchronize(1L))
            .thenReturn(result(firstPlayer, 0, PLAYER_ONE_COMPLETED_AT));

        service.synchronizeAllPlayers();

        verifyNoInteractions(challengeRecalculationService);
    }

    /**
     * Verifies that a failed recalculation does not fail an otherwise successful import.
     *
     * <p>The matches are already committed when recalculation runs. Propagating its failure would
     * report a successful import as failed and discard the summary of every processed player.
     */
    @Test
    void shouldReportSuccessWhenChallengeRecalculationFails() {
        Player firstPlayer = player(1L);

        when(playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED))
            .thenReturn(List.of(firstPlayer));

        when(playerSynchronizationService.synchronize(1L))
            .thenReturn(result(firstPlayer, 3, PLAYER_ONE_COMPLETED_AT));

        doThrow(new IllegalStateException("recalculation failed"))
            .when(challengeRecalculationService)
            .recalculateCurrentWeekProgress();

        SynchronizationResponse response = service.synchronizeAllPlayers();

        assertThat(response.status())
            .isEqualTo(SynchronizationStatus.COMPLETED);
        assertThat(response.matchesImported()).isEqualTo(3);
        assertThat(response.errorMessage()).isNull();
    }

    /**
     * Verifies that a single-player import also rebuilds the challenge progress.
     */
    @Test
    void shouldRecalculateChallengeProgressAfterSinglePlayerImport() {
        Player firstPlayer = player(1L);

        when(playerSynchronizationService.synchronize(1L))
            .thenReturn(result(firstPlayer, 7, PLAYER_ONE_COMPLETED_AT));

        service.synchronizePlayer(1L);

        verify(challengeRecalculationService).recalculateCurrentWeekProgress();
    }

    /**
     * Creates a player for a test case.
     *
     * @param id player identifier
     * @return initialized player
     */
    private Player player(long id) {
        Player player = new Player();
        player.setId(id);
        return player;
    }

    /**
     * Creates a successful player synchronization result.
     *
     * @param player          synchronized player
     * @param matchesImported imported match count
     * @param completedAt     completion timestamp
     * @return synchronization result
     */
    private PlayerSynchronizationResult result(
        Player player,
        int matchesImported,
        Instant completedAt
    ) {
        return new PlayerSynchronizationResult(
            player,
            1,
            matchesImported,
            completedAt,
            SynchronizationStopReason.SEASON_BOUNDARY
        );
    }
}
