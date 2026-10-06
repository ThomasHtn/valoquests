package io.github.thomashtn.valoquests.scoring.model;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What a set of players produced over a range of days, priced once for everything that reads a day.
 *
 * <p>A day, or a player within a day, absent from a reading played nothing. Played days cover the lookback,
 * so {@link #playedDaysUpTo(long, LocalDate)} also answers for the day before the range.
 */
public final class DailyOutput {

    /**
     * Output per day and per player, days inside the requested range only.
     */
    private final Map<LocalDate, Map<Long, PlayerDayOutput>> byDayAndPlayer;

    /**
     * Days played this week per player and per played day, lookback days included.
     */
    private final Map<Long, Map<LocalDate, Integer>> playedDaysByPlayerAndDay;

    /**
     * Every valued match inside the requested range, in chronological order.
     */
    private final List<ValuedMatch> valuedMatches;

    /**
     * Creates an immutable reading, copying every level of every map handed in.
     *
     * @param byDayAndPlayer           output per day and per player
     * @param playedDaysByPlayerAndDay days played this week per player and per played day
     * @param valuedMatches            every valued match of the range, chronological
     */
    public DailyOutput(
        Map<LocalDate, Map<Long, PlayerDayOutput>> byDayAndPlayer,
        Map<Long, Map<LocalDate, Integer>> playedDaysByPlayerAndDay,
        List<ValuedMatch> valuedMatches
    ) {
        this.byDayAndPlayer = copyNested(byDayAndPlayer);
        this.playedDaysByPlayerAndDay = copyNested(playedDaysByPlayerAndDay);
        this.valuedMatches = List.copyOf(valuedMatches);
    }

    /**
     * Returns what each player produced on one day.
     *
     * @param day day to read
     * @return output indexed by player identifier, players who did not play omitted
     */
    public Map<Long, PlayerDayOutput> on(LocalDate day) {
        return byDayAndPlayer.getOrDefault(day, Map.of());
    }

    /**
     * Returns what one player produced on one day.
     *
     * @param playerId internal player identifier
     * @param day      day to read
     * @return the day's output, {@link PlayerDayOutput#NONE} when the player did not play
     */
    public PlayerDayOutput of(long playerId, LocalDate day) {
        return on(day).getOrDefault(playerId, PlayerDayOutput.NONE);
    }

    /**
     * Returns the days of the week played up to a day, that day included.
     *
     * @param playerId internal player identifier
     * @param day      day to read
     * @return days played this week up to that day, zero when the player played nothing that day
     */
    public int playedDaysUpTo(long playerId, LocalDate day) {
        return playedDaysByPlayerAndDay.getOrDefault(playerId, Map.of()).getOrDefault(day, 0);
    }

    /**
     * Returns every valued match of the range, whoever played it, in chronological order.
     *
     * @return valued matches ordered by start instant then identifier
     */
    public List<ValuedMatch> valuedMatches() {
        return valuedMatches;
    }

    /**
     * Deep-copies one two-level map.
     *
     * @param source map to copy
     * @param <K>    outer key type
     * @param <I>    inner key type
     * @param <V>    value type
     * @return an unmodifiable copy, inner maps included
     */
    private static <K, I, V> Map<K, Map<I, V>> copyNested(Map<K, Map<I, V>> source) {
        Map<K, Map<I, V>> copied = new HashMap<>(source.size());
        source.forEach((key, inner) -> copied.put(key, Map.copyOf(inner)));

        return Map.copyOf(copied);
    }
}
