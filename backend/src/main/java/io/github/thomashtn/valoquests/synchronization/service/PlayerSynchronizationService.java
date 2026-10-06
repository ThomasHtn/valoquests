package io.github.thomashtn.valoquests.synchronization.service;

import io.github.thomashtn.valoquests.henrik.client.HenrikMmrClient;
import io.github.thomashtn.valoquests.henrik.dto.mmr.HenrikMmrResponse;
import io.github.thomashtn.valoquests.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.exception.PlayerNotFoundException;
import io.github.thomashtn.valoquests.player.mapper.HenrikMmrMapper;
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
 * <p>Deliberately not transactional, enforced by {@link NonTransactionalGuard}. The rank is refreshed
 * only when a match arrived since the last successful pass.
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
     * Repository telling whether matches arrived since the last successful pass.
     */
    private final PlayerMatchRepository playerMatchRepository;

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
     * @param matchHistoryWalker       walker importing the current and previous seasons
     * @param playerMatchRepository    repository of the player's imported matches
     * @param clock                    clock producing deterministic timestamps
     */
    public PlayerSynchronizationService(
        PlayerRepository playerRepository,
        PlayerAccountResolutionService accountResolutionService,
        HenrikMmrClient mmrClient,
        HenrikMmrMapper mmrMapper,
        SeasonMatchHistoryWalker matchHistoryWalker,
        PlayerMatchRepository playerMatchRepository,
        Clock clock
    ) {
        this.playerRepository = playerRepository;
        this.accountResolutionService = accountResolutionService;
        this.mmrClient = mmrClient;
        this.mmrMapper = mmrMapper;
        this.matchHistoryWalker = matchHistoryWalker;
        this.playerMatchRepository = playerMatchRepository;
        this.clock = clock;
    }

    /**
     * Synchronizes the Riot account, rank and matches of the current and previous seasons.
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

        MatchHistoryWalkResult walkResult = matchHistoryWalker.walk(resolvedPlayer);
        if (rankMayHaveChanged(resolvedPlayer, walkResult)) {
            HenrikMmrResponse mmrResponse = mmrClient.getCurrentMmr(resolvedPlayer.getRiotPuuid());
            mmrMapper.updatePlayer(mmrResponse, resolvedPlayer);
        }

        Instant completedAt = clock.instant();
        resolvedPlayer.setLastSuccessfulSynchronizationAt(completedAt);
        // Only the synchronized fields: the player was loaded minutes ago and may have been edited.
        playerRepository.recordSuccessfulSynchronization(
            resolvedPlayer.getId(),
            resolvedPlayer.getCompetitiveTier(),
            resolvedPlayer.getRankRating(),
            completedAt
        );

        LOGGER.info(
            "Completed synchronization for player {}: pages={} importedMatches={} stopReason={}",
            resolvedPlayer.getId(),
            walkResult.pagesFetched(),
            walkResult.matchesImported(),
            walkResult.stopReason()
        );

        return new PlayerSynchronizationResult(
            resolvedPlayer,
            walkResult.pagesFetched(),
            walkResult.matchesImported(),
            walkResult.stopReason()
        );
    }

    /**
     * Tells whether the rank needs a Henrik call: only a played match can move it.
     *
     * <p>Matches imported by a pass that then failed count too, so a failed rank call is retried.
     *
     * @param player     synchronized player, holding the previous successful pass instant
     * @param walkResult outcome of this pass's match history walk
     * @return {@code true} when the stored rank may be outdated
     */
    private boolean rankMayHaveChanged(Player player, MatchHistoryWalkResult walkResult) {
        Instant lastSuccessfulAt = player.getLastSuccessfulSynchronizationAt();
        return walkResult.matchesImported() > 0
            || lastSuccessfulAt == null
            || playerMatchRepository.existsByPlayerIdAndCreatedAtAfter(player.getId(), lastSuccessfulAt);
    }
}
