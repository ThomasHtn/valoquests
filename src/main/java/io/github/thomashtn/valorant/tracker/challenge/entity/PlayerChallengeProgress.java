package io.github.thomashtn.valorant.tracker.challenge.entity;

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
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Stores a player's calculated progress for one weekly challenge.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "player_challenge_progress",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_progress_player_weekly_challenge",
        columnNames = {"player_id", "weekly_challenge_id"}
    )
)
public class PlayerChallengeProgress extends AuditableEntity {

    /**
     * Internal database identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Player whose progress is represented.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    /**
     * Weekly challenge being evaluated.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "weekly_challenge_id", nullable = false)
    private WeeklyChallenge weeklyChallenge;

    /**
     * Current calculated metric value.
     */
    @Column(name = "current_value", nullable = false, precision = 14, scale = 4)
    private BigDecimal currentValue = BigDecimal.ZERO;

    /**
     * Target metric value copied from the selected challenge rule.
     */
    @Column(name = "target_value", nullable = false, precision = 14, scale = 4)
    private BigDecimal targetValue;

    /**
     * Whether the target has been reached.
     */
    @Column(nullable = false)
    private boolean completed;

    /**
     * First timestamp at which the challenge was completed.
     */
    @Column(name = "completed_at")
    private Instant completedAt;

    /**
     * Timestamp of the latest progress calculation.
     */
    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;
}
