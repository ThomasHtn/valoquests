package io.github.thomashtn.valoquests.profile.service;

import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valoquests.match.service.SeasonQueryService;
import io.github.thomashtn.valoquests.player.exception.PlayerNotFoundException;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.profile.dto.PlayerProgressionResponse;
import io.github.thomashtn.valoquests.shared.time.WeekCalendar;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements the progression analytics from persisted match data.
 *
 * <p>Loads the selected seasons once and narrows them in memory rather than issuing one query per
 * section, plus the whole career when a selection is set, for the consistency comparison with the
 * season before the selected one. The history of a tracked player is a few thousand rows at most -
 * this application follows a fixed group of seven - and a single load is what lets the day-streak
 * record span every game mode while every other figure stays scoped to competitive play.
 */
@Service
@Transactional(readOnly = true)
public class DefaultPlayerProgressionQueryService implements PlayerProgressionQueryService {

    /**
     * Repository used to confirm the requested player exists.
     */
    private final PlayerRepository playerRepository;

    /**
     * Repository used to load the player's stored matches.
     */
    private final PlayerMatchRepository playerMatchRepository;

    /**
     * Calendar owning the zone every weekday, time slot and calendar day is resolved in.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Resolves the season in progress, the one flagged active.
     */
    private final SeasonQueryService seasonQueryService;

    /**
     * Creates the progression query service.
     *
     * @param playerRepository      repository used to confirm the requested player exists
     * @param playerMatchRepository repository used to load the player's stored matches
     * @param weekCalendar          calendar owning the application's calendar zone
     * @param seasonQueryService    resolves the season in progress
     */
    public DefaultPlayerProgressionQueryService(
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
     * Returns one player's progression analytics.
     *
     * @param playerId  internal player identifier
     * @param seasonIds seasons to restrict the analytics to; empty or {@code null} for every season
     * @return the player's progression analytics
     */
    @Override
    public PlayerProgressionResponse findByPlayerId(long playerId, List<Long> seasonIds) {
        if (!playerRepository.existsById(playerId)) {
            throw new PlayerNotFoundException(playerId);
        }

        boolean everySeason = seasonIds == null || seasonIds.isEmpty();
        // The database narrows a selection: the discarded rows are the ones a career keeps adding.
        List<PlayerMatch> inScope = everySeason
            ? playerMatchRepository.findAllByPlayerIdOrderByMatchStartedAtDesc(playerId)
            : playerMatchRepository.findAllByPlayerIdAndMatchSeasonIdInOrderByMatchStartedAtDesc(
                playerId,
                Set.copyOf(seasonIds)
            );
        List<PlayerMatch> competitive = competitiveOldestFirst(inScope);
        // The previous-season comparison reads beyond the selection.
        List<PlayerMatch> career = everySeason
            ? competitive
            : competitiveOldestFirst(
                playerMatchRepository.findAllByPlayerIdOrderByMatchStartedAtDesc(playerId)
            );

        Long currentSeasonId = seasonQueryService.resolveCurrentSeasonId();

        return new PlayerProgressionResponse(
            EvolutionCalculator.evolution(competitive, currentSeasonId),
            EvolutionCalculator.aim(competitive),
            PlayScheduleCalculator.weekdays(competitive, weekCalendar.zone()),
            PlayScheduleCalculator.hourSlots(competitive, weekCalendar.zone()),
            PlayerRecordsCalculator.records(competitive, inScope, weekCalendar.zone()),
            MatchStatistics.perMap(competitive),
            MatchStatistics.perAgent(competitive),
            RankJourneyCalculator.journey(competitive, currentSeasonId),
            ConsistencyCalculator.consistency(competitive, career)
        );
    }

    /**
     * Keeps the competitive matches, oldest first.
     *
     * @param matches matches in any game mode and order
     * @return the competitive ones, oldest first
     */
    private static List<PlayerMatch> competitiveOldestFirst(List<PlayerMatch> matches) {
        return matches.stream()
            .filter(match -> match.getMatch().getGameMode() == GameMode.COMPETITIVE)
            .sorted(Comparator.comparing(match -> match.getMatch().getStartedAt()))
            .toList();
    }
}
