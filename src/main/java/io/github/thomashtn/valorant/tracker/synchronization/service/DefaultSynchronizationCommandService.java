package io.github.thomashtn.valorant.tracker.synchronization.service;

import io.github.thomashtn.valorant.tracker.player.entity.Player;
import io.github.thomashtn.valorant.tracker.player.model.PlayerStatus;
import io.github.thomashtn.valorant.tracker.player.repository.PlayerRepository;
import io.github.thomashtn.valorant.tracker.synchronization.dto.SynchronizationResponse;
import io.github.thomashtn.valorant.tracker.synchronization.entity.Synchronization;
import io.github.thomashtn.valorant.tracker.synchronization.entity.SynchronizationPlayerResult;
import io.github.thomashtn.valorant.tracker.synchronization.model.PlayerDeepSynchronizationResult;
import io.github.thomashtn.valorant.tracker.synchronization.model.PlayerSynchronizationResult;
import io.github.thomashtn.valorant.tracker.synchronization.model.SynchronizationStatus;
import io.github.thomashtn.valorant.tracker.synchronization.model.SynchronizationTrigger;
import io.github.thomashtn.valorant.tracker.synchronization.model.SynchronizationType;
import io.github.thomashtn.valorant.tracker.synchronization.repository.SynchronizationPlayerResultRepository;
import io.github.thomashtn.valorant.tracker.synchronization.repository.SynchronizationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Executes and records manual standard and deep synchronizations.
 *
 * <p>The service deliberately keeps external API calls outside a global
 * database transaction. Each execution is persisted before processing starts,
 * and every player outcome is recorded independently so partial failures remain
 * visible without blocking the remaining players.</p>
 */
