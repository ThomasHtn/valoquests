package io.github.thomashtn.valorant.tracker.synchronization.entity;

import io.github.thomashtn.valorant.tracker.shared.entity.AuditableEntity;
import io.github.thomashtn.valorant.tracker.synchronization.model.SynchronizationStatus;
import io.github.thomashtn.valorant.tracker.synchronization.model.SynchronizationTrigger;
import io.github.thomashtn.valorant.tracker.synchronization.model.SynchronizationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Stores one global or single-player synchronization execution. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "synchronization")
public class Synchronization extends AuditableEntity {

    /** Internal database identifier. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Standard or deep synchronization mode. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private SynchronizationType type;

    /** Manual or scheduled execution origin. */
    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 20)
    private SynchronizationTrigger trigger;

    /** Current or final execution status. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SynchronizationStatus status;

    /** UTC timestamp at which processing started. */
    @Column(name = "started_at")
    private Instant startedAt;

    /** UTC timestamp at which processing finished. */
    @Column(name = "finished_at")
    private Instant finishedAt;

    /** Number of players attempted by the execution. */
    @Column(name = "players_processed", nullable = false)
    private int playersProcessed;

    /** Number of player-level failures. */
    @Column(name = "failure_count", nullable = false)
    private int failureCount;

    /** Number of newly inserted player-match associations. */
    @Column(name = "matches_imported", nullable = false)
    private int matchesImported;

    /** Aggregated execution error when failures occurred. */
    @Column(name = "error_message", length = 2000)
    private String errorMessage;
}
