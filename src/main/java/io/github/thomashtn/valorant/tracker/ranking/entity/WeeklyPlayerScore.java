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
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * Represents the weekly player score component.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "weekly_player_score", uniqueConstraints = @UniqueConstraint(name = "uk_weekly_score_player_week", columnNames = {"player_id", "week_start"}))
public class WeeklyPlayerScore extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(nullable = false)
    private int points;

    @Column(name = "completed_challenges", nullable = false)
    private int completedChallenges;

    @Column(nullable = false)
    private int position;

    @Column(name = "previous_position")
    private Integer previousPosition;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    @Column(name = "finalized_at")
    private Instant finalizedAt;
}
