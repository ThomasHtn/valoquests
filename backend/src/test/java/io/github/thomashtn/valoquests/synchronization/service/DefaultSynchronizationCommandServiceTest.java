package io.github.thomashtn.valoquests.synchronization.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.campaign.service.CampaignReplayService;
import io.github.thomashtn.valoquests.challenge.service.ChallengeRecalculationService;
import io.github.thomashtn.valoquests.henrik.exception.HenrikServiceUnavailableException;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.exception.PlayerNotFoundException;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Unit tests for {@link DefaultSynchronizationCommandService}.
 */
@ExtendWith(MockitoExtension.class)
class DefaultSynchronizationCommandServiceTest {

    private static final Instant STARTED_AT =
        Instant.parse("2026-07-18T14:00:00Z");

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
    private CampaignReplayService campaignReplayService;

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
            new SynchronizationRecorder(synchronizationRepository, playerResultRepository, clock),
            challengeRecalculationService,
            campaignReplayService
        );

        // Shared by most tests; lenient so the fail-fast paths that never save do not trip strict stubs.
        lenient().when(synchronizationRepository.save(any(Synchronization.class)))
            .thenAnswer(invocation -> {
                Synchronization synchronization = invocation.getArgument(0);

                if (synchronization.getId() == null) {
                    synchronization.setId(10L);
                }

                return synchronization;
            });
    }

    /**
     * Verifies that a batch run inside a transaction fails before recording anything.
     *
     * <p>Checked only per player, the failure would be swallowed and stored as a player failure.
     */
    @Test
    @DisplayName("Fails fast when a batch synchronization is called inside a transaction")
    void shouldRejectABatchRunInsideAnActiveTransaction() {
        TransactionSynchronizationManager.setActualTransactionActive(true);

        try {
            assertThatThrownBy(() -> service.synchronizeAllPlayers(SynchronizationTrigger.MANUAL))
                .isInstanceOf(IllegalStateException.class);

            verifyNoInteractions(synchronizationRepository, playerSynchronizationService);
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    /**
     * Verifies that a single-player run inside a transaction fails before recording anything.
     */
    @Test
    @DisplayName("Fails fast when a single-player synchronization is called inside a transaction")
    void shouldRejectASinglePlayerRunInsideAnActiveTransaction() {
        TransactionSynchronizationManager.setActualTransactionActive(true);

        try {
            assertThatThrownBy(() -> service.synchronizePlayer(1L))
                .isInstanceOf(IllegalStateException.class);

            verifyNoInteractions(synchronizationRepository, playerSynchronizationService);
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
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
                result(firstPlayer, 10)
            );

        when(playerSynchronizationService.synchronize(2L))
            .thenReturn(
                result(secondPlayer, 4)
            );

        service.synchronizeAllPlayers(SynchronizationTrigger.MANUAL);
        Synchronization execution = lastSavedSynchronization();

        assertThat(execution.getStatus())
            .isEqualTo(SynchronizationStatus.COMPLETED);
        assertThat(execution.getPlayersProcessed()).isEqualTo(2);
        assertThat(execution.getFailureCount()).isZero();
        assertThat(execution.getMatchesImported()).isEqualTo(14);
        assertThat(execution.getErrorMessage()).isNull();

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
            .thenReturn(result(activePlayer, 10));

        when(playerSynchronizationService.synchronize(2L))
            .thenReturn(result(inactivePlayer, 4));

        service.synchronizeAllPlayers(SynchronizationTrigger.MANUAL);
        Synchronization execution = lastSavedSynchronization();

        assertThat(execution.getPlayersProcessed()).isEqualTo(2);
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
                result(secondPlayer, 5)
            );

        service.synchronizeAllPlayers(SynchronizationTrigger.MANUAL);
        Synchronization execution = lastSavedSynchronization();

        assertThat(execution.getStatus())
            .isEqualTo(SynchronizationStatus.PARTIAL);
        assertThat(execution.getPlayersProcessed()).isEqualTo(2);
        assertThat(execution.getFailureCount()).isEqualTo(1);
        assertThat(execution.getMatchesImported()).isEqualTo(5);
        assertThat(execution.getErrorMessage())
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

        service.synchronizeAllPlayers(SynchronizationTrigger.MANUAL);
        Synchronization execution = lastSavedSynchronization();

        assertThat(execution.getStatus())
            .isEqualTo(SynchronizationStatus.FAILED);
        assertThat(execution.getPlayersProcessed()).isEqualTo(2);
        assertThat(execution.getFailureCount()).isEqualTo(2);
        assertThat(execution.getMatchesImported()).isZero();
        assertThat(execution.getErrorMessage())
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

        service.synchronizeAllPlayers(SynchronizationTrigger.SCHEDULED);
        Synchronization execution = lastSavedSynchronization();

        assertThat(execution.getTrigger())
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

        service.synchronizeAllPlayers(SynchronizationTrigger.MANUAL);
        Synchronization execution = lastSavedSynchronization();

        assertThat(execution.getStatus())
            .isEqualTo(SynchronizationStatus.COMPLETED);
        assertThat(execution.getPlayersProcessed()).isZero();
        assertThat(execution.getFailureCount()).isZero();
        assertThat(execution.getMatchesImported()).isZero();
    }

    /**
     * Verifies that a successful individual import is recorded as completed.
     */
    @Test
    void shouldRecordCompletedPlayerSynchronization() {
        Player player = player(3L);

        when(playerRepository.findById(3L))
            .thenReturn(Optional.of(player));
        when(playerSynchronizationService.synchronize(3L))
            .thenReturn(
                result(player, 7)
            );

        service.synchronizePlayer(3L);
        Synchronization execution = lastSavedSynchronization();

        assertThat(execution.getId()).isEqualTo(10L);
        assertThat(execution.getType())
            .isEqualTo(SynchronizationType.STANDARD);
        assertThat(execution.getTrigger())
            .isEqualTo(SynchronizationTrigger.MANUAL);
        assertThat(execution.getStatus())
            .isEqualTo(SynchronizationStatus.COMPLETED);
        assertThat(execution.getStartedAt()).isEqualTo(STARTED_AT);
        assertThat(execution.getFinishedAt())
            .isEqualTo(STARTED_AT);
        assertThat(execution.getPlayersProcessed()).isEqualTo(1);
        assertThat(execution.getFailureCount()).isZero();
        assertThat(execution.getMatchesImported()).isEqualTo(7);
        assertThat(execution.getErrorMessage()).isNull();
    }

    /**
     * Verifies that an individual failure is recorded and produces a failed player result, without
     * being thrown back to the caller.
     */
    @Test
    void shouldRecordFailedPlayerSynchronization() {
        Player player = player(3L);
        HenrikServiceUnavailableException exception =
            new HenrikServiceUnavailableException(
                "Henrik service is unavailable"
            );

        when(playerRepository.findById(3L))
            .thenReturn(Optional.of(player));
        when(playerSynchronizationService.synchronize(3L))
            .thenThrow(exception);

        service.synchronizePlayer(3L);

        assertThat(lastSavedSynchronization().getStatus()).isEqualTo(SynchronizationStatus.FAILED);
        verify(
            synchronizationRepository,
            times(2)
        ).save(any(Synchronization.class));

        ArgumentCaptor<SynchronizationPlayerResult> playerResult =
            ArgumentCaptor.forClass(SynchronizationPlayerResult.class);
        verify(playerResultRepository).save(playerResult.capture());

        assertThat(playerResult.getValue().getPlayer().getId()).isEqualTo(3L);
        assertThat(playerResult.getValue().getStatus())
            .isEqualTo(SynchronizationStatus.FAILED);
    }

    /**
     * Verifies that synchronizing an unknown player fails fast, before any execution is recorded.
     */
    @Test
    void shouldRejectUnknownPlayer() {
        when(playerRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> service.synchronizePlayer(3L)
        ).isInstanceOf(PlayerNotFoundException.class);

        verifyNoInteractions(synchronizationRepository, playerSynchronizationService);
    }

    /**
     * Verifies that each persisted player result records why its match-history walk stopped.
     *
     * <p>It tells a season end from a truncation; a failed player completed no walk, so it has no reason.
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
                    SynchronizationStopReason.PAGE_LIMIT_REACHED
                )
            );

        service.synchronizeAllPlayers(SynchronizationTrigger.MANUAL);

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
     * <p>Without this step a scheduled run would import matches the challenges never count.
     */
    @Test
    void shouldRecalculateChallengeProgressAfterImportingMatches() {
        Player firstPlayer = player(1L);

        when(playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED))
            .thenReturn(List.of(firstPlayer));

        when(playerSynchronizationService.synchronize(1L))
            .thenReturn(result(firstPlayer, 3));

        service.synchronizeAllPlayers(SynchronizationTrigger.MANUAL);

        verify(challengeRecalculationService).drawAndRecalculateCurrentWeek();
    }

    /**
     * Verifies that an execution is only marked finished once the campaign has been replayed.
     *
     * <p>A finished execution reads as up-to-date screens, so finishing before the replay would lie.
     */
    @Test
    void shouldCompleteTheExecutionAfterTheCampaignReplay() {
        Player firstPlayer = player(1L);

        when(playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED))
            .thenReturn(List.of(firstPlayer));

        when(playerSynchronizationService.synchronize(1L))
            .thenReturn(result(firstPlayer, 3));

        service.synchronizeAllPlayers(SynchronizationTrigger.MANUAL);

        InOrder ordered = inOrder(challengeRecalculationService, campaignReplayService, synchronizationRepository);
        ordered.verify(challengeRecalculationService).drawAndRecalculateCurrentWeek();
        ordered.verify(campaignReplayService).replayRunningCampaign();
        ordered.verify(synchronizationRepository).save(any(Synchronization.class));
    }

    /**
     * Verifies that a single-player execution is also marked finished after the campaign replay.
     */
    @Test
    void shouldCompleteTheSinglePlayerExecutionAfterTheCampaignReplay() {
        Player firstPlayer = player(1L);

        when(playerRepository.findById(1L)).thenReturn(Optional.of(firstPlayer));
        when(playerSynchronizationService.synchronize(1L))
            .thenReturn(result(firstPlayer, 3));

        service.synchronizePlayer(1L);

        InOrder ordered = inOrder(campaignReplayService, synchronizationRepository);
        ordered.verify(campaignReplayService).replayRunningCampaign();
        ordered.verify(synchronizationRepository).save(any(Synchronization.class));
    }

    /**
     * Verifies that a run importing nothing leaves challenge progress untouched.
     *
     * <p>Without a new match, recalculating would rewrite identical values for every player and challenge.
     */
    @Test
    void shouldNotRecalculateChallengeProgressWhenNothingWasImported() {
        Player firstPlayer = player(1L);

        when(playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED))
            .thenReturn(List.of(firstPlayer));

        when(playerSynchronizationService.synchronize(1L))
            .thenReturn(result(firstPlayer, 0));

        service.synchronizeAllPlayers(SynchronizationTrigger.MANUAL);

        verifyNoInteractions(challengeRecalculationService);
    }

    /**
     * Verifies that a failed recalculation does not fail an otherwise successful import.
     *
     * <p>The matches are already committed; failing would report the import as failed and drop its summary.
     */
    @Test
    void shouldReportSuccessWhenChallengeRecalculationFails() {
        Player firstPlayer = player(1L);

        when(playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED))
            .thenReturn(List.of(firstPlayer));

        when(playerSynchronizationService.synchronize(1L))
            .thenReturn(result(firstPlayer, 3));

        doThrow(new IllegalStateException("recalculation failed"))
            .when(challengeRecalculationService)
            .drawAndRecalculateCurrentWeek();

        service.synchronizeAllPlayers(SynchronizationTrigger.MANUAL);
        Synchronization execution = lastSavedSynchronization();

        assertThat(execution.getStatus())
            .isEqualTo(SynchronizationStatus.COMPLETED);
        assertThat(execution.getMatchesImported()).isEqualTo(3);
        assertThat(execution.getErrorMessage()).isNull();
    }

    /**
     * Verifies that a single-player import also rebuilds the challenge progress.
     */
    @Test
    void shouldRecalculateChallengeProgressAfterSinglePlayerImport() {
        Player firstPlayer = player(1L);

        when(playerRepository.findById(1L))
            .thenReturn(Optional.of(firstPlayer));
        when(playerSynchronizationService.synchronize(1L))
            .thenReturn(result(firstPlayer, 7));

        service.synchronizePlayer(1L);

        verify(challengeRecalculationService).drawAndRecalculateCurrentWeek();
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
     * @return synchronization result
     */
    private PlayerSynchronizationResult result(
        Player player,
        int matchesImported
    ) {
        return new PlayerSynchronizationResult(
            player,
            1,
            matchesImported,
            SynchronizationStopReason.SEASON_BOUNDARY
        );
    }

    /**
     * Returns the execution as it was last saved.
     *
     * @return last saved execution
     */
    private Synchronization lastSavedSynchronization() {
        ArgumentCaptor<Synchronization> saved = ArgumentCaptor.forClass(Synchronization.class);
        verify(synchronizationRepository, atLeastOnce()).save(saved.capture());
        return saved.getValue();
    }
}
