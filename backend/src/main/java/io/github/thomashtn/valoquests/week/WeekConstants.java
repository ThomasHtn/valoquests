package io.github.thomashtn.valoquests.week;

/**
 * Sizes of the game week, Monday to Sunday.
 */
public final class WeekConstants {

    /**
     * Days in one game week.
     */
    public static final int DAYS_PER_WEEK = 7;

    /**
     * Offset from a week's Monday to its Sunday.
     */
    public static final int LAST_DAY_OFFSET = DAYS_PER_WEEK - 1;

    /**
     * Not instantiable: static helpers only.
     */
    private WeekConstants() {
    }
}
