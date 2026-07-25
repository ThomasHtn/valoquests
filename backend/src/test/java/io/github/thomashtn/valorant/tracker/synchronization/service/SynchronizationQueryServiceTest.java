package io.github.thomashtn.valorant.tracker.synchronization.service;

import io.github.thomashtn.valorant.tracker.player.entity.Player;
import io.github.thomashtn.valorant.tracker.player.repository.PlayerRepository;
import io.github.thomashtn.valorant.tracker.shared.exception.ResourceNotFoundException;
import io.github.thomashtn.valorant.tracker.synchronization.dto.SynchronizationDetailsResponse;
import io.github.thomashtn.valorant.tracker.synchronization.dto.SynchronizationResponse;
import io.github.thomashtn.valorant.tracker.synchronization.entity.Synchronization;
import io.github.thomashtn.valorant.tracker.synchronization.entity.SynchronizationPlayerResult;
import io.github.thomashtn.valorant.tracker.synchronization.model.SynchronizationStatus;
import io.github.thomashtn.valorant.tracker.synchronization.model.SynchronizationTrigger;
import io.github.thomashtn.valorant.tracker.synchronization.model.SynchronizationType;
import io.github.thomashtn.valorant.tracker.synchronization.repository.SynchronizationPlayerResultRepository;
import io.github.thomashtn.valorant.tracker.synchronization.repository.SynchronizationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Unit tests for {@link SynchronizationQueryService}. */
@ExtendWith(MockitoExtension.class)
class SynchronizationQueryServiceTest {

    private static final Instant STARTED_AT =
        Instant.parse("2026-07-20T08:00:00Z");

    private static final Instant FINISHED_AT =
        Instant.parse("2026-07-20T08:00:05Z");

    private static final Instant LAST_SUCCESSFUL_AT =
        Instant.parse("2026-07-20T08:00:04Z");

    @Mock
    private SynchronizationRepository synchronizationRepository;

    @Mock
    private SynchronizationPlayerResultRepository playerResultRepository;

    @Mock
    private PlayerRepository playerRepository;

    private SynchronizationQueryService service;

    /** Creates the service under test before each test. */
    @BeforeEach
    void setUp() {
        service = new SynchronizationQueryService(
            synchronizationRepository,
            playerResultRepository,
            playerRepository
        );
    }

    /** Returns the most recent synchronization and latest player success time. */
    @Test
    void shouldReturnLatestSynchronization() {
        Synchronization synchronization = synchronization(12L);
        Player player = player(1L, "Psilonnix");
        player.setLastSuccessfulSynchronizationAt(LAST_SUCCESSFUL_AT);

        when(synchronizationRepository.findFirstByOrderByStartedAtDescIdDesc())
            .thenReturn(Optional.of(synchronization));
        when(playerRepository.findAll()).thenReturn(List.of(player));

        SynchronizationResponse response = service.findLatest();

        assertThat(response.id()).isEqualTo(12L);
        assertThat(response.type()).isEqualTo(SynchronizationType.STANDARD);
        assertThat(response.lastSuccessfulSynchronizationAt())
            .isEqualTo(LAST_SUCCESSFUL_AT);
    }

    /** Rejects history pagination values outside the public contract. */
    @Test
    void shouldRejectInvalidPagination() {
        assertThatThrownBy(() -> service.findHistory(-1, 20))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("page");

        assertThatThrownBy(() -> service.findHistory(0, 101))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("size");
    }

    /** Maps persisted synchronization history to the shared page response. */
    @Test
    void shouldReturnSynchronizationHistory() {
        Synchronization synchronization = synchronization(13L);
        when(synchronizationRepository.findAll(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(synchronization)));
        when(playerRepository.findAll()).thenReturn(List.of());

        var response = service.findHistory(0, 20);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().id()).isEqualTo(13L);
        assertThat(response.totalElements()).isEqualTo(1);
    }

    /** Returns one synchronization with its persisted player-level results. */
    @Test
    void shouldReturnSynchronizationDetails() {
        Synchronization synchronization = synchronization(14L);
        Player player = player(2L, "kikoucraft");
        SynchronizationPlayerResult playerResult =
            new SynchronizationPlayerResult();
        playerResult.setPlayer(player);
        playerResult.setStatus(SynchronizationStatus.COMPLETED);
        playerResult.setPagesFetched(3);
        playerResult.setMatchesImported(17);

        when(synchronizationRepository.findById(14L))
            .thenReturn(Optional.of(synchronization));
        when(playerResultRepository
            .findAllBySynchronizationIdOrderByPlayerIdAsc(14L))
            .thenReturn(List.of(playerResult));

        SynchronizationDetailsResponse response = service.findById(14L);

        assertThat(response.players()).hasSize(1);
        assertThat(response.players().getFirst().displayName())
            .isEqualTo("kikoucraft");
        assertThat(response.players().getFirst().pagesFetched()).isEqualTo(3);
    }

    /** Returns a domain-level 404 exception for an unknown execution. */
    @Test
    void shouldRejectUnknownSynchronization() {
        when(synchronizationRepository.findById(99L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Synchronization not found: 99");
    }

    /** Creates a complete synchronization entity for query tests. */
    private Synchronization synchronization(long id) {
        Synchronization synchronization = new Synchronization();
        synchronization.setId(id);
        synchronization.setType(SynchronizationType.STANDARD);
        synchronization.setTrigger(SynchronizationTrigger.MANUAL);
        synchronization.setStatus(SynchronizationStatus.COMPLETED);
        synchronization.setStartedAt(STARTED_AT);
        synchronization.setFinishedAt(FINISHED_AT);
        synchronization.setPlayersProcessed(1);
        synchronization.setFailureCount(0);
        synchronization.setMatchesImported(10);
        return synchronization;
    }

    /** Creates a player used by query tests. */
    private Player player(long id, String displayName) {
        Player player = new Player();
        player.setId(id);
        player.setDisplayName(displayName);
        return player;
    }
}
