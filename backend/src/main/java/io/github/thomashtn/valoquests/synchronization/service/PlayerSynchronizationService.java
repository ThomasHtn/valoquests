package io.github.thomashtn.valoquests.synchronization.service;

import io.github.thomashtn.valoquests.henrik.client.HenrikMmrClient;
import io.github.thomashtn.valoquests.henrik.dto.mmr.HenrikMmrResponse;
import io.github.thomashtn.valoquests.henrik.mapper.HenrikMmrMapper;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.exception.PlayerNotFoundException;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.player.service.PlayerAccountResolutionService;
import io.github.thomashtn.valoquests.shared.util.NonTransactionalGuard;
import io.github.thomashtn.valoquests.synchronization.model.MatchHistoryWalkResult;
import io.github.thomashtn.valoquests.synchronization.model.PlayerSynchronizationResult;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the synchronization of one tracked player.
 *
 * <p>Resolves the Riot account, refreshes the competitive rank, then delegates the match history to
 * {@link SeasonMatchHistoryWalker}, which owns the season scope and pagination rules.
 *
 * <p><strong>Deliberately not transactional.</strong> Henrik calls must stay outside a database
 * transaction, and the walker relies on each of its steps committing independently to keep the
 * per-season completion flag honest. See {@link SeasonSynchronizationStateService}. Enforced at
 * entry by {@link NonTransactionalGuard}, so wrapping this method in {@code @Transactional} fails
 * fast instead of silently defeating the checkpoint.
 */
@Service
public class PlayerSynchronizationService {

    /**
     * Logger used to report operational and diagnostic information.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(PlayerSynchronizationService.class);

    /**
     * Repository used to load and persist tracked players.
     */
    private final PlayerRepository playerRepository;

    /**
     * Service used to resolve missing Riot account identifiers.
     */
    private final PlayerAccountResolutionService accountResolutionService;

    /**
     * Henrik client used to retrieve the current competitive rank.
     */
    private final HenrikMmrClient mmrClient;

    /**
     * Mapper used to apply Henrik rank data to tracked players.
     */
    private final HenrikMmrMapper mmrMapper;

    /**
     * Service walking the player's match history within the current and previous seasons.
     */
    private final SeasonMatchHistoryWalker matchHistoryWalker;

    /**
     * Clock used to produce deterministic timestamps.
     */
    private final Clock clock;

    /**
     * Creates the player synchronization service.
     *
     * @param playerRepository         repository holding tracked players
     * @param accountResolutionService service resolving missing Riot account identifiers
     * @param mmrClient                Henrik client returning competitive ranks
     * @param mmrMapper                mapper turning Henrik rank payloads into player fields
     * @param matchHistoryWalker       walker importing the current and previous seasons' match
     *                                 history
     * @param clock                    clock producing deterministic timestamps
     */
    public PlayerSynchronizationService(
        PlayerRepository playerRepository,
        PlayerAccountResolutionService accountResolutionService,
        HenrikMmrClient mmrClient,
        HenrikMmrMapper mmrMapper,
        SeasonMatchHistoryWalker matchHistoryWalker,
        Clock clock
    ) {
        this.playerRepository = playerRepository;
        this.accountResolutionService = accountResolutionService;
        this.mmrClient = mmrClient;
        this.mmrMapper = mmrMapper;
        this.matchHistoryWalker = matchHistoryWalker;
        this.clock = clock;
    }

    /**
     * Synchronizes the Riot account, current rank and every match of the current and previous
     * seasons.
     *
     * @param playerId internal player identifier
     * @return synchronization result
     */
    public PlayerSynchronizationResult synchronize(Long playerId) {
        NonTransactionalGuard.assertNoActiveTransaction("Player synchronization");

        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new PlayerNotFoundException(playerId));
        Player resolvedPlayer = accountResolutionService.resolvePuuid(player);

        LOGGER.info(
            "Starting synchronization for player {} ({})",
            resolvedPlayer.getId(),
            resolvedPlayer.getDisplayName()
        );

        HenrikMmrResponse mmrResponse = mmrClient.getCurrentMmr(
            resolvedPlayer.getRiotPuuid()
        );
        mmrMapper.updatePlayer(mmrResponse, resolvedPlayer);

        MatchHistoryWalkResult walkResult = matchHistoryWalker.walk(resolvedPlayer);
        Instant completedAt = clock.instant();
        resolvedPlayer.setLastSuccessfulSynchronizationAt(completedAt);
        Player savedPlayer = playerRepository.save(resolvedPlayer);

        LOGGER.info(
            "Completed synchronization for player {}: pages={} importedMatches={} stopReason={}",
            savedPlayer.getId(),
            walkResult.pagesFetched(),
            walkResult.matchesImported(),
            walkResult.stopReason()
        );

        return new PlayerSynchronizationResult(
            savedPlayer,
            walkResult.pagesFetched(),
            walkResult.matchesImported(),
            completedAt,
            walkResult.stopReason()
        );
    }
}
