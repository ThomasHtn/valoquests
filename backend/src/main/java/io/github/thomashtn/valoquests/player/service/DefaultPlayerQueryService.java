package io.github.thomashtn.valoquests.player.service;

import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.repository.PlayerMatchHistoryCriteria;
import io.github.thomashtn.valoquests.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valoquests.match.service.SeasonQueryService;
import io.github.thomashtn.valoquests.player.dto.PlayerDetailsResponse;
import io.github.thomashtn.valoquests.player.dto.PlayerSummaryResponse;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.exception.PlayerNotFoundException;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.shared.exception.InvalidRequestException;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.Instant;
import java.time.LocalDate;
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
     * Calendar resolving a week's instant bounds.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Resolves the season currently in progress, used to scope the player list's statistics.
     */
    private final SeasonQueryService seasonQueryService;

    /**
     * Creates the persisted player query service.
     *
     * @param playerRepository repository used to load tracked players
     * @param playerMatchRepository repository used to query persisted player matches
     * @param weekCalendar calendar resolving a week's instant bounds
     * @param seasonQueryService resolves the season currently in progress
     */
    public DefaultPlayerQueryService(
        PlayerRepository playerRepository,
        PlayerMatchRepository playerMatchRepository,
        WeekCalendar weekCalendar,
        SeasonQueryService seasonQueryService
    ) {
        this.playerRepository = playerRepository;
        this.playerMatchRepository = playerMatchRepository;
        this.weekCalendar = weekCalendar;
        this.seasonQueryService = seasonQueryService;
    }

    /**
     * Returns every tracked player with aggregate match statistics scoped to the season currently in
     * progress and to competitive matches.
     *
     * <p>Archived players are left out: they were removed from the roster and only remain stored so
     * the finalized weeks naming them stay readable. They are still resolvable through
     * {@link #findById}, which is what keeps a link from such a week working.
     *
     * <p>Falls back to every competitive match on record when no season is known yet - an empty
     * database, before the first synchronization ever runs.
     *
     * @return tracked player summaries
     */
    @Override
    public List<PlayerSummaryResponse> findAll() {
        Long currentSeasonId = seasonQueryService.resolveCurrentSeasonId();
        PlayerMatchHistoryCriteria criteria = new PlayerMatchHistoryCriteria(
            currentSeasonId, null, null, null, GameMode.COMPETITIVE,
            PlayerMatchHistoryCriteria.UNBOUNDED_PERIOD_START,
            PlayerMatchHistoryCriteria.UNBOUNDED_PERIOD_END
        );
        return playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED).stream()
            .map(player -> toSummary(
                player,
                playerMatchRepository.findAllByPlayerIdAndSeasonAndGameMode(player.getId(), criteria)
            ))
            .toList();
    }

    /**
     * Returns the detailed profile and aggregate statistics of one player.
     *
     * @param playerId  internal player identifier
     * @param seasonId  optional season identifier restricting the statistics; {@code null} for every season
     * @param gameMode  optional game mode restricting the statistics; {@code null} for every mode
     * @param weekStart optional Monday restricting the statistics to that calendar week; {@code null}
     *     for every week
     * @return player details
     */
    @Override
    public PlayerDetailsResponse findById(long playerId, Long seasonId, String gameMode, LocalDate weekStart) {
        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new PlayerNotFoundException(playerId));
        Instant periodStart = weekStart == null
            ? PlayerMatchHistoryCriteria.UNBOUNDED_PERIOD_START : weekCalendar.startOf(weekStart);
        Instant periodEnd = weekStart == null
            ? PlayerMatchHistoryCriteria.UNBOUNDED_PERIOD_END : weekCalendar.endOf(weekStart);
        List<PlayerMatch> matches = playerMatchRepository.findAllByPlayerIdAndSeasonAndGameMode(
            playerId,
            new PlayerMatchHistoryCriteria(
                seasonId, null, null, null, parseGameMode(gameMode), periodStart, periodEnd
            )
        );
        MatchStatistics statistics = MatchStatistics.from(matches);

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
                .map(entry -> MatchStatistics.toAgentStatistics(entry.getKey(), entry.getValue()))
                .sorted((left, right) -> Long.compare(right.matchesPlayed(), left.matchesPlayed()))
                .toList(),
            groupBy(matches, match -> match.getMatch().getMapName()).entrySet().stream()
                .map(entry -> MatchStatistics.toMapStatistics(entry.getKey(), entry.getValue()))
                .sorted((left, right) -> Long.compare(right.matchesPlayed(), left.matchesPlayed()))
                .toList()
        );
    }

    private PlayerSummaryResponse toSummary(Player player, List<PlayerMatch> matches) {
        MatchStatistics statistics = MatchStatistics.from(matches);
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
            throw new InvalidRequestException(
                "gameMode must be one of " + Arrays.toString(GameMode.values()),
                exception
            );
        }
    }

}
