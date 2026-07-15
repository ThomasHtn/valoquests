package io.github.thomashtn.valorant.tracker.challenge.entity;

import io.github.thomashtn.valorant.tracker.shared.entity.AuditableEntity;
import jakarta.persistence.*;
import java.time.*;
import lombok.*;

/**
 * Represents the weekly challenge component.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "weekly_challenge", uniqueConstraints = @UniqueConstraint(name = "uk_weekly_challenge_week_challenge", columnNames = {"week_start", "challenge_id"}))
public class WeeklyChallenge extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @Column(name = "selected_at", nullable = false)
    private Instant selectedAt;

    @Column(name = "finalized_at")
    private Instant finalizedAt;
}
