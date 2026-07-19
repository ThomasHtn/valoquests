package io.github.thomashtn.valorant.tracker.synchronization.service;

import io.github.thomashtn.valorant.tracker.henrik.client.HenrikMatchClient;
import io.github.thomashtn.valorant.tracker.henrik.client.HenrikMmrClient;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchHistoryResponse;
import io.github.thomashtn.valorant.tracker.henrik.dto.mmr.HenrikMmrResponse;
import io.github.thomashtn.valorant.tracker.henrik.mapper.HenrikMmrMapper;
import io.github.thomashtn.valorant.tracker.match.service.MatchImportService;
import io.github.thomashtn.valorant.tracker.player.entity.Player;
import io.github.thomashtn.valorant.tracker.player.exception.PlayerNotFoundException;
import io.github.thomashtn.valorant.tracker.player.repository.PlayerRepository;
import io.github.thomashtn.valorant.tracker.player.service.PlayerAccountResolutionService;
import io.github.thomashtn.valorant.tracker.synchronization.model.PlayerSynchronizationResult;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Orchestrates the synchronization of one tracked Valorant player. */
@Service
public class PlayerSynchronizationService {

    private static final int RECENT_MATCH_PAGE_SIZE = 10;

    private final PlayerRepository playerRepository;
    private final PlayerAccountResolutionService accountResolutionService;
    private final HenrikMmrClient mmrClient;
    private final HenrikMmrMapper mmrMapper;
    private final HenrikMatchClient matchClient;
    private final MatchImportService matchImportService;
    private final Clock clock;

    public PlayerSynchronizationService(
        PlayerRepository playerRepository,
        PlayerAccountResolutionService accountResolutionService,
        HenrikMmrClient mmrClient,
        HenrikMmrMapper mmrMapper,
        HenrikMatchClient matchClient,
        MatchImportService matchImportService,
        Clock clock
    ) {
        this.playerRepository = playerRepository;
        this.accountResolutionService = accountResolutionService;
        this.mmrClient = mmrClient;
        this.mmrMapper = mmrMapper;
        this.matchClient = matchClient;
        this.matchImportService = matchImportService;
        this.clock = clock;
    }

    /**
     * Synchronizes the account, current rank and recent completed matches of
     * one tracked player.
     *
     * @param playerId internal player identifier
     * @return synchronization result
     */
    @Transactional
    public PlayerSynchronizationResult synchronize(Long playerId) {
        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new PlayerNotFoundException(playerId));

        Player resolvedPlayer =
            accountResolutionService.resolvePuuid(player);

        HenrikMmrResponse mmrResponse = mmrClient.getCurrentMmr(
            resolvedPlayer.getRiotPuuid()
        );
        mmrMapper.updatePlayer(mmrResponse, resolvedPlayer);

        HenrikMatchHistoryResponse matchResponse =
            matchClient.getMatches(
                resolvedPlayer.getRiotPuuid(),
                0,
                RECENT_MATCH_PAGE_SIZE
            );

        int matchesImported = matchImportService.importMatches(
            resolvedPlayer,
            matchResponse
        );

        Instant completedAt = clock.instant();
        resolvedPlayer.setLastSuccessfulSynchronizationAt(completedAt);

        Player savedPlayer = playerRepository.save(resolvedPlayer);

        return new PlayerSynchronizationResult(
            savedPlayer,
            matchesImported,
            completedAt
        );
    }
}
