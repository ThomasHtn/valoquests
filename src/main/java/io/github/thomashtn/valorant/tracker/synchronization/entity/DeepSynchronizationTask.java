package io.github.thomashtn.valorant.tracker.synchronization.entity;

import io.github.thomashtn.valorant.tracker.match.entity.Season;
import io.github.thomashtn.valorant.tracker.player.entity.Player;
import io.github.thomashtn.valorant.tracker.synchronization.model.SynchronizationStatus;
import io.github.thomashtn.valorant.tracker.shared.entity.AuditableEntity;
import jakarta.persistence.*;
import java.time.*;
import lombok.*;

/**
 * Represents the deep synchronization task component.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "deep_synchronization_task")
public class DeepSynchronizationTask extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SynchronizationStatus status;

    @Column(name = "next_page")
    private Integer nextPage;

    @Column(name = "next_cursor", length = 500)
    private String nextCursor;

    @Column(name = "last_match_started_at")
    private Instant lastMatchStartedAt;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;
}
