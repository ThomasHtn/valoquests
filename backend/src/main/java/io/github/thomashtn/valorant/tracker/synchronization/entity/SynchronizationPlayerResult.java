package io.github.thomashtn.valorant.tracker.synchronization.entity;

import io.github.thomashtn.valorant.tracker.player.entity.Player;
import io.github.thomashtn.valorant.tracker.shared.entity.AuditableEntity;
import io.github.thomashtn.valorant.tracker.synchronization.model.SynchronizationStatus;
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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Stores the result of one player within a synchronization execution.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "synchronization_player_result",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_sync_player_result",
        columnNames = {"synchronization_id", "player_id"}
    )
)
public class SynchronizationPlayerResult extends AuditableEntity {

    /**
     * Internal database identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Parent synchronization execution.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "synchronization_id", nullable = false)
    private Synchronization synchronization;

    /**
     * Player processed by the execution.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    /**
     * Final player-level execution status.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SynchronizationStatus status;

    /**
     * Number of Henrik match-history pages retrieved.
     */
    @Column(name = "pages_fetched", nullable = false)
    private int pagesFetched;

    /**
     * Number of newly imported player-match associations.
     */
    @Column(name = "matches_imported", nullable = false)
    private int matchesImported;

    /**
     * Player-level error description when processing failed.
     */
    @Column(name = "error_message", length = 2000)
    private String errorMessage;
}
