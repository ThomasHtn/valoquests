package io.github.thomashtn.valoquests.match.service;

import io.github.thomashtn.valoquests.match.dto.MatchResponse;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.MatchHistoryFilter;
import io.github.thomashtn.valoquests.match.model.MatchResult;
import io.github.thomashtn.valoquests.match.repository.PlayerMatchHistoryCriteria;
import io.github.thomashtn.valoquests.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valoquests.player.exception.PlayerNotFoundException;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.shared.dto.PageResponse;
import io.github.thomashtn.valoquests.shared.exception.InvalidRequestException;
import io.github.thomashtn.valoquests.shared.util.PaginationGuard;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements filtered and paginated player match-history consultation.
 */
@Service
@Transactional(readOnly = true)
public class DefaultMatchQueryService implements MatchQueryService {

    /**
     * Repository used to load tracked players.
     */
    private final PlayerRepository playerRepository;

    /**
     * Repository used to query persisted player matches.
     */
    private final PlayerMatchRepository playerMatchRepository;

    /**
     * Calendar resolving a week's instant bounds.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the persisted match query service.
     *
     * @param playerRepository      repository used to validate tracked players
     * @param playerMatchRepository repository used to query persisted player matches
     * @param weekCalendar          calendar resolving a week's instant bounds
     */
    public DefaultMatchQueryService(
        PlayerRepository playerRepository,
        PlayerMatchRepository playerMatchRepository,
        WeekCalendar weekCalendar
    ) {
        this.playerRepository = playerRepository;
        this.playerMatchRepository = playerMatchRepository;
        this.weekCalendar = weekCalendar;
    }

    /**
     * Returns one filtered page of matches for a tracked player.
     *
     * @param playerId internal player identifier
     * @param page     zero-based page index
     * @param size     requested page size
     * @param filter   optional season, map, agent, result and game mode filters
     * @return requested page of player matches
     */
    @Override
    public PageResponse<MatchResponse> findByPlayer(
        long playerId,
        int page,
        int size,
        MatchHistoryFilter filter
    ) {
        PaginationGuard.assertValidPageRequest(page, size);
        if (!playerRepository.existsById(playerId)) {
            throw new PlayerNotFoundException(playerId);
        }
        MatchResult parsedResult = parseResult(filter.result());
        GameMode parsedGameMode = parseGameMode(filter.gameMode());
        Instant periodStart = filter.weekStart() == null
            ? PlayerMatchHistoryCriteria.UNBOUNDED_PERIOD_START : weekCalendar.startOf(filter.weekStart());
        Instant periodEnd = filter.weekStart() == null
            ? PlayerMatchHistoryCriteria.UNBOUNDED_PERIOD_END : weekCalendar.endOf(filter.weekStart());
        PlayerMatchHistoryCriteria criteria = new PlayerMatchHistoryCriteria(
            filter.seasonId(),
            normalize(filter.map()),
            normalize(filter.agent()),
            parsedResult,
            parsedGameMode,
            periodStart,
            periodEnd
        );
        Page<PlayerMatch> matches = playerMatchRepository.findHistory(
            playerId,
            criteria,
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
            playerMatch.getCompetitiveTier()
        );
    }

    private MatchResult parseResult(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return MatchResult.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException("result must be WIN, LOSS or DRAW", exception);
        }
    }

    private GameMode parseGameMode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return GameMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException(
                "gameMode must be one of " + Arrays.toString(GameMode.values()),
                exception
            );
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
