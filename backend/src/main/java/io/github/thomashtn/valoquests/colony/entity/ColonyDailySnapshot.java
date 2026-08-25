package io.github.thomashtn.valoquests.colony.entity;

import io.github.thomashtn.valoquests.run.entity.Run;
import io.github.thomashtn.valoquests.shared.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The colony on one day of one run.
 *
 * <p>The only table the colony needs. Its current state is the latest snapshot of the run in progress,
 * and its erected buildings are a pure function of its materials, so there is nothing else to persist.
 *
 * <p>Rows are never updated in place by the engine: a replay deletes the run's snapshots and writes
 * them again. Two consecutive replays therefore produce identical rows, which is what makes both the
 * daily tick and the admin recompute free of any double-application risk.
 *
 * <p>{@code foodGain}, {@code energyGain} and {@code activePlayerCount} are not needed to rebuild the
 * state — the replay recomputes them from the same inputs — but they are what a recalibration would
 * have to look at, and keeping them means doing it without asking Henrik for anything again.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "colony_daily_snapshot",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_colony_daily_snapshot_run_day",
        columnNames = {"run_id", "day"}
    )
)
public class ColonyDailySnapshot extends AuditableEntity {

    /**
     * Internal database identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Run this day belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private Run run;

    /**
     * Calendar day, in the week calendar's zone.
     */
    @Column(name = "day", nullable = false)
    private LocalDate day;

    /**
     * Food gauge at the end of the day, in {@code [0, 100]}.
     */
    @Column(name = "food", nullable = false, precision = 9, scale = 3)
    private BigDecimal food;

    /**
     * Energy gauge at the end of the day, in {@code [0, 100]}.
     */
    @Column(name = "energy", nullable = false, precision = 9, scale = 3)
    private BigDecimal energy;

    /**
     * Cumulative materials, which never go back down.
     */
    @Column(name = "materials", nullable = false)
    private int materials;

    /**
     * Population at the end of the day, in {@code [0, capacity]}.
     */
    @Column(name = "population", nullable = false, precision = 11, scale = 3)
    private BigDecimal population;

    /**
     * Capacity the cumulative materials unlock.
     */
    @Column(name = "capacity", nullable = false)
    private int capacity;

    /**
     * Distinct players who played at least one eligible match that day.
     */
    @Column(name = "active_player_count", nullable = false)
    private int activePlayerCount;

    /**
     * Food gained from the day's match damage.
     */
    @Column(name = "food_gain", nullable = false, precision = 9, scale = 3)
    private BigDecimal foodGain;

    /**
     * Energy gained from the day's turnout.
     */
    @Column(name = "energy_gain", nullable = false, precision = 9, scale = 3)
    private BigDecimal energyGain;
}
