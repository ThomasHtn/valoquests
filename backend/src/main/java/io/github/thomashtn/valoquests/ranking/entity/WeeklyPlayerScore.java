package io.github.thomashtn.valoquests.ranking.entity;

import io.github.thomashtn.valoquests.player.entity.Player;
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
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Stores one player's week: what their matches and challenges were worth, and where that put them.
 *
 * <p>Current rows are rebuilt from the stored matches and challenge progress after every import.
 * Finalized rows are immutable snapshots the ranking history and the weekly titles read.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "weekly_player_score",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_weekly_score_player_week",
        columnNames = {"player_id", "week_start"}
    )
)
public class WeeklyPlayerScore extends AuditableEntity {

    /**
     * Internal database identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Player represented by the weekly score.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    /**
     * Monday identifying the ranking week.
     */
    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    /**
     * Damage the player's valued matches dealt to the guardian this week, both multipliers applied.
     */
    @Column(name = "guardian_damage", nullable = false)
    private int guardianDamage;

    /**
     * Food share of that damage.
     */
    @Column(nullable = false)
    private int food;

    /**
     * Components share of that damage.
     */
    @Column(nullable = false)
    private int components;

    /**
     * Valued matches played this week.
     */
    @Column(name = "match_count", nullable = false)
    private int matchCount;

    /**
     * Number of distinct days with at least one valued match this week.
     */
    @Column(name = "active_days", nullable = false)
    private int activeDays;

    /**
     * Longest run of consecutive played days reached during the week, days before it included.
     */
    @Column(name = "streak_days", nullable = false)
    private int streakDays;

    /**
     * Points the player's validated challenges added, priced at the reference in force.
     */
    @Column(name = "challenge_points", nullable = false)
    private int challengePoints;

    /**
     * Number of weekly challenges validated by the player.
     */
    @Column(name = "completed_challenges", nullable = false)
    private int completedChallenges;

    /**
     * Number of daily challenges validated by the player this week.
     */
    @Column(name = "completed_daily_challenges", nullable = false)
    private int completedDailyChallenges;

    /**
     * {@link #guardianDamage} + {@link #challengePoints}: the individual ranking key.
     */
    @Column(name = "total_points", nullable = false)
    private int totalPoints;

    /**
     * Current or final one-based ranking position, {@code null} when the player is not
     * competitive and therefore never occupies a ranking slot.
     */
    @Column
    private Integer position;

    /**
     * Position stored before the latest recalculation.
     */
    @Column(name = "previous_position")
    private Integer previousPosition;

    /**
     * Timestamp of the latest ranking calculation.
     */
    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    /**
     * Timestamp at which the weekly score became immutable.
     */
    @Column(name = "finalized_at")
    private Instant finalizedAt;

    /**
     * Returns every challenge the player validated this week, daily and weekly together.
     *
     * @return validated challenge count
     */
    public int completedAllChallenges() {
        return completedChallenges + completedDailyChallenges;
    }
}
