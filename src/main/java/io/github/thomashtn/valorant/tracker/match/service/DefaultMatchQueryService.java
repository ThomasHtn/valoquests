package io.github.thomashtn.valorant.tracker.match.service;

import io.github.thomashtn.valorant.tracker.match.dto.MatchResponse;
import io.github.thomashtn.valorant.tracker.match.entity.PlayerMatch;
import io.github.thomashtn.valorant.tracker.match.model.MatchResult;
import io.github.thomashtn.valorant.tracker.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valorant.tracker.player.exception.PlayerNotFoundException;
import io.github.thomashtn.valorant.tracker.player.repository.PlayerRepository;
import io.github.thomashtn.valorant.tracker.shared.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Implements filtered and paginated player match-history consultation.
 */
@Service
@Transactional(readOnly = true)
public class DefaultMatchQueryService implements MatchQueryService {

    /**
     * Maximum number of historical weeks accepted by one request.
     */
    private static final int MAXIMUM_PAGE_SIZE = 100;

    /**
     * Repository used to load tracked players.
     */
    private final PlayerRepository playerRepository;

    /**
     * Repository used to query persisted player matches.
     */
    private final PlayerMatchRepository playerMatchRepository;

    /**
     * Creates the persisted match query service.
     *
     * @param playerRepository      repository used to validate tracked players
     * @param playerMatchRepository repository used to query persisted player matches
     */
    public DefaultMatchQueryService(
        PlayerRepository playerRepository,
        PlayerMatchRepository playerMatchRepository
    ) {
        this.playerRepository = playerRepository;
        this.playerMatchRepository = playerMatchRepository;
    }

    /**
     * Returns one filtered page of matches for a tracked player.
     *
     * @param playerId internal player identifier
     * @param page     zero-based page index
     * @param size     requested page size
     * @param seasonId optional season identifier
     * @param map      optional map name
     * @param agent    optional agent name
     * @param result   optional match result
     * @return requested page of player matches
     */
    @Override
    public PageResponse<MatchResponse> findByPlayer(
        long playerId,
        int page,
        int size,
        Long seasonId,
        String map,
        String agent,
        String result
    ) {
        validatePagination(page, size);
        if (!playerRepository.existsById(playerId)) {
            throw new PlayerNotFoundException(playerId);
        }
        MatchResult parsedResult = parseResult(result);
        Page<PlayerMatch> matches = playerMatchRepository.findHistory(
            playerId,
            seasonId,
            normalize(map),
            normalize(agent),
            parsedResult,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "match.startedAt", "id"))
        );
        return new PageResponse<>(
            matches.stream().map(this::toResponse).toList(),
            matches.getNumber(),
            matches.getSize(),
            matches.getTotalElements(),
            matches.getTotalPages()
        );
    }

    private MatchResponse toResponse(PlayerMatch playerMatch) {
        int shots = playerMatch.getHeadshots() + playerMatch.getBodyshots() + playerMatch.getLegshots();
        BigDecimal kda = BigDecimal.valueOf(playerMatch.getKills() + playerMatch.getAssists())
            .divide(BigDecimal.valueOf(Math.max(1, playerMatch.getDeaths())), 2, RoundingMode.HALF_UP);
        BigDecimal headshotPercentage = shots == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(playerMatch.getHeadshots())
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(shots), 2, RoundingMode.HALF_UP);
        boolean redTeam = "Red".equalsIgnoreCase(playerMatch.getTeamId());
        Integer allyScore = redTeam ? playerMatch.getMatch().getRedScore() : playerMatch.getMatch().getBlueScore();
        Integer enemyScore = redTeam ? playerMatch.getMatch().getBlueScore() : playerMatch.getMatch().getRedScore();
        return new MatchResponse(
            playerMatch.getId(),
            playerMatch.getMatch().getStartedAt(),
            playerMatch.getMatch().getMapName(),
            playerMatch.getMatch().getGameMode(),
            playerMatch.getAgentName(),
            playerMatch.getResult(),
            allyScore,
            enemyScore,
            playerMatch.getKills(),
            playerMatch.getDeaths(),
            playerMatch.getAssists(),
            kda,
            playerMatch.getAcs(),
            playerMatch.getAdr(),
            headshotPercentage,
            playerMatch.getCompetitiveTier(),
            playerMatch.getRankRating()
        );
    }

    private MatchResult parseResult(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return MatchResult.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("result must be WIN, LOSS or DRAW", exception);
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to 0");
        }
        if (size < 1 || size > MAXIMUM_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAXIMUM_PAGE_SIZE);
        }
    }
}
