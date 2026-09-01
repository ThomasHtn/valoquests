package io.github.thomashtn.valoquests.scoring.model;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * What the squad's matches were worth over a stretch of the calendar, read day by day.
 *
 * <p>Two readings of the same matches, and they are deliberately not the same number. The
 * <b>weighted</b> figures have been through the scoring ruleset's daily diminishing returns and are
 * what anything pricing output must use, so the colony and the ranking pay a given match to the unit.
 * The <b>raw</b> figures are read before those returns, because they answer a different question —
 * whether somebody turned up tonight — and a player stringing fifteen games together must not watch
 * their own turnout drop by playing more.
 *
 * <p>A day, or a player inside a day, absent from a reading played nothing.
 *
 * <p>A class rather than a record on purpose: the two per-player readings are indexed by day, and a
 * record would have to hand the whole nested map back on an accessor of its own. Read one day at a
 * time through {@link #weightedDamageOn(LocalDate)} and {@link #rawDamageOn(LocalDate)} instead —
 * which is how every caller uses them, and which keeps the inner maps off the public surface.
 */
public final class DailyMatchDamage {

    /**
     * Damage of the whole squad per day, after the daily diminishing returns.
     */
    private final Map<LocalDate, Integer> weightedDamageByDay;

    /**
     * Damage per day and per player, after the daily diminishing returns.
     */
    private final Map<LocalDate, Map<Long, Integer>> weightedDamageByDayAndPlayer;

    /**
     * Damage per day and per player, before the daily diminishing returns.
     */
    private final Map<LocalDate, Map<Long, Integer>> rawDamageByDayAndPlayer;

    /**
     * Creates an immutable reading, copying every level of every map handed in.
     *
     * @param weightedDamageByDay          damage of the whole squad per day, after diminishing returns
     * @param weightedDamageByDayAndPlayer damage per day and per player, after diminishing returns
     * @param rawDamageByDayAndPlayer      damage per day and per player, before diminishing returns
     */
    public DailyMatchDamage(
        Map<LocalDate, Integer> weightedDamageByDay,
        Map<LocalDate, Map<Long, Integer>> weightedDamageByDayAndPlayer,
        Map<LocalDate, Map<Long, Integer>> rawDamageByDayAndPlayer
    ) {
        this.weightedDamageByDay = Map.copyOf(weightedDamageByDay);
        this.weightedDamageByDayAndPlayer = copyNested(weightedDamageByDayAndPlayer);
        this.rawDamageByDayAndPlayer = copyNested(rawDamageByDayAndPlayer);
    }

    /**
     * Returns what the whole squad brought in each day, after the daily diminishing returns.
     *
     * @return weighted damage indexed by day, days nobody played omitted
     */
    public Map<LocalDate, Integer> weightedDamageByDay() {
        return weightedDamageByDay;
    }

    /**
     * Returns what each player brought to one day, after the daily diminishing returns.
     *
     * @param day day to read
     * @return weighted damage indexed by player identifier, players who did not play omitted
     */
    public Map<Long, Integer> weightedDamageOn(LocalDate day) {
        return weightedDamageByDayAndPlayer.getOrDefault(day, Map.of());
    }

    /**
     * Returns what each player brought to one day, before the daily diminishing returns.
     *
     * @param day day to read
     * @return raw damage indexed by player identifier, players who did not play omitted
     */
    public Map<Long, Integer> rawDamageOn(LocalDate day) {
        return rawDamageByDayAndPlayer.getOrDefault(day, Map.of());
    }

    /**
     * Deep-copies one day-indexed map of per-player figures.
     *
     * @param source map to copy
     * @return an unmodifiable copy, inner maps included
     */
    private static Map<LocalDate, Map<Long, Integer>> copyNested(
        Map<LocalDate, Map<Long, Integer>> source
    ) {
        Map<LocalDate, Map<Long, Integer>> copied = new HashMap<>(source.size());
        source.forEach((day, byPlayer) -> copied.put(day, Map.copyOf(byPlayer)));

        return Map.copyOf(copied);
    }
}
