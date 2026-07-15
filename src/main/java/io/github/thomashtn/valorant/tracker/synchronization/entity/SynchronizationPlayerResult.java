package io.github.thomashtn.valorant.tracker.synchronization.entity;

import io.github.thomashtn.valorant.tracker.player.entity.Player;
import io.github.thomashtn.valorant.tracker.synchronization.model.SynchronizationStatus;
import io.github.thomashtn.valorant.tracker.shared.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Represents the synchronization player result component.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "synchronization_player_result", uniqueConstraints = @UniqueConstraint(name = "uk_sync_player_result", columnNames = {"synchronization_id", "player_id"}))
public class SynchronizationPlayerResult extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "synchronization_id", nullable = false)
    private Synchronization synchronization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SynchronizationStatus status;

    @Column(name = "pages_fetched", nullable = false)
    private int pagesFetched;

    @Column(name = "matches_imported", nullable = false)
    private int matchesImported;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;
}
