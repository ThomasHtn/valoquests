package io.github.thomashtn.valorant.tracker.synchronization.entity;

import io.github.thomashtn.valorant.tracker.synchronization.model.*;
import io.github.thomashtn.valorant.tracker.shared.entity.AuditableEntity;
import io.github.thomashtn.valorant.tracker.synchronization.model.SynchronizationType;
import jakarta.persistence.*;
import java.time.*;
import lombok.*;

/**
 * Represents the synchronization component.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "synchronization")
public class Synchronization extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private SynchronizationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 20)
    private SynchronizationTrigger trigger;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SynchronizationStatus status;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "players_processed", nullable = false)
    private int playersProcessed;

    @Column(name = "failure_count", nullable = false)
    private int failureCount;

    @Column(name = "matches_imported", nullable = false)
    private int matchesImported;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;
}
