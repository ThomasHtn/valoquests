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
 * and the tier its town wears is a pure function of its housing, so there is nothing else to persist.
 *
 * <p>Rows are never updated in place by the engine: a replay deletes the run's snapshots and writes
 * them again. Two consecutive replays therefore produce identical rows, which is what makes both the
 * daily tick and the admin recompute free of any double-application risk.
 *
 * <p>{@code foodHarvest}, {@code matchDamage} and {@code presenceCount} are not needed to rebuild the
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
     * Food of the last seven days, the whole of what the town has to eat.
     */
    @Column(name = "food_stock", nullable = false, precision = 11, scale = 3)
    private BigDecimal foodStock;

    /**
     * What this day alone brought in, turnout multiplier included.
     */
    @Column(name = "food_harvest", nullable = false, precision = 11, scale = 3)
    private BigDecimal foodHarvest;

    /**
     * Match damage of the day, after the daily diminishing returns.
     */
    @Column(name = "match_damage", nullable = false)
    private int matchDamage;

    /**
     * Players whose raw damage that day cleared the turnout threshold.
     */
    @Column(name = "presence_count", nullable = false)
    private int presenceCount;

    /**
     * Morale the day ends on.
     */
    @Column(name = "morale", nullable = false, precision = 6, scale = 2)
    private BigDecimal morale;

    /**
     * Cumulative materials, which never go back down.
     */
    @Column(name = "materials", nullable = false)
    private int materials;

    /**
     * Inhabitants one point of food feeds, raised by the materials gathered.
     */
    @Column(name = "efficiency", nullable = false, precision = 6, scale = 3)
    private BigDecimal efficiency;

    /**
     * Population at the end of the day.
     */
    @Column(name = "population", nullable = false, precision = 11, scale = 3)
    private BigDecimal population;

    /**
     * What the night moved, negative when the town lost people.
     */
    @Column(name = "population_change", nullable = false, precision = 11, scale = 3)
    private BigDecimal populationChange;
}
