package io.github.thomashtn.valorant.tracker.match.service;

import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchHistoryResponse;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchHistoryResponse.HenrikMatchData;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchMetadata;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchPlayer;
import io.github.thomashtn.valorant.tracker.henrik.mapper.HenrikMatchMapper;
import io.github.thomashtn.valorant.tracker.match.entity.Season;
import io.github.thomashtn.valorant.tracker.match.entity.ValorantMatch;
import io.github.thomashtn.valorant.tracker.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valorant.tracker.match.repository.ValorantMatchRepository;
import io.github.thomashtn.valorant.tracker.player.entity.Player;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Imports completed Henrik matches idempotently for one tracked player. */
@Service
public class MatchImportService {

    private final ValorantMatchRepository matchRepository;
    private final PlayerMatchRepository playerMatchRepository;
    private final SeasonResolutionService seasonResolutionService;
    private final HenrikMatchMapper mapper;

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
     * Imports every valid match from one Henrik response.
     *
     * @return number of new player-match associations inserted
     */
    @Transactional
    public int importMatches(
        Player player,
        HenrikMatchHistoryResponse response
    ) {
        Objects.requireNonNull(player, "player must not be null");
        Objects.requireNonNull(response, "response must not be null");

        int imported = 0;

        for (HenrikMatchData source : response.data()) {
            if (importMatch(player, source)) {
                imported++;
            }
        }

        return imported;
    }

    private boolean importMatch(
        Player player,
        HenrikMatchData source
    ) {
        if (!isImportable(source)) {
            return false;
        }

        HenrikMatchPlayer sourcePlayer = findTrackedPlayer(
            player,
            source
        );

        if (sourcePlayer == null) {
            return false;
        }

        HenrikMatchMetadata metadata = source.metadata();

        ValorantMatch match = matchRepository
            .findByExternalMatchId(metadata.matchId())
            .orElseGet(() -> createMatch(source));

        if (playerMatchRepository.existsByPlayerIdAndMatchId(
            player.getId(),
            match.getId()
        )) {
            return false;
        }

        playerMatchRepository.save(
            mapper.toPlayerMatch(
                source,
                sourcePlayer,
                player,
                match
            )
        );

        return true;
    }

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

    private HenrikMatchPlayer findTrackedPlayer(
        Player player,
        HenrikMatchData source
    ) {
        String puuid = player.getRiotPuuid();

        if (puuid == null || puuid.isBlank()) {
            return null;
        }

        return source.players().stream()
            .filter(candidate -> puuid.equals(candidate.puuid()))
            .findFirst()
            .orElse(null);
    }

    private ValorantMatch createMatch(HenrikMatchData source) {
        Season season = seasonResolutionService.resolve(
            source.metadata().season()
        );

        return matchRepository.save(
            mapper.toValorantMatch(source, season)
        );
    }
}
