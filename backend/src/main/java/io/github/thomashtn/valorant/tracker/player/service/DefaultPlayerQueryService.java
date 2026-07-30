package io.github.thomashtn.valorant.tracker.player.service;

import io.github.thomashtn.valorant.tracker.match.entity.PlayerMatch;
import io.github.thomashtn.valorant.tracker.match.model.GameMode;
import io.github.thomashtn.valorant.tracker.match.model.MatchResult;
import io.github.thomashtn.valorant.tracker.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valorant.tracker.player.dto.AgentStatisticsResponse;
import io.github.thomashtn.valorant.tracker.player.dto.MapStatisticsResponse;
import io.github.thomashtn.valorant.tracker.player.dto.PlayerDetailsResponse;
import io.github.thomashtn.valorant.tracker.player.dto.PlayerSummaryResponse;
import io.github.thomashtn.valorant.tracker.player.entity.Player;
import io.github.thomashtn.valorant.tracker.player.exception.PlayerNotFoundException;
import io.github.thomashtn.valorant.tracker.player.repository.PlayerRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implements player list and profile consultation from persisted match data. */
@Service
@Transactional(readOnly = true)
public class DefaultPlayerQueryService implements PlayerQueryService {

    /**
     * Repository used to load tracked players.
     */
    private final PlayerRepository playerRepository;

    /**
     * Repository used to query persisted player matches.
     */
    private final PlayerMatchRepository playerMatchRepository;

    /**
     * Creates the persisted player query service.
     *
     * @param playerRepository repository used to load tracked players
     * @param playerMatchRepository repository used to query persisted player matches
     */
    public DefaultPlayerQueryService(
        PlayerRepository playerRepository,
        PlayerMatchRepository playerMatchRepository
    ) {
        this.playerRepository = playerRepository;
        this.playerMatchRepository = playerMatchRepository;
    }

    /**
     * Returns every tracked player with aggregate match statistics.
     *
     * @return tracked player summaries
     */
    @Override
    public List<PlayerSummaryResponse> findAll() {
        return playerRepository.findAll().stream()
            .map(player -> toSummary(
                player,
                playerMatchRepository.findAllByPlayerIdOrderByMatchStartedAtDesc(player.getId())
            ))
            .toList();
    }

    /**
     * Returns the detailed profile and aggregate statistics of one player.
     *
     * @param playerId internal player identifier
     * @param seasonId optional season identifier restricting the statistics; {@code null} for every season
     * @param gameMode optional game mode restricting the statistics; {@code null} for every mode
     * @return player details
     */
    @Override
    public PlayerDetailsResponse findById(long playerId, Long seasonId, String gameMode) {
        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new PlayerNotFoundException(playerId));
        List<PlayerMatch> matches = playerMatchRepository
            .findAllByPlayerIdAndSeasonAndGameMode(playerId, seasonId, parseGameMode(gameMode));
        Statistics statistics = Statistics.from(matches);

