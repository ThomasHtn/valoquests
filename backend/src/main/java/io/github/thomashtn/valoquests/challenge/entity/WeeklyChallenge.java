package io.github.thomashtn.valoquests.challenge.entity;

import io.github.thomashtn.valoquests.challenge.model.ChallengeCadence;
import io.github.thomashtn.valoquests.shared.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Associates one catalogue challenge with a calendar week, or with one day of it.
 *
 * <p>Uniqueness is enforced by two partial indexes the schema owns: one weekly row per challenge
 * and week, one daily row per day.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "weekly_challenge")
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
     * Whether this selection covers the whole week or a single day of it.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ChallengeCadence cadence = ChallengeCadence.WEEKLY;

    /**
     * Day a daily selection covers, inside {@link #weekStart}'s week. {@code null} for a weekly one.
     */
    @Column(name = "day")
    private LocalDate day;

    /**
     * Reusable challenge selected for the week.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    /**
     * Conditions resolved against the calibration in force at draw time.
     *
     * <p>Written once by the draw and never recomputed: a campaign is replayed from its first day
     * after every synchronization, and a target that moved with the roster would rewrite the
     * objectives of weeks already played.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resolved_conditions_json", nullable = false, columnDefinition = "jsonb")
    private String resolvedConditionsJson;

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
