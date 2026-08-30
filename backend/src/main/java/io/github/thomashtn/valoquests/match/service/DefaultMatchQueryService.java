package io.github.thomashtn.valoquests.match.service;

import io.github.thomashtn.valoquests.match.dto.MatchDetailResponse;
import io.github.thomashtn.valoquests.match.dto.MatchResponse;
import io.github.thomashtn.valoquests.match.dto.MatchTeammateResponse;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.exception.MatchNotFoundException;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.MatchHistoryFilter;
import io.github.thomashtn.valoquests.match.model.MatchResult;
import io.github.thomashtn.valoquests.match.repository.PlayerMatchHistoryCriteria;
import io.github.thomashtn.valoquests.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valoquests.player.exception.PlayerNotFoundException;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.scoring.ScoringRuleset;
import io.github.thomashtn.valoquests.scoring.service.WeeklyMatchDamageResolver;
import io.github.thomashtn.valoquests.scoring.service.WeeklyMatchDamageResolver.MatchDamage;
import io.github.thomashtn.valoquests.shared.dto.PageResponse;
import io.github.thomashtn.valoquests.shared.exception.InvalidRequestException;
import io.github.thomashtn.valoquests.shared.util.PaginationGuard;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
     * Prices each match after the ruleset's daily diminishing returns, so the history can say what
     * a game was worth to the squad and not only how it went.
     */
    private final WeeklyMatchDamageResolver damageResolver;

    /**
     * The barème both pillars read, passed to the resolver.
     */
    private final ScoringRuleset ruleset;

    /**
     * Creates the persisted match query service.
     *
     * @param playerRepository      repository used to validate tracked players
     * @param playerMatchRepository repository used to query persisted player matches
     * @param weekCalendar          calendar resolving a week's instant bounds
     * @param damageResolver        resolver pricing each match after daily diminishing returns
     * @param ruleset               ruleset the damage is priced against
     */
    public DefaultMatchQueryService(
        PlayerRepository playerRepository,
        PlayerMatchRepository playerMatchRepository,
        WeekCalendar weekCalendar,
        WeeklyMatchDamageResolver damageResolver,
        ScoringRuleset ruleset
    ) {
        this.playerRepository = playerRepository;
        this.playerMatchRepository = playerMatchRepository;
        this.weekCalendar = weekCalendar;
        this.damageResolver = damageResolver;
        this.ruleset = ruleset;
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
        List<PlayerMatch> pageMatches = matches.getContent();
        Map<Long, MatchDamage> damageByPlayerMatchId = resolveDamage(playerId, pageMatches);
        return new PageResponse<>(
            pageMatches.stream()
                .map(playerMatch -> toResponse(playerMatch, damageByPlayerMatchId))
                .toList(),
            matches.getNumber(),
            matches.getSize(),
            matches.getTotalElements(),
            matches.getTotalPages()
        );
    }

    /**
     * Prices every match on the page, week by week.
     *
     * <p>A match's amount depends on how the rest of *that day* went — the ruleset pays a day's best
     * games in full and reduces the ones after them — so the page alone cannot price itself: a page
     * boundary routinely cuts a day in half, and the tail would then be ranked as if it were the
     * day's opening games. Each week the page touches is therefore reloaded whole and priced by the
     * same resolver the ranking and the boss read, which is what keeps the three from disagreeing.
     *
     * <p>Costs one extra query per week the page spans — one or two for a default page, and bounded
     * by the page size in the worst case.
     *
     * @param playerId    internal player identifier
     * @param pageMatches the matches the page is about to return
     * @return damage and coefficient indexed by player-match identifier
     */
    private Map<Long, MatchDamage> resolveDamage(long playerId, List<PlayerMatch> pageMatches) {
        Set<LocalDate> weekStarts = new LinkedHashSet<>();
        for (PlayerMatch playerMatch : pageMatches) {
            weekStarts.add(weekCalendar.weekStartOf(playerMatch.getMatch().getStartedAt()));
        }

        Map<Long, MatchDamage> damageByPlayerMatchId = new HashMap<>();
        for (LocalDate weekStart : weekStarts) {
            damageByPlayerMatchId.putAll(damageResolver.resolveDetailed(
                playerMatchRepository.findForChallengePeriod(
                    playerId,
                    weekCalendar.startOf(weekStart),
                    weekCalendar.endOf(weekStart)
                ),
                ruleset
            ));
        }

        return damageByPlayerMatchId;
    }

    private MatchResponse toResponse(
        PlayerMatch playerMatch,
        Map<Long, MatchDamage> damageByPlayerMatchId
    ) {
        MatchDamage damage =
            damageByPlayerMatchId.getOrDefault(playerMatch.getId(), MatchDamage.NONE);
        Integer allyScore = allyScore(playerMatch);
        Integer enemyScore = enemyScore(playerMatch);
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
            kdaOf(playerMatch),
            playerMatch.getAcs(),
            playerMatch.getAdr(),
            headshotPercentageOf(playerMatch),
            playerMatch.getCompetitiveTier(),
            damage.damage(),
            damage.coefficientPercent()
        );
    }

    /**
     * Loads full detail for one of a tracked player's matches, priced against the ruleset like every
     * other history entry and joined with every other tracked player found in the same match.
     *
     * @param playerId      internal player identifier
     * @param playerMatchId internal player-match identifier
     * @return the requested match's full detail
     */
    @Override
    public MatchDetailResponse findDetail(long playerId, long playerMatchId) {
        if (!playerRepository.existsById(playerId)) {
            throw new PlayerNotFoundException(playerId);
        }
        PlayerMatch playerMatch = playerMatchRepository
            .findByIdAndPlayerId(playerMatchId, playerId)
            .orElseThrow(() -> new MatchNotFoundException(playerMatchId));

        Map<Long, MatchDamage> damageByPlayerMatchId = resolveDamage(playerId, List.of(playerMatch));
        MatchDamage damage =
            damageByPlayerMatchId.getOrDefault(playerMatch.getId(), MatchDamage.NONE);

        List<MatchTeammateResponse> teammates = playerMatchRepository
            .findByMatchIdAndPlayerIdNot(playerMatch.getMatch().getId(), playerId)
            .stream()
            .map(other -> toTeammateResponse(playerMatch, other))
            .toList();

        return new MatchDetailResponse(
            playerMatch.getId(),
            playerMatch.getMatch().getStartedAt(),
            playerMatch.getMatch().getDurationSeconds(),
            playerMatch.getMatch().getMapName(),
            playerMatch.getMatch().getGameMode(),
            playerMatch.getAgentName(),
            playerMatch.getResult(),
            allyScore(playerMatch),
            enemyScore(playerMatch),
            playerMatch.getKills(),
            playerMatch.getDeaths(),
            playerMatch.getAssists(),
            kdaOf(playerMatch),
            playerMatch.getAcs(),
            playerMatch.getAdr(),
            playerMatch.getHeadshots(),
            playerMatch.getBodyshots(),
            playerMatch.getLegshots(),
            headshotPercentageOf(playerMatch),
            playerMatch.getDamageDealt(),
            playerMatch.getRoundsPlayed(),
            playerMatch.isMvp(),
            playerMatch.getCompetitiveTier(),
            damage.damage(),
            damage.coefficientPercent(),
            teammates
        );
    }

    private MatchTeammateResponse toTeammateResponse(PlayerMatch playerMatch, PlayerMatch other) {
        boolean sameTeam = playerMatch.getTeamId() != null
            && playerMatch.getTeamId().equalsIgnoreCase(other.getTeamId());
        return new MatchTeammateResponse(
            other.getPlayer().getId(),
            other.getPlayer().getDisplayName(),
            other.getPlayer().getPortrait(),
            other.getAgentName(),
            sameTeam,
            other.getResult(),
            other.getKills(),
            other.getDeaths(),
            other.getAssists(),
            other.getAcs()
        );
    }

    private BigDecimal kdaOf(PlayerMatch playerMatch) {
        return BigDecimal.valueOf(playerMatch.getKills() + playerMatch.getAssists())
            .divide(BigDecimal.valueOf(Math.max(1, playerMatch.getDeaths())), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal headshotPercentageOf(PlayerMatch playerMatch) {
        int shots = playerMatch.getHeadshots() + playerMatch.getBodyshots() + playerMatch.getLegshots();
        return shots == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(playerMatch.getHeadshots())
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(shots), 2, RoundingMode.HALF_UP);
    }

    private Integer allyScore(PlayerMatch playerMatch) {
        boolean redTeam = "Red".equalsIgnoreCase(playerMatch.getTeamId());
        return redTeam ? playerMatch.getMatch().getRedScore() : playerMatch.getMatch().getBlueScore();
    }

    private Integer enemyScore(PlayerMatch playerMatch) {
        boolean redTeam = "Red".equalsIgnoreCase(playerMatch.getTeamId());
        return redTeam ? playerMatch.getMatch().getBlueScore() : playerMatch.getMatch().getRedScore();
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
