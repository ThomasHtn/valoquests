package io.github.thomashtn.valorant.tracker.ranking.entity;

import io.github.thomashtn.valorant.tracker.player.entity.Player;
import io.github.thomashtn.valorant.tracker.shared.entity.AuditableEntity;
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
 * Stores one player's calculated score and position for a calendar week.
 *
 * <p>Current rows are recalculated from challenge progress. Finalized rows act as immutable
 * snapshots used by ranking history.</p>
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
     * Total points awarded for completed weekly challenges.
     */
    @Column(nullable = false)
    private int points;

    /**
     * Number of weekly challenges completed by the player.
     */
    @Column(name = "completed_challenges", nullable = false)
    private int completedChallenges;

    /**
     * Current or final one-based ranking position.
     */
    @Column(nullable = false)
    private int position;

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
}
