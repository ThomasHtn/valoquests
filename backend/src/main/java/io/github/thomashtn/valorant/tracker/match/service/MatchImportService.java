package io.github.thomashtn.valorant.tracker.match.service;

import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchHistoryResponse;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchHistoryResponse.HenrikMatchData;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchMetadata;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchPlayer;
import io.github.thomashtn.valorant.tracker.henrik.mapper.HenrikMatchMapper;
import io.github.thomashtn.valorant.tracker.match.entity.Season;
import io.github.thomashtn.valorant.tracker.match.entity.ValorantMatch;
import io.github.thomashtn.valorant.tracker.match.model.GameModeSource;
import io.github.thomashtn.valorant.tracker.match.model.MatchImportResult;
import io.github.thomashtn.valorant.tracker.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valorant.tracker.match.repository.ValorantMatchRepository;
import io.github.thomashtn.valorant.tracker.player.entity.Player;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Imports completed Henrik matches idempotently for one tracked player.
 *
 * <p><strong>Concurrency.</strong> Two synchronizations may race to import the same match: two
 * tracked players who shared it, synced concurrently, or the same player caught up manually while a
 * scheduled run is still in progress. {@code external_match_id} and {@code (player_id, match_id)} are
 * both database-enforced unique constraints, so the loser of such a race fails with a constraint
 * violation rather than creating a duplicate. Match and player-match creation each run in their own
 * transaction so that failure is caught and resolved by reusing the winner's row, instead of poisoning
 * the page-wide transaction every other match on the same page still needs to commit through.
 */
@Service
public class MatchImportService {

    /**
     * Logger used to report operational and diagnostic information.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(MatchImportService.class);

    /**
     * Repository used to load and persist Valorant matches.
     */
    private final ValorantMatchRepository matchRepository;

    /**
     * Repository used to manage player-to-match associations.
     */
    private final PlayerMatchRepository playerMatchRepository;

    /**
     * Service used to resolve and persist match seasons.
     */
    private final SeasonResolutionService seasonResolutionService;

    /**
     * Mapper used to convert Henrik payloads into persistence entities.
     */
    private final HenrikMatchMapper mapper;

    /**
     * Runs one creation attempt in its own transaction, independent from the page-wide transaction.
     */
    private final TransactionTemplate newRowTransactionTemplate;

    /**
     * Creates the idempotent match import service.
     *
     * @param matchRepository         repository holding Valorant matches
     * @param playerMatchRepository   repository holding player-to-match associations
     * @param seasonResolutionService service resolving the season a match belongs to
     * @param mapper                  mapper turning Henrik payloads into entities
     * @param transactionManager      transaction manager used to isolate racy row creation
     */
    public MatchImportService(
        ValorantMatchRepository matchRepository,
        PlayerMatchRepository playerMatchRepository,
        SeasonResolutionService seasonResolutionService,
        HenrikMatchMapper mapper,
        PlatformTransactionManager transactionManager
    ) {
        this.matchRepository = matchRepository;
        this.playerMatchRepository = playerMatchRepository;
        this.seasonResolutionService = seasonResolutionService;
        this.mapper = mapper;

        this.newRowTransactionTemplate = new TransactionTemplate(transactionManager);
        this.newRowTransactionTemplate.setPropagationBehavior(
            TransactionDefinition.PROPAGATION_REQUIRES_NEW
        );
    }

    /**
     * Imports a Henrik page and exposes enough detail for safe pagination.
     *
     * @param player tracked player
     * @param response Henrik match-history response
     * @return detailed import counters
     */
    @Transactional
    public MatchImportResult importMatchesWithSummary(
        Player player,
        HenrikMatchHistoryResponse response
    ) {
        Objects.requireNonNull(player, "player must not be null");
        Objects.requireNonNull(response, "response must not be null");

        List<HenrikMatchData> matches = response.data();
        int imported = 0;
        int alreadyKnown = 0;
        int rejected = 0;
        int skipped = 0;

        for (HenrikMatchData source : matches) {
            ImportOutcome outcome = importMatch(player, source);
            switch (outcome) {
                case IMPORTED -> imported++;
                case ALREADY_KNOWN -> alreadyKnown++;
                case REJECTED -> rejected++;
                case SKIPPED -> skipped++;
            }
        }

        MatchImportResult result = new MatchImportResult(
            matches.size(),
            imported,
            alreadyKnown,
            rejected,
            skipped
        );

        LOGGER.debug(
            "Processed Henrik response for player {}: received={} imported={} alreadyKnown={} "
                + "rejected={} skipped={}",
            player.getId(),
            result.received(),
            result.imported(),
            result.alreadyKnown(),
            result.rejected(),
            result.skipped()
        );
        return result;
    }