        return new PlayerDetailsResponse(
            player.getId(),
            riotId(player),
            player.getDisplayName(),
            player.getPortrait(),
            player.getCompetitiveTier(),
            player.getRankRating(),
            player.getLastSuccessfulSynchronizationAt(),
            statistics.toResponse(),
            groupBy(matches, PlayerMatch::getAgentName).entrySet().stream()
                .map(entry -> toAgentStatistics(entry.getKey(), entry.getValue()))
                .sorted((left, right) -> Long.compare(right.matchesPlayed(), left.matchesPlayed()))
                .toList(),
            groupBy(matches, match -> match.getMatch().getMapName()).entrySet().stream()
                .map(entry -> toMapStatistics(entry.getKey(), entry.getValue()))
                .sorted((left, right) -> Long.compare(right.matchesPlayed(), left.matchesPlayed()))
                .toList()
        );
    }

    private PlayerSummaryResponse toSummary(Player player, List<PlayerMatch> matches) {
        Statistics statistics = Statistics.from(matches);
        return new PlayerSummaryResponse(
            player.getId(),
            riotId(player),
            player.getDisplayName(),
            player.getPortrait(),
            player.getCompetitiveTier(),
            player.getRankRating(),
            statistics.kda(),
            statistics.winRate(),
            statistics.headshotPercentage(),
            statistics.matchesPlayed(),
            player.getStatus(),
            player.getLastSuccessfulSynchronizationAt()
        );
    }

    private AgentStatisticsResponse toAgentStatistics(String agentName, List<PlayerMatch> matches) {
        Statistics statistics = Statistics.from(matches);
        String agentId = matches.stream()
            .map(PlayerMatch::getAgentId)
            .filter(value -> value != null && !value.isBlank())
            .findFirst()
            .orElse(null);
        return new AgentStatisticsResponse(
            agentId,
            agentName,
            statistics.matchesPlayed(),
            statistics.wins(),
            statistics.losses(),
            statistics.winRate(),
            statistics.kda(),
            statistics.adr(),
            statistics.acs()
        );
    }

    private MapStatisticsResponse toMapStatistics(String mapName, List<PlayerMatch> matches) {
        Statistics statistics = Statistics.from(matches);
        String mapId = matches.stream().map(match -> match.getMatch().getMapId())
            .filter(value -> value != null && !value.isBlank()).findFirst().orElse(null);
        return new MapStatisticsResponse(
            mapId,
            mapName,
            statistics.matchesPlayed(),
            statistics.wins(),
            statistics.losses(),
            statistics.winRate(),
            statistics.kda(),
            statistics.adr(),
            statistics.acs()
        );
    }

    private <K> Map<K, List<PlayerMatch>> groupBy(
        List<PlayerMatch> matches,
        Function<PlayerMatch, K> classifier
    ) {
        return matches.stream().filter(match -> classifier.apply(match) != null)
            .collect(Collectors.groupingBy(classifier));
    }

    private String riotId(Player player) {
        return player.getGameName() + "#" + player.getTagLine();
    }

    private GameMode parseGameMode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return GameMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                "gameMode must be one of " + Arrays.toString(GameMode.values()),
                exception
            );
        }
    }

    private record Statistics(
        long matchesPlayed,
        long wins,
        long losses,
        long kills,
        long deaths,
        long assists,
        long mvps,
        BigDecimal kda,
        BigDecimal winRate,
        BigDecimal adr,
        BigDecimal acs,
        BigDecimal headshotPercentage
    ) {
        private static Statistics from(List<PlayerMatch> matches) {
            long wins = matches.stream().filter(match -> match.getResult() == MatchResult.WIN).count();
            long losses = matches.stream().filter(match -> match.getResult() == MatchResult.LOSS).count();
            long kills = matches.stream().mapToLong(PlayerMatch::getKills).sum();
            long deaths = matches.stream().mapToLong(PlayerMatch::getDeaths).sum();
            long assists = matches.stream().mapToLong(PlayerMatch::getAssists).sum();
            long mvps = matches.stream().filter(PlayerMatch::isMvp).count();
            long headshots = matches.stream().mapToLong(PlayerMatch::getHeadshots).sum();
            long shots = matches.stream().mapToLong(match ->
                match.getHeadshots() + match.getBodyshots() + match.getLegshots()
            ).sum();
            BigDecimal kda = divide(kills + assists, Math.max(1, deaths));
            BigDecimal winRate = percentage(wins, matches.size());
            BigDecimal adr = average(matches.stream().map(PlayerMatch::getAdr).toList());
            BigDecimal acs = average(matches.stream().map(PlayerMatch::getAcs).toList());
            BigDecimal headshotPercentage = percentage(headshots, shots);
            return new Statistics(
                matches.size(), wins, losses, kills, deaths, assists, mvps,
                kda, winRate, adr, acs, headshotPercentage
            );
        }

        private PlayerDetailsResponse.PlayerStatistics toResponse() {
            return new PlayerDetailsResponse.PlayerStatistics(
                kda, winRate, adr, acs, headshotPercentage,
                kills, deaths, assists, matchesPlayed, wins, losses, mvps
            );
        }

        private static BigDecimal average(List<BigDecimal> values) {
            List<BigDecimal> nonNull = values.stream().filter(value -> value != null).toList();
            if (nonNull.isEmpty()) {
                return BigDecimal.ZERO;
            }
            return nonNull.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(nonNull.size()), 2, RoundingMode.HALF_UP);
        }

        private static BigDecimal divide(long numerator, long denominator) {
            return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
        }

        private static BigDecimal percentage(long numerator, long denominator) {
            return denominator == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
        }
    }
}
