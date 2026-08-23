package io.github.thomashtn.valoquests.synchronization.service;

import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.shared.dto.PageResponse;
import io.github.thomashtn.valoquests.shared.exception.InvalidRequestException;
import io.github.thomashtn.valoquests.shared.exception.ResourceNotFoundException;
import io.github.thomashtn.valoquests.synchronization.dto.SynchronizationDetailsResponse;
import io.github.thomashtn.valoquests.synchronization.dto.SynchronizationResponse;
import io.github.thomashtn.valoquests.synchronization.entity.Synchronization;
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
 * Provides read-only access to persisted synchronization executions.
 */
@Service
@Transactional(readOnly = true)
public class SynchronizationQueryService {

    /**
     * Maximum page size accepted by the administrative history endpoint.
     */
    private static final int MAXIMUM_PAGE_SIZE = 100;

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
    public SynchronizationQueryService(
        SynchronizationRepository synchronizationRepository,
        SynchronizationPlayerResultRepository playerResultRepository,
        PlayerRepository playerRepository
    ) {
        this.synchronizationRepository = synchronizationRepository;
        this.playerResultRepository = playerResultRepository;
        this.playerRepository = playerRepository;
    }

    /**
     * Returns the most recently started synchronization execution.
     *
     * @return the latest execution summary
     * @throws io.github.thomashtn.valoquests.shared.exception.ResourceNotFoundException when
     *     no execution has ever been recorded
     */
    public SynchronizationResponse findLatest() {
        Synchronization synchronization = synchronizationRepository
            .findFirstByOrderByStartedAtDescIdDesc()
            .orElseThrow(() -> new ResourceNotFoundException(
                "No synchronization execution has been recorded"
            ));

        return toResponse(
            synchronization,
            findLatestSuccessfulPlayerSynchronizationAt()
        );
    }

    /**
     * Returns synchronization history ordered from newest to oldest.
     *
     * @param page zero-based page index
     * @param size number of executions returned per page
     * @return the requested page of execution summaries
     */
    public PageResponse<SynchronizationResponse> findHistory(
        int page,
        int size
    ) {
        validatePagination(page, size);

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
            .map(item -> toResponse(item, latestSuccessfulAt))
            .toList();

        return new PageResponse<>(
            content,
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    /**
     * Returns one execution and every persisted per-player result.
     *
     * @param synchronizationId internal synchronization identifier
     * @return the execution with one detailed result per processed player
     * @throws io.github.thomashtn.valoquests.shared.exception.ResourceNotFoundException when
     *     no execution carries that identifier
     */
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
     * Maps one persisted execution to its summary API representation.
     */
    private SynchronizationResponse toResponse(
        Synchronization synchronization,
        Instant lastSuccessfulSynchronizationAt
    ) {
        return new SynchronizationResponse(
            synchronization.getId(),
            synchronization.getType(),
            synchronization.getTrigger(),
            synchronization.getStatus(),
            synchronization.getStartedAt(),
            synchronization.getFinishedAt(),
            synchronization.getStartedAt(),
            lastSuccessfulSynchronizationAt,
            synchronization.getPlayersProcessed(),
            synchronization.getFailureCount(),
            synchronization.getMatchesImported(),
            synchronization.getErrorMessage()
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

    /**
     * Validates public pagination parameters before creating a page request.
     */
    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new InvalidRequestException(
                "page must be greater than or equal to 0"
            );
        }
        if (size < 1 || size > MAXIMUM_PAGE_SIZE) {
            throw new InvalidRequestException(
                "size must be between 1 and " + MAXIMUM_PAGE_SIZE
            );
        }
    }
}
