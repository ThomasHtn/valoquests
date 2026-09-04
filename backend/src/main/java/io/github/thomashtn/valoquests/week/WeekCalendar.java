package io.github.thomashtn.valoquests.week;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Owns the calendar every weekly calculation is anchored on.
 *
 * <p>A week runs from Monday 00:00 to the following Monday 00:00 in the configured zone, and is
 * identified throughout the application by that Monday's {@link LocalDate}.
 *
 * <p>This exists because the zone has to be one decision, made once. Challenge selection, challenge
 * progress, active-day counting, ranking and rollover all have to agree on where a week starts and
 * which day a match falls on; when each computed it separately, a single divergence would silently
 * move a Sunday-night match into the wrong week and change a ranking nobody could then explain.
 *
 * <p>Instants remain stored in UTC. Only their calendar interpretation uses this zone.
 *
 * <p>Final because the constructor validates its arguments: leaving the class extensible would let
 * a subclass observe a partially initialized instance.
 */
@Component
public final class WeekCalendar {

    /**
     * Day a week starts on.
     */
    private static final DayOfWeek FIRST_DAY_OF_WEEK = DayOfWeek.MONDAY;

    /**
     * Clock resolving the current instant.
     */
    private final Clock clock;

    /**
     * Zone every weekly boundary is expressed in.
     */
    private final ZoneId zone;

    /**
     * Creates the week calendar.
     *
     * @param clock application clock
     * @param zone  zone weekly boundaries are resolved in; must match the zone the rollover job is
     *     scheduled with, or a week would be frozen at an instant that is not its own boundary
     */
    public WeekCalendar(
        Clock clock,
        @Value("${app.scheduling.week-rollover-zone}") ZoneId zone
    ) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.zone = Objects.requireNonNull(zone, "zone must not be null");
    }

    /**
     * Returns the zone weekly boundaries are expressed in.
     *
     * @return configured week zone
     */
    public ZoneId zone() {
        return zone;
    }

    /**
     * Returns the Monday beginning the week currently in progress.
     *
     * @return current week identifier
     */
    public LocalDate currentWeekStart() {
        return weekStartOf(LocalDate.now(clock.withZone(zone)));
    }

    /**
     * Returns the calendar day currently in progress, in the calendar's own zone.
     *
     * <p>The day the daily diminishing returns are counted over, which is not the caller's day: a
     * player finishing a match at one in the morning is still on the previous day as far as the
     * ladder is concerned, and only this calendar knows where that boundary sits.
     *
     * @return current day
     */
    public LocalDate today() {
        return LocalDate.now(clock.withZone(zone));
    }

    /**
     * Returns the Monday beginning the week that contains a calendar day.
     *
     * @param day day to place, must not be {@code null}
     * @return the week identifier containing that day
     */
    public LocalDate weekStartOf(LocalDate day) {
        Objects.requireNonNull(day, "day must not be null");

        return day.with(TemporalAdjusters.previousOrSame(FIRST_DAY_OF_WEEK));
    }

    /**
     * Returns the Monday beginning the week that contains an instant.
     *
     * @param instant instant to place, must not be {@code null}
     * @return the week identifier containing that instant
     */
    public LocalDate weekStartOf(Instant instant) {
        return weekStartOf(dayOf(instant));
    }

    /**
     * Returns the calendar day an instant falls on.
     *
     * <p>This is what makes a match count towards one active day rather than another, so it has to
     * use the same zone as the week it is counted in.
     *
     * @param instant instant to place, must not be {@code null}
     * @return the local day containing that instant
     */
    public LocalDate dayOf(Instant instant) {
        Objects.requireNonNull(instant, "instant must not be null");

        return instant.atZone(zone).toLocalDate();
    }

    /**
     * Returns the inclusive instant a week begins at.
     *
     * @param weekStart Monday identifying the week, must not be {@code null}
     * @return first instant belonging to the week
     */
    public Instant startOf(LocalDate weekStart) {
        Objects.requireNonNull(weekStart, "weekStart must not be null");

        return weekStart.atStartOfDay(zone).toInstant();
    }

    /**
     * Returns the exclusive instant a week ends at.
     *
     * <p>Exclusive on purpose: it is the following week's start, so consecutive weeks tile the
     * timeline without a gap or an overlap that would drop or double-count a match.
     *
     * @param weekStart Monday identifying the week, must not be {@code null}
     * @return first instant no longer belonging to the week
     */
    public Instant endOf(LocalDate weekStart) {
        Objects.requireNonNull(weekStart, "weekStart must not be null");

        return weekStart.plusWeeks(1).atStartOfDay(zone).toInstant();
    }

    /**
     * Returns the inclusive instant a calendar day begins at.
     *
     * @param day day of the project's zone, must not be {@code null}
     * @return first instant belonging to the day
     */
    public Instant startOfDay(LocalDate day) {
        Objects.requireNonNull(day, "day must not be null");

        return day.atStartOfDay(zone).toInstant();
    }

    /**
     * Returns the exclusive instant a calendar day ends at, the following day's start.
     *
     * @param day day of the project's zone, must not be {@code null}
     * @return first instant no longer belonging to the day
     */
    public Instant endOfDay(LocalDate day) {
        Objects.requireNonNull(day, "day must not be null");

        return day.plusDays(1).atStartOfDay(zone).toInstant();
    }

    /**
     * Determines whether a date identifies a week, meaning it is a Monday.
     *
     * @param weekStart date to check, must not be {@code null}
     * @return {@code true} when the date is a valid week identifier
     */
    public boolean isWeekStart(LocalDate weekStart) {
        Objects.requireNonNull(weekStart, "weekStart must not be null");

        return weekStart.getDayOfWeek() == FIRST_DAY_OF_WEEK;
    }
}
