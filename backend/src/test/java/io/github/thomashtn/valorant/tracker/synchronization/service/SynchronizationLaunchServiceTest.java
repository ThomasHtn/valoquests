package io.github.thomashtn.valorant.tracker.synchronization.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valorant.tracker.player.repository.PlayerRepository;
import io.github.thomashtn.valorant.tracker.shared.exception.ConflictException;
import io.github.thomashtn.valorant.tracker.shared.exception.ResourceNotFoundException;
import io.github.thomashtn.valorant.tracker.synchronization.model.SynchronizationStatus;
import io.github.thomashtn.valorant.tracker.synchronization.repository.SynchronizationRepository;
import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link SynchronizationLaunchService}.
 */
@ExtendWith(MockitoExtension.class)
class SynchronizationLaunchServiceTest {

    /**
     * Mocked asynchronous runner.
     */
    @Mock
    private AsyncSynchronizationRunner runner;

    /**
     * Mocked synchronization repository.
     */
    @Mock
    private SynchronizationRepository synchronizationRepository;

    /**
     * Mocked player repository.
     */
    @Mock
    private PlayerRepository playerRepository;

    /**
     * Captures the statuses the guard looks for.
     */
    @SuppressWarnings("rawtypes")
    @Captor
    private ArgumentCaptor<Collection> statusCaptor;

    /**
     * Service under test.
     */
    private SynchronizationLaunchService service;

    /**
     * Creates the service under test before each test.
     */
    @BeforeEach
    void setUp() {
        service = new SynchronizationLaunchService(
            runner,
            synchronizationRepository,
            playerRepository
        );
    }

    /**
     * Verifies that an idle application dispatches the batch run.
     */
    @Test
    void shouldDispatchABatchRunWhenNothingIsInProgress() {
        when(synchronizationRepository.existsByStatusIn(anyCollection())).thenReturn(false);

        service.launchAllPlayers();

        verify(runner).runAllPlayers();
    }

    /**
     * Verifies that the guard looks for exactly the two statuses meaning "still running".
     *
     * <p>A COMPLETED or FAILED execution must never block a new request, which is what pinning the
     * exact status list guarantees.
     */
    @Test
    void shouldOnlyConsiderPendingAndRunningExecutionsAsInProgress() {
        when(synchronizationRepository.existsByStatusIn(anyCollection())).thenReturn(false);

        service.launchAllPlayers();

        verify(synchronizationRepository).existsByStatusIn(statusCaptor.capture());

        assertThat(statusCaptor.getValue()).containsExactlyInAnyOrder(
            SynchronizationStatus.PENDING,
            SynchronizationStatus.RUNNING
        );
    }

    /**
     * Verifies that a concurrent batch request is refused rather than queued.
     */
    @Test
    void shouldRefuseABatchRunWhileAnotherIsInProgress() {
        when(synchronizationRepository.existsByStatusIn(anyCollection())).thenReturn(true);

        assertThatThrownBy(() -> service.launchAllPlayers())
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("already in progress");

        verifyNoInteractions(runner);
    }

    /**
     * Verifies that a single-player run is dispatched for a known player.
     */
    @Test
    void shouldDispatchASinglePlayerRun() {
        when(playerRepository.existsById(3L)).thenReturn(true);
        when(synchronizationRepository.existsByStatusIn(anyCollection())).thenReturn(false);

        service.launchPlayer(3L);

        verify(runner).runPlayer(3L);
    }

    /**
     * Verifies that an unknown player is reported now rather than as a failed execution later.
     */
    @Test
    void shouldRejectAnUnknownPlayerBeforeAccepting() {
        when(playerRepository.existsById(404L)).thenReturn(false);

        assertThatThrownBy(() -> service.launchPlayer(404L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("404");

        verifyNoInteractions(runner, synchronizationRepository);
    }

    /**
     * Verifies that a concurrent single-player request is refused too.
     */
    @Test
    void shouldRefuseASinglePlayerRunWhileAnotherIsInProgress() {
        when(playerRepository.existsById(3L)).thenReturn(true);
        when(synchronizationRepository.existsByStatusIn(anyCollection())).thenReturn(true);

        assertThatThrownBy(() -> service.launchPlayer(3L))
            .isInstanceOf(ConflictException.class);

        verifyNoInteractions(runner);
    }

    /**
     * Verifies that the in-progress query is exposed for other maintenance operations.
     */
    @Test
    void shouldReportWhetherASynchronizationIsInProgress() {
        when(synchronizationRepository.existsByStatusIn(anyCollection())).thenReturn(true);

        assertThat(service.isSynchronizationInProgress()).isTrue();
    }
}