    /**
     * Imports one match and classifies the processing outcome.
     */
    private ImportOutcome importMatch(
        Player player,
        HenrikMatchData source
    ) {
        String rejectionReason = findRejectionReason(source);
        if (rejectionReason != null) {
            LOGGER.debug(
                "Ignoring Henrik match for player {}: {}",
                player.getId(),
                rejectionReason
            );
            return ImportOutcome.REJECTED;
        }

        HenrikMatchPlayer sourcePlayer = findTrackedPlayer(player, source);
        if (sourcePlayer == null) {
            LOGGER.warn(
                "Ignoring Henrik match {} because player {} was not present in the payload",
                source.metadata().matchId(),
                player.getId()
            );
            return ImportOutcome.REJECTED;
        }

        HenrikMatchMetadata metadata = source.metadata();

        // Checked before any lookup so an ignored mode never creates a match row. An unresolved
        // queue is eligible on purpose: see GameMode.OTHER.
        HenrikMatchMapper.GameModeResolution resolution = mapper.resolveGameModeWithSource(metadata);
        if (!resolution.gameMode().isImportEligible()) {
            LOGGER.debug(
                "Skipping Henrik match {} for player {}: game mode {} is not imported",
                metadata.matchId(),
                player.getId(),
                resolution.gameMode()
            );
            return ImportOutcome.SKIPPED;
        }

        ValorantMatch match = findOrCreateMatch(source, metadata.matchId());
        enrichGameMode(match, resolution);

        if (playerMatchRepository.existsByPlayerIdAndMatchId(
            player.getId(),
            match.getId()
        )) {
            LOGGER.debug(
                "Skipping existing player-match association: player={} match={}",
                player.getId(),
                metadata.matchId()
            );
            return ImportOutcome.ALREADY_KNOWN;
        }

        return saveNewPlayerMatch(source, sourcePlayer, player, match)
            ? ImportOutcome.IMPORTED
            : ImportOutcome.ALREADY_KNOWN;
    }

    /**
     * Validates the minimum payload required to persist a match.
     *
     * <p>Names the failed precondition rather than returning a bare boolean: a whole game mode
     * disappearing because Henrik systematically omits one field is otherwise indistinguishable from
     * the player simply not having played it.
     *
     * @param source Henrik match payload
     * @return the unmet precondition, or {@code null} when the match can be persisted
     */
    private String findRejectionReason(HenrikMatchData source) {
        if (source == null || source.metadata() == null) {
            return "the payload carries no metadata";
        }

        HenrikMatchMetadata metadata = source.metadata();
        if (!Boolean.TRUE.equals(metadata.completed())) {
            return "the match is not completed";
        }
        if (metadata.matchId() == null || metadata.matchId().isBlank()) {
            return "the match identifier is missing";
        }
        if (metadata.startedAt() == null) {
            return "the start instant is missing";
        }
        if (metadata.season() == null
            || metadata.season().id() == null
            || metadata.season().id().isBlank()) {
            return "the season identifier is missing for match " + metadata.matchId();
        }
        return null;
    }

    /**
     * Finds the tracked player in the Henrik match participant list.
     */
    private HenrikMatchPlayer findTrackedPlayer(
        Player player,
        HenrikMatchData source
    ) {
        String puuid = player.getRiotPuuid();
        if (puuid == null || puuid.isBlank() || source.players() == null) {
            return null;
        }

        return source.players().stream()
            .filter(Objects::nonNull)
            .filter(candidate -> puuid.equals(candidate.puuid()))
            .findFirst()
            .orElse(null);
    }

