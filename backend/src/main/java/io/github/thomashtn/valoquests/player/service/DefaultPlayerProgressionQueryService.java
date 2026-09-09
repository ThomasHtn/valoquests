package io.github.thomashtn.valoquests.player.service;

import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.entity.Season;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valoquests.player.dto.AgentStatisticsResponse;
import io.github.thomashtn.valoquests.player.dto.MapStatisticsResponse;
import io.github.thomashtn.valoquests.player.dto.PlayerProgressionResponse;
import io.github.thomashtn.valoquests.player.exception.PlayerNotFoundException;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements the progression analytics from persisted match data.
 *
 * <p>Loads a player's whole stored history once and narrows it in memory rather than issuing one
 * query per section. The history of a tracked player is a few thousand rows at most - this
 * application follows a fixed group of seven - and a single load is what lets the day-streak
 * record span every game mode while every other figure stays scoped to competitive play.
 */
@Service
@Transactional(readOnly = true)
public class DefaultPlayerProgressionQueryService implements PlayerProgressionQueryService {

    /**
     * Matches a weekday or time slot must hold before it can be called a player's best.
     *
     * <p>Without a floor, the strongest slot would almost always be one the player barely played:
     * a single win on a Tuesday morning reads as a 100% win rate and would outrank a hundred
     * evening matches at 58%.
     */
    private static final int MINIMUM_SLOT_SAMPLE = 5;

    /**
     * Width, in hours, of one time slot on the schedule chart.
     */
    private static final int HOUR_SLOT_SPAN = 3;

    /**
     * Number of time slots covering a day.
     */
    private static final int HOUR_SLOT_COUNT = 24 / HOUR_SLOT_SPAN;

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
     * Creates the progression query service.
     *
     * @param playerRepository      repository used to confirm the requested player exists
     * @param playerMatchRepository repository used to load the player's stored matches
     * @param weekCalendar          calendar owning the application's calendar zone
     */
    public DefaultPlayerProgressionQueryService(
        PlayerRepository playerRepository,
        PlayerMatchRepository playerMatchRepository,
        WeekCalendar weekCalendar
    ) {
        this.playerRepository = playerRepository;
        this.playerMatchRepository = playerMatchRepository;
        this.weekCalendar = weekCalendar;
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

        List<PlayerMatch> inScope = findInSelectedSeasons(playerId, seasonIds);
        List<PlayerMatch> competitive = inScope.stream()
            .filter(match -> match.getMatch().getGameMode() == GameMode.COMPETITIVE)
            .sorted(Comparator.comparing(match -> match.getMatch().getStartedAt()))
            .toList();

        return new PlayerProgressionResponse(
            evolution(competitive),
            aim(competitive),
            weekdays(competitive),
            hourSlots(competitive),
            PlayerRecordsCalculator.records(competitive, inScope, weekCalendar.zone()),
            mapStatistics(competitive),
            agentStatistics(competitive)
        );
    }

    /**
     * Loads the player's history restricted to the selected seasons.
     *
     * <p>The restriction is handed to the database rather than applied to a full history in
     * memory: these analytics cover a whole career, so the discarded rows are exactly the ones
     * that keep growing. An empty or absent selection means "every season" and is the one case
     * that still reads the entire history, because that is what the caller asked for.
     *
     * @param playerId  internal player identifier
     * @param seasonIds selected season identifiers, possibly {@code null} or empty
     * @return the matches falling inside the selection, most recent first
     */
    private List<PlayerMatch> findInSelectedSeasons(long playerId, List<Long> seasonIds) {
        if (seasonIds == null || seasonIds.isEmpty()) {
            return playerMatchRepository.findAllByPlayerIdOrderByMatchStartedAtDesc(playerId);
        }
        return playerMatchRepository
            .findAllByPlayerIdAndMatchSeasonIdInOrderByMatchStartedAtDesc(
                playerId,
                Set.copyOf(seasonIds)
            );
    }

    /**
     * Builds one match-by-match series per season, oldest season first.
     *
     * @param matches competitive matches in scope, oldest first
     * @return one entry per season the player actually played in scope
     */
    private List<PlayerProgressionResponse.SeasonEvolution> evolution(List<PlayerMatch> matches) {
        return matches.stream()
            .collect(Collectors.groupingBy(
                match -> match.getMatch().getSeason().getId(),
                LinkedHashMap::new,
                Collectors.toList()
            ))
            .values().stream()
            .map(this::toSeasonEvolution)
            .toList();
    }

    /**
     * Turns one season's matches into its plotted series and its legend averages.
     *
     * <p>The averages are the season's aggregate indicators, not the mean of the plotted points:
     * that is the same definition the profile's summary tiles use, and the two must agree.
     *
     * @param matches one season's competitive matches, oldest first
     * @return the season's evolution entry
     */
    private PlayerProgressionResponse.SeasonEvolution toSeasonEvolution(List<PlayerMatch> matches) {
        Season season = matches.get(0).getMatch().getSeason();
        MatchStatistics statistics = MatchStatistics.from(matches);
        return new PlayerProgressionResponse.SeasonEvolution(
            season.getId(),
            season.getName(),
            season.isActive(),
            matches.stream().map(this::toMatchPoint).toList(),
            new PlayerProgressionResponse.Averages(
                statistics.headshotPercentage(),
                statistics.kda(),
                statistics.acs(),
                statistics.adr()
            )
        );
    }

    /**
     * Turns one match into a point on the evolution charts.
     *
     * @param match the match to plot
     * @return that match's point
     */
    private PlayerProgressionResponse.MatchPoint toMatchPoint(PlayerMatch match) {
        return new PlayerProgressionResponse.MatchPoint(
            match.getMatch().getStartedAt(),
            PlayerRecordsCalculator.headshotPercentage(match),
            PlayerRecordsCalculator.matchKda(match),
            match.getAcs(),
            match.getAdr()
        );
    }

