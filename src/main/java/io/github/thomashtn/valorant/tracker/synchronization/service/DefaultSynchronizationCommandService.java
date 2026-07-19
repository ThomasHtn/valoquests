package io.github.thomashtn.valorant.tracker.synchronization.service;

import io.github.thomashtn.valorant.tracker.player.entity.Player;
import io.github.thomashtn.valorant.tracker.player.repository.PlayerRepository;
import io.github.thomashtn.valorant.tracker.shared.dto.PageResponse;
import io.github.thomashtn.valorant.tracker.shared.exception.FeatureNotImplementedException;
import io.github.thomashtn.valorant.tracker.synchronization.dto.SynchronizationDetailsResponse;
import io.github.thomashtn.valorant.tracker.synchronization.dto.SynchronizationResponse;
import io.github.thomashtn.valorant.tracker.synchronization.entity.Synchronization;
import io.github.thomashtn.valorant.tracker.synchronization.model.PlayerDeepSynchronizationResult;
import io.github.thomashtn.valorant.tracker.synchronization.model.PlayerSynchronizationResult;
import io.github.thomashtn.valorant.tracker.synchronization.model.SynchronizationStatus;
import io.github.thomashtn.valorant.tracker.synchronization.model.SynchronizationTrigger;
import io.github.thomashtn.valorant.tracker.synchronization.model.SynchronizationType;
import io.github.thomashtn.valorant.tracker.synchronization.repository.SynchronizationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Default implementation of administrative synchronization operations.
 *
 * <p>Standard synchronization is currently available for one player or all
 * active players. Deep synchronization and synchronization consultation
 * endpoints remain outside the current implementation lot.</p>
 */
