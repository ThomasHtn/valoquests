package io.github.thomashtn.valoquests.profile.service;

import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.profile.dto.PlayerProgressionResponse;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Computes how a player performs per weekday and per time of day, and which slot suits them best.
 *
 * <p>Pure: takes the matches and the calendar zone, touches no repository.</p>
 */
final class PlayScheduleCalculator {

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
     * Not instantiable: static helpers only.
     */
    private PlayScheduleCalculator() {
    }

    /**
     * Summarizes performance per day of the week, Monday first.
     *
     * @param matches competitive matches in scope
     * @param zone    calendar zone the weekday is read in
     * @return seven entries, one per weekday, whether or not the player played that day
     */
    static List<PlayerProgressionResponse.WeekdayPerformance> weekdays(List<PlayerMatch> matches, ZoneId zone) {
        Map<DayOfWeek, List<PlayerMatch>> byDay = matches.stream()
            .collect(Collectors.groupingBy(match -> zoned(match, zone).getDayOfWeek()));
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
     * @param zone    calendar zone the hour is read in
     * @return eight entries, one per slot, whether or not the player played then
     */
    static List<PlayerProgressionResponse.HourSlotPerformance> hourSlots(List<PlayerMatch> matches, ZoneId zone) {
        Map<Integer, List<PlayerMatch>> bySlot = matches.stream()
            .collect(Collectors.groupingBy(match -> zoned(match, zone).getHour() / HOUR_SLOT_SPAN));
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
     * Reads a match's start time in the application's calendar zone.
     *
     * @param match the match to place on the calendar
     * @param zone  the application's calendar zone
     * @return the match's start time, zoned
     */
    private static ZonedDateTime zoned(PlayerMatch match, ZoneId zone) {
        return match.getMatch().getStartedAt().atZone(zone);
    }
}
