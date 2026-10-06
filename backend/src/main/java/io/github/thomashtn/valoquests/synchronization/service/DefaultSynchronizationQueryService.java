package io.github.thomashtn.valoquests.synchronization.service;

import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.shared.dto.PageResponse;
import io.github.thomashtn.valoquests.shared.exception.ResourceNotFoundException;
import io.github.thomashtn.valoquests.shared.util.PaginationGuard;
import io.github.thomashtn.valoquests.synchronization.dto.SynchronizationDetailsResponse;
import io.github.thomashtn.valoquests.synchronization.dto.SynchronizationResponse;
import io.github.thomashtn.valoquests.synchronization.dto.SynchronizationStatusResponse;
import io.github.thomashtn.valoquests.synchronization.entity.Synchronization;
import io.github.thomashtn.valoquests.synchronization.model.SynchronizationStatus;
import io.github.thomashtn.valoquests.synchronization.repository.SynchronizationPlayerResultRepository;
import io.github.thomashtn.valoquests.synchronization.repository.SynchronizationRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads synchronization executions from the database.
 */
@Service
@Transactional(readOnly = true)
public class DefaultSynchronizationQueryService implements SynchronizationQueryService {

    /**
     * Statuses of an execution that completed or partially succeeded.
     */
    private static final List<SynchronizationStatus> SUCCEEDED_STATUSES =
        List.of(SynchronizationStatus.COMPLETED, SynchronizationStatus.PARTIAL);

    /**
     * Repository used to persist synchronization executions.
     */
    private final SynchronizationRepository synchronizationRepository;

    /**
     * Repository used to persist and query per-player synchronization results.
     */
    private final SynchronizationPlayerResultRepository playerResultRepository;

    /**
     * Repository used to load and persist tracked players.
     */
    private final PlayerRepository playerRepository;

    /**
     * Creates the synchronization query service.
     *
     * @param synchronizationRepository repository holding synchronization executions
     * @param playerResultRepository    repository holding per-player results
     * @param playerRepository          repository holding tracked players
     */
    public DefaultSynchronizationQueryService(
        SynchronizationRepository synchronizationRepository,
        SynchronizationPlayerResultRepository playerResultRepository,
        PlayerRepository playerRepository
    ) {
        this.synchronizationRepository = synchronizationRepository;
        this.playerResultRepository = playerResultRepository;
        this.playerRepository = playerRepository;
    }

    @Override
    public SynchronizationResponse findLatest() {
        Synchronization synchronization = synchronizationRepository
            .findFirstByOrderByStartedAtDescIdDesc()
            .orElseThrow(() -> new ResourceNotFoundException(
                "No synchronization execution has been recorded"
            ));

        return SynchronizationResponse.from(
            synchronization,
            findLatestSuccessfulPlayerSynchronizationAt()
        );
    }

    /**
     * Returns whether a synchronization is in progress and when the last one finished.
     *
     * <p>Read from the executions rather than from the players: a player's own timestamp moves
     * mid-batch, well before the challenges and the campaign are rebuilt.
     *
     * @return the public synchronization status
     */
    @Override
    public SynchronizationStatusResponse findStatus() {
        Instant lastCompletedAt = synchronizationRepository
            .findFirstByStatusInAndFinishedAtNotNullOrderByFinishedAtDescIdDesc(SUCCEEDED_STATUSES)
            .map(Synchronization::getFinishedAt)
            .orElse(null);

        return new SynchronizationStatusResponse(
            synchronizationRepository.existsByStatusIn(SynchronizationStatus.IN_PROGRESS),
            lastCompletedAt
        );
    }

    @Override
    public PageResponse<SynchronizationResponse> findHistory(
        int page,
        int size
    ) {
        PaginationGuard.assertValidPageRequest(page, size);

        Page<Synchronization> result = synchronizationRepository.findAll(
            PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "startedAt", "id")
            )
        );
        Instant latestSuccessfulAt =
            findLatestSuccessfulPlayerSynchronizationAt();

        List<SynchronizationResponse> content = result.getContent().stream()
            .map(item -> SynchronizationResponse.from(item, latestSuccessfulAt))
            .toList();

        return PageResponse.from(result, content);
    }

    @Override
    public SynchronizationDetailsResponse findById(long synchronizationId) {
        Synchronization synchronization = synchronizationRepository
            .findById(synchronizationId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Synchronization not found: " + synchronizationId
            ));

        List<SynchronizationDetailsResponse.PlayerResultResponse> players =
            playerResultRepository
                .findAllBySynchronizationIdOrderByPlayerIdAsc(
                    synchronizationId
                )
                .stream()
                .map(result ->
                    new SynchronizationDetailsResponse.PlayerResultResponse(
                        result.getPlayer().getId(),
                        result.getPlayer().getDisplayName(),
                        result.getStatus(),
                        result.getPagesFetched(),
                        result.getMatchesImported(),
                        result.getErrorMessage(),
                        result.getStopReason()
                    )
                )
                .toList();

        return new SynchronizationDetailsResponse(
            synchronization.getId(),
            synchronization.getType(),
            synchronization.getTrigger(),
            synchronization.getStatus(),
            synchronization.getStartedAt(),
            synchronization.getFinishedAt(),
            synchronization.getPlayersProcessed(),
            synchronization.getFailureCount(),
            synchronization.getMatchesImported(),
            synchronization.getErrorMessage(),
            players
        );
    }

    /**
     * Returns the latest successful synchronization timestamp among players.
     */
    private Instant findLatestSuccessfulPlayerSynchronizationAt() {
        return playerRepository
            .findLatestSuccessfulSynchronizationAt()
            .orElse(null);
    }
}
