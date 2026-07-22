package io.github.thomashtn.valorant.tracker.match.service;

import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchHistoryResponse;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchHistoryResponse.HenrikMatchData;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchMetadata;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchPlayer;
import io.github.thomashtn.valorant.tracker.henrik.mapper.HenrikMatchMapper;
import io.github.thomashtn.valorant.tracker.match.entity.Season;
import io.github.thomashtn.valorant.tracker.match.entity.ValorantMatch;
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
     * Imports every valid match and returns the number of inserted associations.
     *
     * @param player tracked player
     * @param response Henrik match-history response
     * @return number of newly imported player-match associations
     */
    @Transactional
    public int importMatches(
        Player player,
        HenrikMatchHistoryResponse response
    ) {
        return processMatches(player, response).imported();
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
        return processMatches(player, response);
    }

    /**
     * Performs the shared import algorithm for both public transactional entry points.
     *
     * @param player tracked player
     * @param response Henrik match-history response
     * @return detailed import counters
     */
    private MatchImportResult processMatches(
        Player player,
        HenrikMatchHistoryResponse response
    ) {
        Objects.requireNonNull(player, "player must not be null");
        Objects.requireNonNull(response, "response must not be null");

        List<HenrikMatchData> matches = response.data() == null
            ? List.of()
            : response.data();
        int imported = 0;
        int alreadyKnown = 0;
        int rejected = 0;

        for (HenrikMatchData source : matches) {
            ImportOutcome outcome = importMatch(player, source);
            switch (outcome) {
                case IMPORTED -> imported++;
                case ALREADY_KNOWN -> alreadyKnown++;
                case REJECTED -> rejected++;
            }
        }

        MatchImportResult result = new MatchImportResult(
            matches.size(),
            imported,
            alreadyKnown,
            rejected
        );

        LOGGER.debug(
            "Processed Henrik response for player {}: received={} imported={} alreadyKnown={} rejected={}",
            player.getId(),
            result.received(),
            result.imported(),
            result.alreadyKnown(),
            result.rejected()
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
        if (!isImportable(source)) {
            LOGGER.debug(
                "Ignoring malformed or incomplete Henrik match for player {}",
                player.getId()
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
     */
    private boolean isImportable(HenrikMatchData source) {
        if (source == null || source.metadata() == null) {
            return false;
        }

        HenrikMatchMetadata metadata = source.metadata();
        return Boolean.TRUE.equals(metadata.completed())
            && metadata.matchId() != null
            && !metadata.matchId().isBlank()
            && metadata.startedAt() != null
            && metadata.season() != null
            && metadata.season().id() != null
            && !metadata.season().id().isBlank();
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
        REJECTED
    }
}
