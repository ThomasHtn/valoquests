package io.github.thomashtn.valorant.tracker.challenge.entity;

import io.github.thomashtn.valorant.tracker.player.entity.Player;
import io.github.thomashtn.valorant.tracker.shared.entity.AuditableEntity;
import jakarta.persistence.*;
import java.math.*;
import java.time.*;
import lombok.*;

/**
 * Represents the player challenge progress component.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "player_challenge_progress", uniqueConstraints = @UniqueConstraint(name = "uk_progress_player_weekly_challenge", columnNames = {"player_id", "weekly_challenge_id"}))
public class PlayerChallengeProgress extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "weekly_challenge_id", nullable = false)
    private WeeklyChallenge weeklyChallenge;

    @Column(name = "current_value", nullable = false, precision = 14, scale = 4)
    private BigDecimal currentValue = BigDecimal.ZERO;

    @Column(name = "target_value", nullable = false, precision = 14, scale = 4)
    private BigDecimal targetValue;

    @Column(nullable = false)
    private boolean completed;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;
}
