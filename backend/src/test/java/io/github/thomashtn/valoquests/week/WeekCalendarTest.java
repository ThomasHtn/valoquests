package io.github.thomashtn.valoquests.week;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies the calendar every weekly calculation is anchored on.
 */
@DisplayName("WeekCalendar")
class WeekCalendarTest {

    /**
     * Zone used to prove the calendar honours a configured offset rather than assuming UTC.
     */
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    /**
     * Wednesday 2026-07-15, comfortably inside a week.
     */
    private static final Instant MIDWEEK = Instant.parse("2026-07-15T12:00:00Z");

    /**
     * Builds a calendar pinned to one instant and zone.
     */
    private WeekCalendar calendarAt(Instant instant, ZoneId zone) {
        return new WeekCalendar(Clock.fixed(instant, zone), zone);
    }

    @Test
    @DisplayName("resolves the Monday of the week in progress")
    void shouldResolveTheMondayOfTheWeekInProgress() {
        WeekCalendar calendar = calendarAt(MIDWEEK, ZoneOffset.UTC);

        assertThat(calendar.currentWeekStart())
            .isEqualTo(LocalDate.of(2026, 7, 13));
    }

    @Test
    @DisplayName("keeps a Monday as its own week start")
    void shouldKeepAMondayAsItsOwnWeekStart() {
        WeekCalendar calendar = calendarAt(MIDWEEK, ZoneOffset.UTC);
        LocalDate monday = LocalDate.of(2026, 7, 13);

        assertThat(calendar.weekStartOf(monday)).isEqualTo(monday);
        assertThat(calendar.isWeekStart(monday)).isTrue();
        assertThat(calendar.isWeekStart(monday.plusDays(1))).isFalse();
    }

    @Test
    @DisplayName("tiles consecutive weeks without a gap or an overlap")
    void shouldTileConsecutiveWeeksWithoutGapOrOverlap() {
        WeekCalendar calendar = calendarAt(MIDWEEK, ZoneOffset.UTC);
        LocalDate week = LocalDate.of(2026, 7, 13);

        assertThat(calendar.endOf(week)).isEqualTo(calendar.startOf(week.plusWeeks(1)));
        assertThat(calendar.startOf(week)).isEqualTo(Instant.parse("2026-07-13T00:00:00Z"));
        assertThat(calendar.endOf(week)).isEqualTo(Instant.parse("2026-07-20T00:00:00Z"));
    }

    @Test
    @DisplayName("places a late Sunday match in the week the configured zone puts it in")
    void shouldPlaceALateSundayMatchAccordingToTheConfiguredZone() {
        // 2026-07-19 is a Sunday. At 23:30 in Paris it is already 21:30 UTC the same day, but a
        // match played at 23:30 UTC is Monday 01:30 in Paris and belongs to the following week.
        Instant sundayNight = Instant.parse("2026-07-19T23:30:00Z");

        WeekCalendar utc = calendarAt(MIDWEEK, ZoneOffset.UTC);
        WeekCalendar paris = calendarAt(MIDWEEK, PARIS);

        assertThat(utc.dayOf(sundayNight)).isEqualTo(LocalDate.of(2026, 7, 19));
        assertThat(utc.weekStartOf(sundayNight)).isEqualTo(LocalDate.of(2026, 7, 13));

        assertThat(paris.dayOf(sundayNight)).isEqualTo(LocalDate.of(2026, 7, 20));
        assertThat(paris.weekStartOf(sundayNight)).isEqualTo(LocalDate.of(2026, 7, 20));
    }

    @Test
    @DisplayName("anchors week bounds on the configured zone, not on UTC")
    void shouldAnchorWeekBoundsOnTheConfiguredZone() {
        WeekCalendar paris = calendarAt(MIDWEEK, PARIS);
        LocalDate week = LocalDate.of(2026, 7, 13);

        // Paris is UTC+2 in July, so the week opens two hours before midnight UTC.
        assertThat(paris.startOf(week)).isEqualTo(Instant.parse("2026-07-12T22:00:00Z"));
        assertThat(paris.endOf(week)).isEqualTo(Instant.parse("2026-07-19T22:00:00Z"));
    }

    @Test
    @DisplayName("rejects a missing argument instead of resolving a wrong week")
    void shouldRejectMissingArguments() {
        WeekCalendar calendar = calendarAt(MIDWEEK, ZoneOffset.UTC);

        assertThatThrownBy(() -> calendar.dayOf(null))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> calendar.startOf(null))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> calendar.endOf(null))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> calendar.weekStartOf((LocalDate) null))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> calendar.isWeekStart(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("exposes the zone it was configured with")
    void shouldExposeItsZone() {
        assertThat(calendarAt(MIDWEEK, PARIS).zone()).isEqualTo(PARIS);
    }
}
