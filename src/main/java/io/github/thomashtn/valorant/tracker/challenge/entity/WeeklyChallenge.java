package io.github.thomashtn.valorant.tracker.challenge.entity;

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
 * Associates one catalogue challenge with a calendar week.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "weekly_challenge",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_weekly_challenge_week_challenge",
        columnNames = {"week_start", "challenge_id"}
    )
)
public class WeeklyChallenge extends AuditableEntity {

    /**
     * Internal database identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Monday identifying the challenge week.
     */
    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    /**
     * Reusable challenge selected for the week.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    /**
     * Timestamp at which the challenge was selected.
     */
    @Column(name = "selected_at", nullable = false)
    private Instant selectedAt;

    /**
     * Timestamp at which the weekly result became immutable.
     */
    @Column(name = "finalized_at")
    private Instant finalizedAt;
}
