package io.github.thomashtn.valoquests.match.service;

import io.github.thomashtn.valoquests.henrik.dto.match.HenrikMatchHistoryResponse.HenrikMatchData;
import io.github.thomashtn.valoquests.henrik.dto.match.HenrikMatchMetadata;
import io.github.thomashtn.valoquests.henrik.dto.match.HenrikMatchPlayer;
import io.github.thomashtn.valoquests.match.entity.Season;
import io.github.thomashtn.valoquests.match.entity.ValorantMatch;
import io.github.thomashtn.valoquests.match.mapper.HenrikMatchMapper;
import io.github.thomashtn.valoquests.match.model.GameModeResolution;
import io.github.thomashtn.valoquests.match.model.GameModeSource;
import io.github.thomashtn.valoquests.match.model.MatchImportOutcome;
import io.github.thomashtn.valoquests.match.model.MatchImportResult;
import io.github.thomashtn.valoquests.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valoquests.match.repository.ValorantMatchRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.shared.util.ConcurrentRowCreation;
import io.github.thomashtn.valoquests.shared.util.NonTransactionalGuard;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Imports completed Henrik matches idempotently for one tracked player.
 *
 * <p>Deliberately not transactional: each match commits on its own, and a lost unique-constraint race
 * reuses the winner's row, which is only safe outside a transaction.
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
     * Imports the matches of a Henrik page and exposes enough detail for safe pagination.
     *
     * @param player  tracked player
     * @param matches Henrik matches of the page, possibly holding {@code null} entries
     * @return detailed import counters
     * @throws IllegalStateException when called inside a transaction
     */
    public MatchImportResult importPage(Player player, List<HenrikMatchData> matches) {
        NonTransactionalGuard.assertNoActiveTransaction("Match import");
        Objects.requireNonNull(player, "player must not be null");
        Objects.requireNonNull(matches, "matches must not be null");

        Map<MatchImportOutcome, Integer> counts = new EnumMap<>(MatchImportOutcome.class);
        for (HenrikMatchData source : matches) {
            counts.merge(importMatch(player, source), 1, Integer::sum);
        }
        MatchImportResult result = MatchImportResult.of(matches.size(), counts);

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
    private MatchImportOutcome importMatch(
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
            return MatchImportOutcome.REJECTED;
        }

        HenrikMatchPlayer sourcePlayer = findTrackedPlayer(player, source);
        if (sourcePlayer == null) {
            LOGGER.warn(
                "Ignoring Henrik match {} because player {} was not present in the payload",
                source.metadata().matchId(),
                player.getId()
            );
            return MatchImportOutcome.REJECTED;
        }

        HenrikMatchMetadata metadata = source.metadata();

        // Checked first so an ignored mode never creates a row; an unresolved queue is eligible (GameMode.OTHER).
        GameModeResolution resolution = mapper.resolveGameModeWithSource(metadata);
        if (!resolution.gameMode().isImportEligible()) {
            LOGGER.debug(
                "Skipping Henrik match {} for player {}: game mode {} is not imported",
                metadata.matchId(),
                player.getId(),
                resolution.gameMode()
            );
            return MatchImportOutcome.SKIPPED;
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
            return MatchImportOutcome.ALREADY_KNOWN;
        }

        return saveNewPlayerMatch(source, sourcePlayer, player, match)
            ? MatchImportOutcome.IMPORTED
            : MatchImportOutcome.ALREADY_KNOWN;
    }

    /**
     * Validates the minimum payload required to persist a match.
     *
     * <p>Returns a reason rather than a boolean, so a field Henrik always omits shows up in the logs.
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
        if (source.seasonId() == null) {
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
     * @param source         Henrik match payload
     * @param externalMatchId Henrik match identifier
     * @return the persisted match, created by this call or by a concurrent one
     */
    private ValorantMatch findOrCreateMatch(HenrikMatchData source, String externalMatchId) {
        return ConcurrentRowCreation.findOrCreate(
            () -> matchRepository.findByExternalMatchId(externalMatchId),
            () -> createMatch(source)
        );
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
     * Updates a stored match's game mode when this synchronization's source ranks at least as high.
     *
     * <p>Safe on every import: a lower source never overwrites, so a
     * {@link GameModeSource#MANUALLY_CORRECTED} value is never undone.
     *
     * @param match      persisted match, possibly stale
     * @param resolution mode this synchronization resolved for the match
     */
    private void enrichGameMode(ValorantMatch match, GameModeResolution resolution) {
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
     * Saves a new player-match association, tolerating a concurrent synchronization creating it first.
     *
     * @param source       Henrik match payload
     * @param sourcePlayer the tracked player's entry in that payload
     * @param player       tracked player the association belongs to
     * @param match        persisted match the association attaches to
     * @return {@code false} when a concurrent call created it first
     */
    private boolean saveNewPlayerMatch(
        HenrikMatchData source,
        HenrikMatchPlayer sourcePlayer,
        Player player,
        ValorantMatch match
    ) {
        try {
            playerMatchRepository.save(mapper.toPlayerMatch(source, sourcePlayer, player, match));
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
}