    /**
     * Sums where the player's hits landed over the whole filtered set.
     *
     * @param matches competitive matches in scope
     * @return the aim breakdown
     */
    private PlayerProgressionResponse.AimBreakdown aim(List<PlayerMatch> matches) {
        long head = matches.stream().mapToLong(PlayerMatch::getHeadshots).sum();
        long body = matches.stream().mapToLong(PlayerMatch::getBodyshots).sum();
        long leg = matches.stream().mapToLong(PlayerMatch::getLegshots).sum();
        long total = head + body + leg;
        return new PlayerProgressionResponse.AimBreakdown(
            MatchStatistics.percentage(head, total),
            MatchStatistics.percentage(body, total),
            MatchStatistics.percentage(leg, total),
            total
        );
    }

    /**
     * Summarizes performance per day of the week, Monday first.
     *
     * @param matches competitive matches in scope
     * @return seven entries, one per weekday, whether or not the player played that day
     */
    private List<PlayerProgressionResponse.WeekdayPerformance> weekdays(List<PlayerMatch> matches) {
        Map<DayOfWeek, List<PlayerMatch>> byDay = matches.stream()
            .collect(Collectors.groupingBy(match -> zoned(match).getDayOfWeek()));
        List<MatchStatistics> slots = Arrays.stream(DayOfWeek.values())
            .map(day -> MatchStatistics.from(byDay.getOrDefault(day, List.of())))
            .toList();
        int best = bestSlotIndex(slots);

        return IntStream.range(0, slots.size())
            .mapToObj(index -> new PlayerProgressionResponse.WeekdayPerformance(
                DayOfWeek.of(index + 1),
                slots.get(index).matchesPlayed(),
                slots.get(index).wins(),
                slots.get(index).winRate(),
                index == best
            ))
            .toList();
    }

    /**
     * Summarizes performance per three-hour slot, starting at midnight.
     *
     * @param matches competitive matches in scope
     * @return eight entries, one per slot, whether or not the player played then
     */
    private List<PlayerProgressionResponse.HourSlotPerformance> hourSlots(List<PlayerMatch> matches) {
        Map<Integer, List<PlayerMatch>> bySlot = matches.stream()
            .collect(Collectors.groupingBy(match -> zoned(match).getHour() / HOUR_SLOT_SPAN));
        List<MatchStatistics> slots = IntStream.range(0, HOUR_SLOT_COUNT)
            .mapToObj(index -> MatchStatistics.from(bySlot.getOrDefault(index, List.of())))
            .toList();
        int best = bestSlotIndex(slots);

        return IntStream.range(0, slots.size())
            .mapToObj(index -> new PlayerProgressionResponse.HourSlotPerformance(
                index * HOUR_SLOT_SPAN,
                slots.get(index).matchesPlayed(),
                slots.get(index).wins(),
                slots.get(index).winRate(),
                index == best
            ))
            .toList();
    }

    /**
     * Picks the slot with the best win rate among those holding enough matches.
     *
     * @param slots slot statistics, in display order
     * @return the winning slot's index, or {@code -1} when no slot clears {@link #MINIMUM_SLOT_SAMPLE}
     */
    private static int bestSlotIndex(List<MatchStatistics> slots) {
        int best = -1;
        for (int index = 0; index < slots.size(); index++) {
            MatchStatistics slot = slots.get(index);
            if (slot.matchesPlayed() < MINIMUM_SLOT_SAMPLE) {
                continue;
            }
            if (best < 0 || slot.winRate().compareTo(slots.get(best).winRate()) > 0) {
                best = index;
            }
        }
        return best;
    }

    /**
     * Aggregates the filtered matches per map, most-played first.
     *
     * @param matches competitive matches in scope
     * @return per-map statistics
     */
    private List<MapStatisticsResponse> mapStatistics(List<PlayerMatch> matches) {
        return groupBy(matches, match -> match.getMatch().getMapName()).entrySet().stream()
            .map(entry -> MatchStatistics.toMapStatistics(entry.getKey(), entry.getValue()))
            .sorted((left, right) -> Long.compare(right.matchesPlayed(), left.matchesPlayed()))
            .toList();
    }

    /**
     * Aggregates the filtered matches per agent, most-played first.
     *
     * @param matches competitive matches in scope
     * @return per-agent statistics
     */
    private List<AgentStatisticsResponse> agentStatistics(List<PlayerMatch> matches) {
        return groupBy(matches, PlayerMatch::getAgentName).entrySet().stream()
            .map(entry -> MatchStatistics.toAgentStatistics(entry.getKey(), entry.getValue()))
            .sorted((left, right) -> Long.compare(right.matchesPlayed(), left.matchesPlayed()))
            .toList();
    }

    /**
     * Groups matches by a key, dropping the ones the key cannot be read from.
     *
     * @param matches    matches to group
     * @param classifier reads the grouping key off one match
     * @param <K>        grouping key type
     * @return the grouped matches
     */
    private static <K> Map<K, List<PlayerMatch>> groupBy(
        List<PlayerMatch> matches,
        Function<PlayerMatch, K> classifier
    ) {
        return matches.stream()
            .filter(match -> classifier.apply(match) != null)
            .collect(Collectors.groupingBy(classifier, LinkedHashMap::new, Collectors.toList()));
    }

    /**
     * Reads a match's start time in the application's calendar zone.
     *
     * @param match the match to place on the calendar
     * @return the match's start time, zoned
     */
    private ZonedDateTime zoned(PlayerMatch match) {
        return match.getMatch().getStartedAt().atZone(weekCalendar.zone());
    }
}
