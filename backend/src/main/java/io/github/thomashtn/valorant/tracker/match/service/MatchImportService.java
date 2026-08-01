package io.github.thomashtn.valorant.tracker.match.service;

import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchHistoryResponse;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchHistoryResponse.HenrikMatchData;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchMetadata;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchPlayer;
import io.github.thomashtn.valorant.tracker.henrik.mapper.HenrikMatchMapper;
import io.github.thomashtn.valorant.tracker.match.entity.Season;
import io.github.thomashtn.valorant.tracker.match.entity.ValorantMatch;
import io.github.thomashtn.valorant.tracker.match.model.GameMode;
import io.github.thomashtn.valorant.tracker.match.model.MatchImportResult;
import io.github.thomashtn.valorant.tracker.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valorant.tracker.match.repository.ValorantMatchRepository;
import io.github.thomashtn.valorant.tracker.player.entity.Player;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Imports completed Henrik matches idempotently for one tracked player.
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
     * Creates the idempotent match import service.
     *
     * @param matchRepository         repository holding Valorant matches
     * @param playerMatchRepository   repository holding player-to-match associations
     * @param seasonResolutionService service resolving the season a match belongs to
     * @param mapper                  mapper turning Henrik payloads into entities
     */
    public MatchImportService(
        ValorantMatchRepository matchRepository,
        PlayerMatchRepository playerMatchRepository,
        SeasonResolutionService seasonResolutionService,
        HenrikMatchMapper mapper
    ) {
        this.matchRepository = matchRepository;
        this.playerMatchRepository = playerMatchRepository;
        this.seasonResolutionService = seasonResolutionService;
        this.mapper = mapper;
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
        GameMode gameMode = mapper.resolveGameMode(metadata);
        if (!gameMode.isImportEligible()) {
            LOGGER.debug(
                "Skipping Henrik match {} for player {}: game mode {} is not imported",
                metadata.matchId(),
                player.getId(),
                gameMode
            );
            return ImportOutcome.SKIPPED;
        }

        ValorantMatch match = matchRepository
            .findByExternalMatchId(metadata.matchId())
            .orElseGet(() -> createMatch(source));

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

        playerMatchRepository.save(
            mapper.toPlayerMatch(source, sourcePlayer, player, match)
        );
        return ImportOutcome.IMPORTED;
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
     * Internal outcome used to build import counters.
     */
    private enum ImportOutcome {
        IMPORTED,
        ALREADY_KNOWN,
        REJECTED,
        SKIPPED
    }
}
