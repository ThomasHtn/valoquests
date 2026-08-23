package io.github.thomashtn.valoquests.player.service;

import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.entity.Season;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.MatchResult;
import io.github.thomashtn.valoquests.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valoquests.player.dto.AgentStatisticsResponse;
import io.github.thomashtn.valoquests.player.dto.MapStatisticsResponse;
import io.github.thomashtn.valoquests.player.dto.PlayerProgressionResponse;
import io.github.thomashtn.valoquests.player.exception.PlayerNotFoundException;
import io.github.thomashtn.valoquests.player.model.CompetitiveTier;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
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
     * Rounds a match must have lasted for its headshot rate to stand as a personal best.
     *
     * <p>A match abandoned after two rounds can show a headshot rate no full match will ever beat,
     * which would freeze that record forever on a game barely played.
     */
    private static final int MINIMUM_HEADSHOT_ROUNDS = 10;

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

        List<PlayerMatch> inScope = inSelectedSeasons(
            playerMatchRepository.findAllByPlayerIdOrderByMatchStartedAtDesc(playerId),
            seasonIds
        );
        List<PlayerMatch> competitive = inScope.stream()
            .filter(match -> match.getMatch().getGameMode() == GameMode.COMPETITIVE)
            .sorted(Comparator.comparing(match -> match.getMatch().getStartedAt()))
            .toList();

        return new PlayerProgressionResponse(
            evolution(competitive),
            aim(competitive),
            weekdays(competitive),
            hourSlots(competitive),
            records(competitive, inScope),
            mapStatistics(competitive),
            agentStatistics(competitive)
        );
    }

    /**
     * Narrows a history to the selected seasons, keeping every season when none is selected.
     *
     * @param matches   the player's whole stored history
     * @param seasonIds selected season identifiers, possibly {@code null} or empty
     * @return the matches falling inside the selection
     */
    private List<PlayerMatch> inSelectedSeasons(List<PlayerMatch> matches, List<Long> seasonIds) {
        if (seasonIds == null || seasonIds.isEmpty()) {
            return matches;
        }
        Set<Long> selected = Set.copyOf(seasonIds);
        return matches.stream()
            .filter(match -> selected.contains(match.getMatch().getSeason().getId()))
            .toList();
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
            headshotPercentage(match),
            matchKda(match),
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
     * Collects the player's personal bests.
     *
     * @param competitive competitive matches in scope, oldest first
     * @param everyMode   every match in scope regardless of game mode, used by the day streak
     * @return the personal records
     */
    private PlayerProgressionResponse.PersonalRecords records(
        List<PlayerMatch> competitive,
        List<PlayerMatch> everyMode
    ) {
        List<PlayerMatch> longEnough = competitive.stream()
            .filter(match -> match.getRoundsPlayed() >= MINIMUM_HEADSHOT_ROUNDS)
            .toList();

        return new PlayerProgressionResponse.PersonalRecords(
            best(competitive, match -> BigDecimal.valueOf(match.getKills())),
            best(competitive, PlayerMatch::getAcs),
            best(competitive, match -> BigDecimal.valueOf(match.getDamageDealt())),
            best(competitive, DefaultPlayerProgressionQueryService::matchKda),
            best(longEnough, DefaultPlayerProgressionQueryService::headshotPercentage),
            longestWinStreak(competitive),
            longestActiveDayStreak(everyMode),
            competitive.stream().filter(PlayerMatch::isMvp).count(),
            peakTier(competitive)
        );
    }

    /**
     * Finds the match holding the highest value of one metric.
     *
     * <p>Zero and negative values never qualify: a personal best of nothing is not a record, and
     * reporting one would fill the section with empty boasts on a freshly synchronized player.
     *
     * @param matches   matches to search
     * @param extractor reads the metric off one match, possibly returning {@code null}
     * @return the record, or {@code null} when no match qualifies
     */
    private static PlayerProgressionResponse.RecordEntry best(
        List<PlayerMatch> matches,
        Function<PlayerMatch, BigDecimal> extractor
    ) {
        PlayerMatch holder = null;
        BigDecimal record = null;
        for (PlayerMatch match : matches) {
            BigDecimal candidate = extractor.apply(match);
            if (candidate == null || candidate.signum() <= 0) {
                continue;
            }
            if (record == null || candidate.compareTo(record) > 0) {
                holder = match;
                record = candidate;
            }
        }
        if (holder == null) {
            return null;
        }
        return new PlayerProgressionResponse.RecordEntry(
            record,
            holder.getMatch().getStartedAt(),
            holder.getMatch().getMapName(),
            holder.getAgentName()
        );
    }

    /**
     * Measures the longest run of consecutive wins.
     *
     * <p>Remakes are skipped rather than counted as a loss: a game that never really happened
     * should not break a streak the player did earn.
     *
     * @param matches competitive matches in scope, oldest first
     * @return the longest run of wins, or zero when the player never won
     */
    private static int longestWinStreak(List<PlayerMatch> matches) {
        int longest = 0;
        int current = 0;
        for (PlayerMatch match : matches) {
            if (match.getResult() == MatchResult.REMAKE) {
                continue;
            }
            current = match.getResult() == MatchResult.WIN ? current + 1 : 0;
            longest = Math.max(longest, current);
        }
        return longest;
    }

    /**
     * Measures the longest run of consecutive calendar days holding at least one match.
     *
     * <p>Counted over every game mode, deliberately: this records showing up, not competing, so a
     * night of deathmatch keeps the run alive.
     *
     * @param matches every match in scope, in any order
     * @return the longest run of active days, or zero when the player played nothing
     */
    private int longestActiveDayStreak(List<PlayerMatch> matches) {
        List<LocalDate> days = matches.stream()
            .map(match -> zoned(match).toLocalDate())
            .distinct()
            .sorted()
            .toList();

        int longest = 0;
        int current = 0;
        LocalDate previous = null;
        for (LocalDate day : days) {
            boolean consecutive = previous != null && day.equals(previous.plusDays(1));
            current = consecutive ? current + 1 : 1;
            longest = Math.max(longest, current);
            previous = day;
        }
        return longest;
    }

    /**
     * Finds the highest competitive rank the player held during the filtered matches.
     *
     * @param matches competitive matches in scope
     * @return the peak tier, or {@code null} when no match carries a rank
     */
    private static CompetitiveTier peakTier(List<PlayerMatch> matches) {
        return matches.stream()
            .map(PlayerMatch::getCompetitiveTier)
            .filter(tier -> tier != null && tier != CompetitiveTier.UNRANKED)
            .max(Comparator.naturalOrder())
            .orElse(null);
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
     * Reads one match's headshot rate.
     *
     * @param match the match to measure
     * @return the share of that match's hits that landed on the head
     */
    private static BigDecimal headshotPercentage(PlayerMatch match) {
        long shots = (long) match.getHeadshots() + match.getBodyshots() + match.getLegshots();
        return MatchStatistics.percentage(match.getHeadshots(), shots);
    }

    /**
     * Reads one match's ratio of kills and assists to deaths.
     *
     * @param match the match to measure
     * @return that match's KDA ratio
     */
    private static BigDecimal matchKda(PlayerMatch match) {
        return MatchStatistics.divide(
            (long) match.getKills() + match.getAssists(),
            Math.max(1, match.getDeaths())
        );
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
