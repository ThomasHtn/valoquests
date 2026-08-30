package io.github.thomashtn.valoquests.run.entity;

import io.github.thomashtn.valoquests.shared.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ten consecutive weeks, opened and closed by the weekly rollover.
 *
 * <p>The campaign's boundary, replacing the Valorant act. An act has no regular duration, so two
 * campaigns fought in two acts could never be compared; a run is exactly ten weekly rollovers, which
 * makes every run comparable to every other one by construction.
 *
 * <p>Ten rollovers rather than ten boss encounters: a week can go by without an encounter ever being
 * drawn, and counting encounters would make a run's length variable, reintroducing exactly the defect
 * of the act this replaces.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "run",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_run_number", columnNames = {"number"}),
        @UniqueConstraint(name = "uk_run_first_week_start", columnNames = {"first_week_start"})
    }
)
public class Run extends AuditableEntity {

    /**
     * Internal database identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Sequential run number, starting at one.
     */
    @Column(name = "number", nullable = false)
    private int number;

    /**
     * Monday identifying the run's first week.
     */
    @Column(name = "first_week_start", nullable = false)
    private LocalDate firstWeekStart;

    /**
     * Monday identifying the run's tenth and last week.
     *
     * <p>Derivable from {@link #firstWeekStart} and the ruleset's run length, but stored so a run's
     * span is readable from the row itself, and so rebalancing that length cannot retroactively move
     * the boundary of a run already under way.
     */
    @Column(name = "last_week_start", nullable = false)
    private LocalDate lastWeekStart;

    /**
     * Number of players the roster held active when the run opened.
     *
     * <p>Frozen here on purpose. It is the denominator of the colony's Energy gauge, and the
     * backoffice can activate, deactivate or archive a player at any time; reading it live would let
     * an archive rewrite the history of a run that has already been played.
     */
    @Column(name = "roster_size", nullable = false)
    private int rosterSize;

    /**
     * Instant the run was closed, or {@code null} while it is the run in progress.
     */
    @Column(name = "closed_at")
    private Instant closedAt;

    /**
     * Calendar day an operator stopped this run early, or {@code null} for a run that ran (or is
     * still running) its full course.
     *
     * <p>{@link #closedAt} alone cannot tell the two apart: it is set either way. This is only ever
     * set alongside it, and gives the replay a day to stop the score at — a stopped run never reaches
     * its own {@link #settlementDay()}, so replaying up to there would credit weeks that were never
     * played.
     */
    @Column(name = "stopped_on")
    private LocalDate stoppedOn;

    /**
     * Returns the settlement day, the run's seventy-first and final day.
     *
     * <p>The Monday opening the eleventh week. It credits the tenth week's materials and boss, applies
     * one last migration and carries the run's score. Without it the last week would be the only one to
     * bring nothing in.
     *
     * @return the run's settlement day
     */
    public LocalDate settlementDay() {
        return lastWeekStart.plusWeeks(1);
    }

    /**
     * Returns the last day this run's score is ever computed on: {@link #stoppedOn} for a run an
     * operator cut short, {@link #settlementDay()} otherwise.
     *
     * <p>What the replay and the history endpoint both read instead of {@link #settlementDay()}
     * directly, so a stopped run is frozen at the day it actually stopped rather than credited for
     * weeks it never played.
     *
     * @return the run's final day
     */
    public LocalDate finalDay() {
        return stoppedOn != null ? stoppedOn : settlementDay();
    }

    /**
     * Determines whether a calendar day falls inside this run.
     *
     * @param day day to place, must not be {@code null}
     * @return {@code true} when the day is between the first day and the settlement day, inclusive
     */
    public boolean covers(LocalDate day) {
        return !day.isBefore(firstWeekStart) && !day.isAfter(settlementDay());
    }

    /**
     * Places one week inside this run, counting from one.
     *
     * <p>The run's own position, which is what the campaign's difficulty ladder and the boss timeline
     * both read. Not clamped here: a caller handed a week outside the run gets a value outside the
     * range and decides for itself what that means.
     *
     * @param weekStart Monday identifying the week, must not be {@code null}
     * @return the week's one-based position in the run
     */
    public int weekIndexOf(LocalDate weekStart) {
        return (int) ChronoUnit.WEEKS.between(firstWeekStart, weekStart) + 1;
    }
}
