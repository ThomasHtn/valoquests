package io.github.thomashtn.valoquests.profile.service;

import io.github.thomashtn.valoquests.match.dto.MatchHistoryFilter;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valoquests.match.service.MatchFilterParser;
import io.github.thomashtn.valoquests.match.service.SeasonQueryService;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.exception.PlayerNotFoundException;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.profile.dto.PlayerDetailsResponse;
import io.github.thomashtn.valoquests.profile.dto.PlayerSummaryResponse;
import io.github.thomashtn.valoquests.scoring.model.DailyYield;
import io.github.thomashtn.valoquests.scoring.service.DailyOutputReader;
import io.github.thomashtn.valoquests.shared.time.WeekCalendar;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements player list and profile consultation from persisted match data.
 */
@Service
@Transactional(readOnly = true)
public class DefaultPlayerQueryService implements PlayerQueryService {

    /**
     * Order of the profile's matches, the one the match history uses.
     */
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "match.startedAt", "id");

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
     * Reports where today stands on the daily diminishing-returns ladder.
     */
    private final DailyOutputReader dailyOutputReader;

    /**
     * Creates the persisted player query service.
     *
     * @param playerRepository repository used to load tracked players
     * @param playerMatchRepository repository used to query persisted player matches
     * @param weekCalendar calendar resolving a week's instant bounds
     * @param seasonQueryService resolves the season currently in progress
     * @param dailyOutputReader reports where today stands on the diminishing-returns ladder
     */
    public DefaultPlayerQueryService(
        PlayerRepository playerRepository,
        PlayerMatchRepository playerMatchRepository,
        WeekCalendar weekCalendar,
        SeasonQueryService seasonQueryService,
        DailyOutputReader dailyOutputReader
    ) {
        this.playerRepository = playerRepository;
        this.playerMatchRepository = playerMatchRepository;
        this.weekCalendar = weekCalendar;
        this.seasonQueryService = seasonQueryService;
        this.dailyOutputReader = dailyOutputReader;
    }

    /**
     * Returns every tracked player with statistics for the current season's competitive matches.
     *
     * <p>Archived players are left out but stay resolvable through {@link #findProfile}. Without a known
     * season, every competitive match on record is used.
     *
     * @return tracked player summaries
     */
    @Override
    public List<PlayerSummaryResponse> findAll() {
        Map<Long, List<PlayerMatch>> matchesByPlayerId = playerMatchRepository
            .findAllBySeasonAndGameModeAndPlayerStatusNot(
                seasonQueryService.resolveCurrentSeasonId(),
                GameMode.COMPETITIVE,
                PlayerStatus.ARCHIVED
            )
            .stream()
            .collect(Collectors.groupingBy(match -> match.getPlayer().getId()));

        return playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED).stream()
            .map(player -> toSummary(player, matchesByPlayerId.getOrDefault(player.getId(), List.of())))
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
    public PlayerDetailsResponse findProfile(long playerId, Long seasonId, String gameMode, LocalDate weekStart) {
        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new PlayerNotFoundException(playerId));
        List<PlayerMatch> matches = playerMatchRepository.findHistory(
            playerId,
            MatchFilterParser.toCriteria(
                new MatchHistoryFilter(seasonId, null, null, null, gameMode, weekStart),
                weekCalendar
            ),
            Pageable.unpaged(NEWEST_FIRST)
        ).getContent();
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
            dailyYieldOf(playerId),
            MatchStatistics.perAgent(matches),
            MatchStatistics.perMap(matches)
        );
    }

    /**
     * Reports where a player stands on today's diminishing-returns ladder.
     *
     * <p>Read separately: the profile's matches are filtered, while the ladder counts every valued match
     * of the day.
     *
     * @param playerId internal player identifier
     * @return the day's standing
     */
    private PlayerDetailsResponse.DailyYield dailyYieldOf(long playerId) {
        DailyYield yield = dailyOutputReader.dailyYield(playerId, weekCalendar.today());

        return new PlayerDetailsResponse.DailyYield(
            yield.matchesToday(),
            yield.nextMatchPercent(),
            yield.dropsAtRank(),
            yield.dropsToPercent()
        );
    }

    /**
     * Maps one player with the statistics of their filtered matches.
     */
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

    /**
     * The full Riot ID, {@code gameName#tagLine}.
     */
    private String riotId(Player player) {
        return player.getGameName() + "#" + player.getTagLine();
    }
}