@Service
public class DefaultSynchronizationCommandService
    implements SynchronizationCommandService {

    /**
     * Logger used for synchronization lifecycle and failure diagnostics.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(DefaultSynchronizationCommandService.class);

    /**
     * Maximum size accepted by synchronization error columns.
     */
    private static final int MAXIMUM_ERROR_MESSAGE_LENGTH = 2_000;

    /**
     * Service used to synchronize recent matches for one player.
     */
    private final PlayerSynchronizationService playerSynchronizationService;

    /**
     * Service used to synchronize historical matches for one player.
     */
    private final PlayerDeepSynchronizationService playerDeepSynchronizationService;

    /**
     * Repository used to retrieve active tracked players.
     */
    private final PlayerRepository playerRepository;

    /**
     * Repository used to persist global synchronization executions.
     */
    private final SynchronizationRepository synchronizationRepository;

    /**
     * Repository used to persist one outcome per processed player.
     */
    private final SynchronizationPlayerResultRepository playerResultRepository;

    /**
     * Clock used to generate deterministic execution timestamps.
     */
    private final Clock clock;

    /**
     * Creates the synchronization command service.
     *
     * @param playerSynchronizationService     standard player synchronization
     *                                         service
     * @param playerDeepSynchronizationService deep player synchronization
     *                                         service
     * @param playerRepository                 tracked-player repository
     * @param synchronizationRepository        global execution repository
     * @param playerResultRepository           per-player result repository
     * @param clock                            application clock
     */
    public DefaultSynchronizationCommandService(
        PlayerSynchronizationService playerSynchronizationService,
        PlayerDeepSynchronizationService playerDeepSynchronizationService,
        PlayerRepository playerRepository,
        SynchronizationRepository synchronizationRepository,
        SynchronizationPlayerResultRepository playerResultRepository,
        Clock clock
    ) {
        this.playerSynchronizationService = playerSynchronizationService;
        this.playerDeepSynchronizationService = playerDeepSynchronizationService;
        this.playerRepository = playerRepository;
        this.synchronizationRepository = synchronizationRepository;
        this.playerResultRepository = playerResultRepository;
        this.clock = clock;
    }

    /**
     * Executes a standard synchronization for every active player.
     *
     * @return persisted synchronization summary
     */
    @Override
    public SynchronizationResponse synchronizeAllPlayers() {
        return executeForAllPlayers(
            SynchronizationType.STANDARD,
            playerId -> toExecutionResult(
                playerSynchronizationService.synchronize(playerId)
            )
        );
    }

    /**
     * Executes a standard synchronization for one player.
     *
     * @param playerId tracked player identifier
     * @return persisted synchronization summary
     */
    @Override
    public SynchronizationResponse synchronizePlayer(long playerId) {
        return executeForPlayer(
            SynchronizationType.STANDARD,
            playerId,
            id -> toExecutionResult(
                playerSynchronizationService.synchronize(id)
            )
        );
    }

    /**
     * Executes a deep synchronization for every active player.
     *
     * @return persisted synchronization summary
     */
    @Override
    public SynchronizationResponse requestDeepSynchronizationForAllPlayers() {
        return executeForAllPlayers(
            SynchronizationType.DEEP,
            playerId -> toExecutionResult(
                playerDeepSynchronizationService.synchronize(playerId)
            )
        );
    }

    /**
     * Executes a deep synchronization for one player.
     *
     * @param playerId tracked player identifier
     * @return persisted synchronization summary
     */
    @Override
    public SynchronizationResponse requestDeepSynchronizationForPlayer(
        long playerId
    ) {
        return executeForPlayer(
            SynchronizationType.DEEP,
            playerId,
            id -> toExecutionResult(
                playerDeepSynchronizationService.synchronize(id)
            )
        );
    }

    /**
     * Executes one synchronization operation for all active players.
     *
     * @param type      synchronization type
     * @param operation player-level operation
     * @return persisted global summary
     */
    private SynchronizationResponse executeForAllPlayers(
        SynchronizationType type,
        PlayerSynchronizationOperation operation
    ) {
        Synchronization synchronization = startSynchronization(type);
        List<Player> players =
            playerRepository.findAllByStatusOrderByIdAsc(PlayerStatus.ACTIVE);
        BatchSummary summary = BatchSummary.empty();

        LOGGER.info(
            "Starting {} synchronization for {} active players",
            type,
            players.size()
        );

        for (Player player : players) {
            summary = synchronizePlayerWithinBatch(
                synchronization,
                type,
                player,
                operation,
                summary
            );
        }

        completeBatchSynchronization(
            synchronization,
            players.size(),
            summary
        );

        LOGGER.info(
            "{} synchronization completed with status {}, {} failures and {} imported matches",
            type,
            synchronization.getStatus(),
            synchronization.getFailureCount(),
            synchronization.getMatchesImported()
        );

        return toResponse(
            synchronization,
            summary.lastSuccessfulSynchronizationAt()
        );
    }

    /**
     * Executes one synchronization operation and records its outcome.
     *
     * @param type      synchronization type
     * @param playerId  player identifier
     * @param operation player-level operation
     * @return persisted synchronization summary
     */
    private SynchronizationResponse executeForPlayer(
        SynchronizationType type,
        long playerId,
        PlayerSynchronizationOperation operation
    ) {
        Synchronization synchronization = startSynchronization(type);

        LOGGER.info(
            "Starting {} synchronization for player {}",
            type,
            playerId
        );

        try {
            PlayerExecutionResult result = operation.synchronize(playerId);

            saveSuccessfulPlayerResult(synchronization, result);
            completeSinglePlayerSynchronization(synchronization, result);

            LOGGER.info(
                "{} synchronization completed for player {} with {} pages and {} imported matches",
                type,
                playerId,
                result.pagesFetched(),
                result.matchesImported()
            );

            return toResponse(
                synchronization,
                result.completedAt()
            );
        } catch (RuntimeException exception) {
            failSinglePlayerSynchronization(synchronization, exception);

            LOGGER.error(
                "{} synchronization failed for player {}",
                type,
                playerId,
                exception
            );

            throw exception;
        }
    }

    /**
     * Executes one player within a batch and returns the updated aggregate.
     *
     * @param synchronization global execution
     * @param type            synchronization type
     * @param player          player to process
     * @param operation       player-level operation
     * @param summary         current batch summary
     * @return updated immutable batch summary
     */
    private BatchSummary synchronizePlayerWithinBatch(
        Synchronization synchronization,
        SynchronizationType type,
        Player player,
        PlayerSynchronizationOperation operation,
        BatchSummary summary
    ) {
        try {
            PlayerExecutionResult result = operation.synchronize(player.getId());
            saveSuccessfulPlayerResult(synchronization, result);
            return summary.withSuccess(result);
        } catch (RuntimeException exception) {
            String errorMessage = safeErrorMessage(exception);

            LOGGER.error(
                "{} synchronization failed for player {}",
                type,
                player.getId(),
                exception
            );

            savePlayerResult(
                synchronization,
                player,
                SynchronizationStatus.FAILED,
                0,
                0,
                errorMessage
            );

            return summary.withFailure(player, errorMessage);
        }
    }

    /**
     * Creates and persists a running synchronization execution.
     *
     * @param type synchronization type
     * @return persisted execution
     */
    private Synchronization startSynchronization(
        SynchronizationType type
    ) {
        Synchronization synchronization = new Synchronization();

        synchronization.setType(type);
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
     * Marks a batch execution as complete and persists its aggregate values.
     *
     * @param synchronization global execution
     * @param playerCount     selected player count
     * @param summary         aggregated outcomes
     */
    private void completeBatchSynchronization(
        Synchronization synchronization,
        int playerCount,
        BatchSummary summary
    ) {
        synchronization.setStatus(
            determineGlobalStatus(
                playerCount,
                summary.successfulPlayers(),
                summary.failureCount()
            )
        );
        synchronization.setFinishedAt(clock.instant());
        synchronization.setPlayersProcessed(playerCount);
        synchronization.setFailureCount(summary.failureCount());
        synchronization.setMatchesImported(summary.matchesImported());
        synchronization.setErrorMessage(
            nullableTruncatedErrorMessage(summary.errorMessages())
        );

        synchronizationRepository.save(synchronization);
    }

    /**
     * Marks a single-player execution as completed.
     *
     * @param synchronization execution to update
     * @param result          successful player outcome
     */
    private void completeSinglePlayerSynchronization(
        Synchronization synchronization,
        PlayerExecutionResult result
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
            truncateErrorMessage(safeErrorMessage(exception))
        );

        synchronizationRepository.save(synchronization);
    }

    /**
     * Persists a successful player outcome.
     *
     * @param synchronization global execution
     * @param result          successful player outcome
     */
    private void saveSuccessfulPlayerResult(
        Synchronization synchronization,
        PlayerExecutionResult result
    ) {
        savePlayerResult(
            synchronization,
            result.player(),
            SynchronizationStatus.COMPLETED,
            result.pagesFetched(),
            result.matchesImported(),
            null
        );
    }

    /**
     * Persists one player outcome within a global execution.
     *
     * @param synchronization global execution
     * @param player          processed player
     * @param status          outcome status
     * @param pagesFetched    retrieved page count
     * @param matchesImported imported match count
     * @param errorMessage    optional failure description
     */
    private void savePlayerResult(
        Synchronization synchronization,
        Player player,
        SynchronizationStatus status,
        int pagesFetched,
        int matchesImported,
        String errorMessage
    ) {
        SynchronizationPlayerResult result =
            new SynchronizationPlayerResult();

        result.setSynchronization(synchronization);
        result.setPlayer(player);
        result.setStatus(status);
        result.setPagesFetched(pagesFetched);
        result.setMatchesImported(matchesImported);
        result.setErrorMessage(
            errorMessage == null
                ? null
                : truncateErrorMessage(errorMessage)
        );

        playerResultRepository.save(result);
    }

    /**
     * Determines the final status of a multi-player execution.
     *
     * @param playerCount       selected player count
     * @param successfulPlayers successful player count
     * @param failureCount      failed player count
     * @return global synchronization status
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
     * Returns a non-empty diagnostic message for an exception.
     *
     * @param exception synchronization exception
     * @return safe error description
     */
    private String safeErrorMessage(RuntimeException exception) {
        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }

        return message;
    }

    /**
     * Maps a standard synchronization result to the shared internal model.
     *
     * @param result standard result
     * @return shared result
     */
    private PlayerExecutionResult toExecutionResult(
        PlayerSynchronizationResult result
    ) {
        return new PlayerExecutionResult(
            result.player(),
            result.pagesFetched(),
            result.matchesImported(),
            result.completedAt()
        );
    }

    /**
     * Maps a deep synchronization result to the shared internal model.
     *
     * @param result deep result
     * @return shared result
     */
    private PlayerExecutionResult toExecutionResult(
        PlayerDeepSynchronizationResult result
    ) {
        return new PlayerExecutionResult(
            result.player(),
            result.pagesFetched(),
            result.matchesImported(),
            result.completedAt()
        );
    }

    /**
     * Maps a persisted execution to its API representation.
     *
     * @param synchronization                 persisted execution
     * @param lastSuccessfulSynchronizationAt latest successful player timestamp
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
     * Returns {@code null} for an empty aggregate or a truncated value.
     *
     * @param message aggregated errors
     * @return nullable persisted value
     */
    private String nullableTruncatedErrorMessage(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }

        return truncateErrorMessage(message);
    }

    /**
     * Restricts an error message to the database column size.
     *
     * @param message original message
     * @return non-empty persisted message
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
     * Executes one player-level synchronization operation.
     */
    @FunctionalInterface
    private interface PlayerSynchronizationOperation {

        /**
         * Synchronizes one player.
         *
         * @param playerId player identifier
         * @return successful synchronization result
         */
        PlayerExecutionResult synchronize(long playerId);
    }

    /**
     * Shared internal representation of standard and deep results.
     *
     * @param player          synchronized player
     * @param pagesFetched    retrieved page count
     * @param matchesImported imported match count
     * @param completedAt     completion timestamp
     */
    private record PlayerExecutionResult(
        Player player,
        int pagesFetched,
        int matchesImported,
        Instant completedAt
    ) {
    }

    /**
     * Immutable aggregate of all player outcomes in one batch.
     *
     * @param successfulPlayers             successful player count
     * @param failureCount                  failed player count
     * @param matchesImported               total imported match count
     * @param lastSuccessfulSynchronizationAt latest successful timestamp
     * @param errorMessages                 aggregated failure descriptions
     */
    private record BatchSummary(
        int successfulPlayers,
        int failureCount,
        int matchesImported,
        Instant lastSuccessfulSynchronizationAt,
        String errorMessages
    ) {

        /**
         * Creates an empty summary.
         *
         * @return empty summary
         */
        private static BatchSummary empty() {
            return new BatchSummary(0, 0, 0, null, null);
        }

        /**
         * Adds a successful player outcome.
         *
         * @param result successful outcome
         * @return updated summary
         */
        private BatchSummary withSuccess(PlayerExecutionResult result) {
            return new BatchSummary(
                successfulPlayers + 1,
                failureCount,
                matchesImported + result.matchesImported(),
                latestInstant(
                    lastSuccessfulSynchronizationAt,
                    result.completedAt()
                ),
                errorMessages
            );
        }

        /**
         * Adds a failed player outcome.
         *
         * @param player       failed player
         * @param errorMessage failure description
         * @return updated summary
         */
        private BatchSummary withFailure(
            Player player,
            String errorMessage
        ) {
            String playerError = "Player "
                + player.getId()
                + ": "
                + errorMessage;
            String updatedErrors = errorMessages == null
                ? playerError
                : errorMessages + System.lineSeparator() + playerError;

            return new BatchSummary(
                successfulPlayers,
                failureCount + 1,
                matchesImported,
                lastSuccessfulSynchronizationAt,
                updatedErrors
            );
        }

        /**
         * Returns the most recent non-null timestamp.
         *
         * @param current   retained timestamp
         * @param candidate candidate timestamp
         * @return latest timestamp
         */
        private static Instant latestInstant(
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
    }
}