    /**
     * Finds the shared match, creating it when this is the first tracked player to report it.
     *
     * <p>Creation runs in its own transaction so the unique-constraint violation a losing concurrent
     * creation hits can be resolved by reusing the winner's row, without poisoning the page-wide
     * transaction the rest of this page's matches still need to commit through.
     *
     * @param source         Henrik match payload
     * @param externalMatchId Henrik match identifier
     * @return the persisted match, created by this call or by a concurrent one
     */
    private ValorantMatch findOrCreateMatch(HenrikMatchData source, String externalMatchId) {
        return matchRepository.findByExternalMatchId(externalMatchId)
            .orElseGet(() -> {
                try {
                    return newRowTransactionTemplate.execute(status -> createMatch(source));
                } catch (DataIntegrityViolationException raceLost) {
                    LOGGER.debug(
                        "Match {} was created concurrently by another synchronization: reusing it",
                        externalMatchId
                    );
                    return matchRepository.findByExternalMatchId(externalMatchId)
                        .orElseThrow(() -> raceLost);
                }
            });
    }

    /**
     * Creates and persists a match that is not already stored.
     */
    private ValorantMatch createMatch(HenrikMatchData source) {
        Season season = seasonResolutionService.resolve(
            source.metadata().season()
        );
        return matchRepository.save(
            mapper.toValorantMatch(source, season)
        );
    }

    /**
     * Enriches an already-stored match's game mode when this synchronization resolved it more
     * confidently than whatever produced the stored value.
     *
     * <p>Priority is what makes this safe to call on every import, including a freshly created match:
     * a value from a source of equal priority to the stored one still refreshes to the latest Henrik
     * data, but a lower-priority source, or {@link GameModeSource#MANUALLY_CORRECTED} already stored,
     * is left untouched. A synchronization can therefore fill in a match Henrik under-classified the
     * first time it was seen, without ever undoing an administrator's correction.
     *
     * @param match      persisted match, possibly stale
     * @param resolution mode this synchronization resolved for the match
     */
    private void enrichGameMode(ValorantMatch match, HenrikMatchMapper.GameModeResolution resolution) {
        boolean unchanged = resolution.gameMode() == match.getGameMode()
            && resolution.source() == match.getGameModeSource();
        if (unchanged || !resolution.source().outranksOrEquals(match.getGameModeSource())) {
            return;
        }

        LOGGER.info(
            "Enriching game mode for match {}: {} ({}) -> {} ({})",
            match.getExternalMatchId(),
            match.getGameMode(),
            match.getGameModeSource(),
            resolution.gameMode(),
            resolution.source()
        );
        match.setGameMode(resolution.gameMode());
        match.setGameModeSource(resolution.source());
        matchRepository.save(match);
    }

    /**
     * Saves a new player-match association, tolerating the race of two concurrent synchronizations of
     * the same player both importing it for the first time.
     *
     * @param source       Henrik match payload
     * @param sourcePlayer the tracked player's entry in that payload
     * @param player       tracked player the association belongs to
     * @param match        persisted match the association attaches to
     * @return {@code true} when this call created the association, {@code false} when a concurrent
     *         call already had
     */
    private boolean saveNewPlayerMatch(
        HenrikMatchData source,
        HenrikMatchPlayer sourcePlayer,
        Player player,
        ValorantMatch match
    ) {
        try {
            newRowTransactionTemplate.executeWithoutResult(status ->
                playerMatchRepository.save(mapper.toPlayerMatch(source, sourcePlayer, player, match))
            );
            return true;
        } catch (DataIntegrityViolationException raceLost) {
            LOGGER.debug(
                "Player-match association was created concurrently by another synchronization: "
                    + "player={} match={}",
                player.getId(),
                match.getExternalMatchId()
            );
            return false;
        }
    }

    /**
     * Internal outcome used to build import counters.
     */
    private enum ImportOutcome {
        IMPORTED,
        ALREADY_KNOWN,
        REJECTED,
        SKIPPED
    }
}
