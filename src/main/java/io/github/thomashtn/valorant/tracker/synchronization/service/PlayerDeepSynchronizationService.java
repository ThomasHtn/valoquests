package io.github.thomashtn.valorant.tracker.synchronization.service;

import io.github.thomashtn.valorant.tracker.henrik.client.HenrikMatchClient;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchHistoryResponse;
import io.github.thomashtn.valorant.tracker.match.service.MatchImportService;
import io.github.thomashtn.valorant.tracker.player.entity.Player;
import io.github.thomashtn.valorant.tracker.player.exception.PlayerNotFoundException;
import io.github.thomashtn.valorant.tracker.player.repository.PlayerRepository;
import io.github.thomashtn.valorant.tracker.player.service.PlayerAccountResolutionService;
import io.github.thomashtn.valorant.tracker.synchronization.model.PlayerDeepSynchronizationResult;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

/**
 * Imports every match-history page currently exposed by Henrik for one player.
 */
@Service
public class PlayerDeepSynchronizationService {

    /**
     * Maximum page size accepted by the current Henrik match client.
     */
    private static final int PAGE_SIZE = 10;

    /**
     * Safety limit preventing an accidental infinite pagination loop.
     */
    private static final int MAXIMUM_PAGE_COUNT = 1_000;

    private final PlayerRepository playerRepository;
    private final PlayerAccountResolutionService accountResolutionService;
    private final HenrikMatchClient matchClient;
    private final MatchImportService matchImportService;
    private final Clock clock;

    /**
     * Creates the deep-synchronization service.
     *
     * @param playerRepository         tracked-player repository
     * @param accountResolutionService Riot account resolution service
     * @param matchClient              Henrik match-history client
     * @param matchImportService       idempotent match-import service
     * @param clock                    application clock
     */
    public PlayerDeepSynchronizationService(
        PlayerRepository playerRepository,
        PlayerAccountResolutionService accountResolutionService,
        HenrikMatchClient matchClient,
        MatchImportService matchImportService,
        Clock clock
    ) {
        this.playerRepository = playerRepository;
        this.accountResolutionService = accountResolutionService;
        this.matchClient = matchClient;
        this.matchImportService = matchImportService;
        this.clock = clock;
    }

    /**
     * Imports the complete match history available through Henrik pagination.
     *
     * <p>The process deliberately does not stop when one page imports zero
     * matches. The first page is normally already present after a standard
     * synchronization, while older pages may still contain unseen matches.</p>
     *
     * <p>Pagination stops only when Henrik returns an empty page or a page
     * containing fewer elements than requested.</p>
     *
     * @param playerId internal player identifier
     * @return completed deep-synchronization result
     */
    public PlayerDeepSynchronizationResult synchronize(long playerId) {
        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new PlayerNotFoundException(playerId));

        Player resolvedPlayer =
            accountResolutionService.resolvePuuid(player);

        int start = 0;
        int pagesFetched = 0;
        int matchesImported = 0;

        while (true) {
            if (pagesFetched >= MAXIMUM_PAGE_COUNT) {
                throw new IllegalStateException(
                    "Deep synchronization exceeded the maximum page count"
                );
            }

            HenrikMatchHistoryResponse response =
                matchClient.getMatches(
                    resolvedPlayer.getRiotPuuid(),
                    start,
                    PAGE_SIZE
                );

            int receivedMatchCount = response.data().size();

            if (receivedMatchCount == 0) {
                break;
            }

            pagesFetched++;

            matchesImported += matchImportService.importMatches(
                resolvedPlayer,
                response
            );

            if (receivedMatchCount < PAGE_SIZE) {
                break;
            }

            /*
             * Henrik's start parameter is an item offset, not a page number.
             */
            start += receivedMatchCount;

        }

        Instant completedAt = clock.instant();

        resolvedPlayer.setLastSuccessfulSynchronizationAt(
            completedAt
        );

        Player savedPlayer =
            playerRepository.save(resolvedPlayer);

        return new PlayerDeepSynchronizationResult(
            savedPlayer,
            pagesFetched,
            matchesImported,
            completedAt
        );
    }

}
