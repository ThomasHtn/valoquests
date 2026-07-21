package io.github.thomashtn.valorant.tracker.synchronization.service;

import io.github.thomashtn.valorant.tracker.henrik.exception.HenrikServiceUnavailableException;
import io.github.thomashtn.valorant.tracker.player.entity.Player;
import io.github.thomashtn.valorant.tracker.player.repository.PlayerRepository;
import io.github.thomashtn.valorant.tracker.player.model.PlayerStatus;
import io.github.thomashtn.valorant.tracker.synchronization.dto.SynchronizationResponse;
import io.github.thomashtn.valorant.tracker.synchronization.entity.Synchronization;
import io.github.thomashtn.valorant.tracker.synchronization.model.PlayerDeepSynchronizationResult;
import io.github.thomashtn.valorant.tracker.synchronization.model.PlayerSynchronizationResult;
import io.github.thomashtn.valorant.tracker.synchronization.model.SynchronizationStatus;
import io.github.thomashtn.valorant.tracker.synchronization.model.SynchronizationTrigger;
import io.github.thomashtn.valorant.tracker.synchronization.model.SynchronizationType;
import io.github.thomashtn.valorant.tracker.synchronization.repository.SynchronizationPlayerResultRepository;
import io.github.thomashtn.valorant.tracker.synchronization.repository.SynchronizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private PlayerDeepSynchronizationService
        playerDeepSynchronizationService;

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
            playerDeepSynchronizationService,
            playerRepository,
            synchronizationRepository,
            playerResultRepository,
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

        when(playerRepository.findAllByStatusOrderByIdAsc(PlayerStatus.ACTIVE))
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
     * Verifies that one player failure does not prevent later players from
     * being synchronized.
     */
    @Test
    void shouldReturnPartialStatusWhenOnePlayerFails() {
        Player firstPlayer = player(1L);
        Player secondPlayer = player(2L);

        when(playerRepository.findAllByStatusOrderByIdAsc(PlayerStatus.ACTIVE))
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

        when(playerRepository.findAllByStatusOrderByIdAsc(PlayerStatus.ACTIVE))
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
     * Verifies that an empty active-player list is treated as a successful
     * no-op.
     */
    @Test
    void shouldCompleteWhenNoActivePlayerExists() {
        when(playerRepository.findAllByStatusOrderByIdAsc(PlayerStatus.ACTIVE))
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
     * Verifies that the deep single-player command uses the shared orchestration.
     */
    @Test
    void shouldRecordCompletedDeepPlayerSynchronization() {
        Player player = player(4L);

        when(playerDeepSynchronizationService.synchronize(4L))
            .thenReturn(
                new PlayerDeepSynchronizationResult(
                    player,
                    6,
                    24,
                    PLAYER_TWO_COMPLETED_AT
                )
            );

        SynchronizationResponse response =
            service.requestDeepSynchronizationForPlayer(4L);

        assertThat(response.type()).isEqualTo(SynchronizationType.DEEP);
        assertThat(response.status())
            .isEqualTo(SynchronizationStatus.COMPLETED);
        assertThat(response.playersProcessed()).isEqualTo(1);
        assertThat(response.failureCount()).isZero();
        assertThat(response.matchesImported()).isEqualTo(24);
        assertThat(response.lastSuccessfulSynchronizationAt())
            .isEqualTo(PLAYER_TWO_COMPLETED_AT);
    }

    /**
     * Verifies that deep batch failures do not interrupt later players.
     */
    @Test
    void shouldReturnPartialStatusForDeepSynchronization() {
        Player firstPlayer = player(1L);
        Player secondPlayer = player(2L);

        when(playerRepository.findAllByStatusOrderByIdAsc(PlayerStatus.ACTIVE))
            .thenReturn(List.of(firstPlayer, secondPlayer));
        when(playerDeepSynchronizationService.synchronize(1L))
            .thenThrow(new IllegalStateException());
        when(playerDeepSynchronizationService.synchronize(2L))
            .thenReturn(
                new PlayerDeepSynchronizationResult(
                    secondPlayer,
                    3,
                    12,
                    PLAYER_TWO_COMPLETED_AT
                )
            );

        SynchronizationResponse response =
            service.requestDeepSynchronizationForAllPlayers();

        assertThat(response.type()).isEqualTo(SynchronizationType.DEEP);
        assertThat(response.status())
            .isEqualTo(SynchronizationStatus.PARTIAL);
        assertThat(response.failureCount()).isEqualTo(1);
        assertThat(response.matchesImported()).isEqualTo(12);
        assertThat(response.errorMessage())
            .contains("Player 1")
            .contains("IllegalStateException");

        verify(playerDeepSynchronizationService).synchronize(2L);
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
            completedAt
        );
    }
}