@Service
public class DefaultSynchronizationCommandService
    implements SynchronizationCommandService {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(DefaultSynchronizationCommandService.class);

    private static final int MAXIMUM_ERROR_MESSAGE_LENGTH = 2_000;

    private final PlayerSynchronizationService playerSynchronizationService;
    private final PlayerDeepSynchronizationService
        playerDeepSynchronizationService;
    private final PlayerRepository playerRepository;
    private final SynchronizationRepository synchronizationRepository;
    private final Clock clock;

    /**
     * Creates the synchronization command service.
     *
     * @param playerSynchronizationService individual player synchronization
     *                                     service
     * @param playerRepository             tracked-player repository
     * @param synchronizationRepository    synchronization repository
     * @param clock                        application clock
     */
    public DefaultSynchronizationCommandService(
        PlayerSynchronizationService playerSynchronizationService,
        PlayerDeepSynchronizationService playerDeepSynchronizationService,
        PlayerRepository playerRepository,
        SynchronizationRepository synchronizationRepository,
        Clock clock
    ) {
        this.playerSynchronizationService = playerSynchronizationService;
        this.playerDeepSynchronizationService = playerDeepSynchronizationService;
        this.playerRepository = playerRepository;
        this.synchronizationRepository = synchronizationRepository;
        this.clock = clock;
    }

    /**
     * Executes a standard synchronization for every active tracked player.
     *
     * <p>Each player is processed independently. A player failure is recorded
     * and logged but does not interrupt the synchronization of the remaining
     * players.</p>
     *
     * @return global synchronization summary
     */
    @Override
    public SynchronizationResponse synchronizeAllPlayers() {
        Synchronization synchronization = startSynchronization(
            SynchronizationType.STANDARD
        );

        List<Player> players =
            playerRepository.findAllByOrderByIdAsc();

        int successfulPlayers = 0;
        int failureCount = 0;
        int matchesImported = 0;
        Instant lastSuccessfulSynchronizationAt = null;
        StringBuilder errorMessages = new StringBuilder();

        for (Player player : players) {
            try {
                PlayerSynchronizationResult result =
                    playerSynchronizationService.synchronize(player.getId());

                successfulPlayers++;
                matchesImported += result.matchesImported();

                lastSuccessfulSynchronizationAt = latestInstant(
                    lastSuccessfulSynchronizationAt,
                    result.completedAt()
                );
            } catch (RuntimeException exception) {
                failureCount++;

                LOGGER.error(
                    "Player synchronization failed for player {}",
                    player.getId(),
                    exception
                );

                appendPlayerError(
                    errorMessages,
                    player,
                    exception
                );
            }
        }

        Instant finishedAt = clock.instant();

        synchronization.setStatus(
            determineGlobalStatus(
                players.size(),
                successfulPlayers,
                failureCount
            )
        );
        synchronization.setFinishedAt(finishedAt);
        synchronization.setPlayersProcessed(players.size());
        synchronization.setFailureCount(failureCount);
        synchronization.setMatchesImported(matchesImported);
        synchronization.setErrorMessage(
            errorMessages.isEmpty()
                ? null
                : truncateErrorMessage(errorMessages.toString())
        );

        synchronizationRepository.save(synchronization);

        return toResponse(
            synchronization,
            lastSuccessfulSynchronizationAt
        );
    }

    /**
     * Executes and records a standard synchronization for one player.
     *
     * <p>The synchronization record is created before contacting Henrik. This
     * ensures that a failed attempt remains visible in the database.</p>
     *
     * @param playerId tracked player identifier
     * @return completed synchronization summary
     */
    @Override
    public SynchronizationResponse synchronizePlayer(long playerId) {
        Synchronization synchronization = startSynchronization(
            SynchronizationType.STANDARD
        );

        try {
            PlayerSynchronizationResult result =
                playerSynchronizationService.synchronize(playerId);

            completeSinglePlayerSynchronization(
                synchronization,
                result
            );

            return toResponse(
                synchronization,
                result.completedAt()
            );
        } catch (RuntimeException exception) {
            failSinglePlayerSynchronization(
                synchronization,
                exception
            );

            throw exception;
        }
    }

    /**
     * Creates and persists a running synchronization attempt.
     *
     * @return persisted synchronization entity
     */
    private Synchronization startSynchronization(
        SynchronizationType type
    ) {
        Synchronization synchronization = new Synchronization();

        synchronization.setType(type);

        synchronization.setType(SynchronizationType.STANDARD);
        synchronization.setTrigger(SynchronizationTrigger.MANUAL);
        synchronization.setStatus(SynchronizationStatus.RUNNING);
        synchronization.setStartedAt(clock.instant());
        synchronization.setPlayersProcessed(0);
        synchronization.setFailureCount(0);
        synchronization.setMatchesImported(0);
        synchronization.setErrorMessage(null);

        return synchronizationRepository.save(synchronization);
    }

    /**
     * Marks a single-player execution as successfully completed.
     *
     * @param synchronization execution to update
     * @param result          successful player synchronization result
     */
    private void completeSinglePlayerSynchronization(
        Synchronization synchronization,
        PlayerSynchronizationResult result
    ) {
        synchronization.setStatus(SynchronizationStatus.COMPLETED);
        synchronization.setFinishedAt(result.completedAt());
        synchronization.setPlayersProcessed(1);
        synchronization.setFailureCount(0);
        synchronization.setMatchesImported(result.matchesImported());
        synchronization.setErrorMessage(null);

        synchronizationRepository.save(synchronization);
    }

    /**
     * Marks a single-player execution as failed.
     *
     * @param synchronization execution to update
     * @param exception       synchronization failure
     */
    private void failSinglePlayerSynchronization(
        Synchronization synchronization,
        RuntimeException exception
    ) {
        synchronization.setStatus(SynchronizationStatus.FAILED);
        synchronization.setFinishedAt(clock.instant());
        synchronization.setPlayersProcessed(1);
        synchronization.setFailureCount(1);
        synchronization.setMatchesImported(0);
        synchronization.setErrorMessage(
            truncateErrorMessage(exception.getMessage())
        );

        synchronizationRepository.save(synchronization);
    }

    /**
     * Determines the final status of a global synchronization.
     *
     * @param playerCount       number of active players selected
     * @param successfulPlayers number of successful player executions
     * @param failureCount      number of failed player executions
     * @return resulting synchronization status
     */
    private SynchronizationStatus determineGlobalStatus(
        int playerCount,
        int successfulPlayers,
        int failureCount
    ) {
        if (playerCount == 0 || failureCount == 0) {
            return SynchronizationStatus.COMPLETED;
        }

        if (successfulPlayers == 0) {
            return SynchronizationStatus.FAILED;
        }

        return SynchronizationStatus.PARTIAL;
    }

    /**
     * Appends a concise player-specific error to the global error message.
     *
     * @param errorMessages accumulated global error messages
     * @param player        failed player
     * @param exception     synchronization exception
     */
    private void appendPlayerError(
        StringBuilder errorMessages,
        Player player,
        RuntimeException exception
    ) {
        if (!errorMessages.isEmpty()) {
            errorMessages.append(System.lineSeparator());
        }

        errorMessages
            .append("Player ")
            .append(player.getId())
            .append(": ")
            .append(safeErrorMessage(exception));
    }

    /**
     * Returns a safe description for an exception.
     *
     * @param exception synchronization exception
     * @return non-empty error description
     */
    private String safeErrorMessage(RuntimeException exception) {
        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }

        return message;
    }

    /**
     * Returns the most recent of two timestamps.
     *
     * @param current   currently retained timestamp
     * @param candidate candidate timestamp
     * @return most recent non-null timestamp
     */
    private Instant latestInstant(
        Instant current,
        Instant candidate
    ) {
        if (current == null) {
            return candidate;
        }

        if (candidate == null) {
            return current;
        }

        return candidate.isAfter(current)
            ? candidate
            : current;
    }

    /**
     * Maps the persisted entity to its public API representation.
     *
     * @param synchronization                 persisted execution
     * @param lastSuccessfulSynchronizationAt latest successful player
     *                                        synchronization timestamp
     * @return API response
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
     * Restricts an error message to the size accepted by PostgreSQL.
     *
     * @param message original exception message
     * @return safe error message
     */
    private String truncateErrorMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Synchronization failed";
        }

        if (message.length() <= MAXIMUM_ERROR_MESSAGE_LENGTH) {
            return message;
        }

        return message.substring(0, MAXIMUM_ERROR_MESSAGE_LENGTH);
    }

    /**
     * Executes a deep synchronization for every tracked player.
     *
     * <p>Each player is processed independently. A failure is recorded but does
     * not interrupt the remaining players.</p>
     *
     * @return completed global deep-synchronization summary
     */
    @Override
    public SynchronizationResponse
    requestDeepSynchronizationForAllPlayers() {
        Synchronization synchronization = startSynchronization(
            SynchronizationType.DEEP
        );

        List<Player> players =
            playerRepository.findAllByOrderByIdAsc();

        int successfulPlayers = 0;
        int failureCount = 0;
        int matchesImported = 0;
        Instant lastSuccessfulSynchronizationAt = null;
        StringBuilder errorMessages = new StringBuilder();

        for (Player player : players) {
            try {
                PlayerDeepSynchronizationResult result =
                    playerDeepSynchronizationService.synchronize(
                        player.getId()
                    );

                successfulPlayers++;
                matchesImported += result.matchesImported();

                lastSuccessfulSynchronizationAt = latestInstant(
                    lastSuccessfulSynchronizationAt,
                    result.completedAt()
                );
            } catch (RuntimeException exception) {
                failureCount++;

                LOGGER.error(
                    "Deep synchronization failed for player {}",
                    player.getId(),
                    exception
                );

                appendPlayerError(
                    errorMessages,
                    player,
                    exception
                );
            }
        }

        synchronization.setStatus(
            determineGlobalStatus(
                players.size(),
                successfulPlayers,
                failureCount
            )
        );
        synchronization.setFinishedAt(clock.instant());
        synchronization.setPlayersProcessed(players.size());
        synchronization.setFailureCount(failureCount);
        synchronization.setMatchesImported(matchesImported);
        synchronization.setErrorMessage(
            errorMessages.isEmpty()
                ? null
                : truncateErrorMessage(errorMessages.toString())
        );

        synchronizationRepository.save(synchronization);

        return toResponse(
            synchronization,
            lastSuccessfulSynchronizationAt
        );
    }

    /**
     * Executes and records a deep synchronization for one player.
     *
     * @param playerId tracked player identifier
     * @return completed deep-synchronization summary
     */
    @Override
    public SynchronizationResponse requestDeepSynchronizationForPlayer(
        long playerId
    ) {
        Synchronization synchronization = startSynchronization(
            SynchronizationType.DEEP
        );

        try {
            PlayerDeepSynchronizationResult result =
                playerDeepSynchronizationService.synchronize(playerId);

            synchronization.setStatus(
                SynchronizationStatus.COMPLETED
            );
            synchronization.setFinishedAt(result.completedAt());
            synchronization.setPlayersProcessed(1);
            synchronization.setFailureCount(0);
            synchronization.setMatchesImported(
                result.matchesImported()
            );
            synchronization.setErrorMessage(null);

            synchronizationRepository.save(synchronization);

            return toResponse(
                synchronization,
                result.completedAt()
            );
        } catch (RuntimeException exception) {
            failSinglePlayerSynchronization(
                synchronization,
                exception
            );

            throw exception;
        }
    }

    /**
     * Synchronization monitoring will be implemented in the next lot.
     *
     * @return never returns normally
     */
    @Override
    public SynchronizationResponse findLatest() {
        throw notImplemented("Latest synchronization consultation");
    }

    /**
     * Synchronization monitoring will be implemented in the next lot.
     *
     * @param page zero-based page index
     * @param size requested page size
     * @return never returns normally
     */
    @Override
    public PageResponse<SynchronizationResponse> findHistory(
        int page,
        int size
    ) {
        throw notImplemented("Synchronization history consultation");
    }

    /**
     * Synchronization monitoring will be implemented in the next lot.
     *
     * @param synchronizationId synchronization identifier
     * @return never returns normally
     */
    @Override
    public SynchronizationDetailsResponse findById(
        long synchronizationId
    ) {
        throw notImplemented("Synchronization details consultation");
    }

    /**
     * Creates a consistent exception for an unfinished feature.
     *
     * @param feature feature name
     * @return feature-not-implemented exception
     */
    private FeatureNotImplementedException notImplemented(String feature) {
        return new FeatureNotImplementedException(
            feature + " is not implemented yet"
        );
    }
}
