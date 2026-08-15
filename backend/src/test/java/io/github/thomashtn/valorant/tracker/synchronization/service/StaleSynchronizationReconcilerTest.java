package io.github.thomashtn.valorant.tracker.synchronization.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valorant.tracker.synchronization.entity.Synchronization;
import io.github.thomashtn.valorant.tracker.synchronization.model.SynchronizationStatus;
import io.github.thomashtn.valorant.tracker.synchronization.repository.SynchronizationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link StaleSynchronizationReconciler}.
 */
@ExtendWith(MockitoExtension.class)
class StaleSynchronizationReconcilerTest {

    /**
     * Fixed startup instant.
     */
    private static final Instant NOW = Instant.parse("2026-08-15T09:00:00Z");

    /**
     * Mocked synchronization repository.
     */
    @Mock
    private SynchronizationRepository synchronizationRepository;

    /**
     * Reconciler under test.
     */
    private StaleSynchronizationReconciler reconciler;

    /**
     * Creates the reconciler under test before each test.
     */
    @BeforeEach
    void setUp() {
        reconciler = new StaleSynchronizationReconciler(
            synchronizationRepository,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    /**
     * Verifies that an execution left running by a shutdown is closed as failed.
     *
     * <p>Without this, the concurrency guard would find a running execution forever and refuse
     * every later synchronization request.
     */
    @Test
    void shouldFailExecutionsLeftInProgressByAShutdown() {
        Synchronization interrupted = new Synchronization();
        interrupted.setStatus(SynchronizationStatus.RUNNING);

        when(synchronizationRepository.findAllByStatusIn(anyCollection()))
            .thenReturn(List.of(interrupted));

        reconciler.run(null);

        assertThat(interrupted.getStatus()).isEqualTo(SynchronizationStatus.FAILED);
        assertThat(interrupted.getFinishedAt()).isEqualTo(NOW);
        assertThat(interrupted.getErrorMessage()).contains("restart");

        verify(synchronizationRepository).saveAll(List.of(interrupted));
    }

    /**
     * Verifies that a clean startup writes nothing.
     */
    @Test
    void shouldWriteNothingWhenNoExecutionWasInterrupted() {
        when(synchronizationRepository.findAllByStatusIn(anyCollection())).thenReturn(List.of());

        reconciler.run(null);

        verify(synchronizationRepository, never()).saveAll(anyCollection());
    }
}
