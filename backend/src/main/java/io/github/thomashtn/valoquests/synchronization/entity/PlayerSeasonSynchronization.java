package io.github.thomashtn.valoquests.synchronization.entity;

import io.github.thomashtn.valoquests.match.entity.Season;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.shared.entity.AuditableEntity;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Records how far match-history synchronization walked one season for one player.
 *
 * <p>Only a complete season may stop at the first known match; an incomplete one is re-walked in full.
 * Below the current and previous seasons, a season with no row was never targeted and is not walked.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "player_season_synchronization",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_player_season_synchronization",
        columnNames = {"player_id", "season_id"}
    )
)
public class PlayerSeasonSynchronization extends AuditableEntity {

    /**
     * Internal database identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tracked player the season was walked for.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    /**
     * Valorant season being walked.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    /**
     * Indicates the season was walked back to its oldest match.
     */
    @Column(nullable = false)
    private boolean complete;

    /**
     * Instant the season was first marked complete.
     */
    @Column(name = "completed_at")
    private Instant completedAt;

    /**
     * Offset a resumed walk starts from, proven by a page already imported to belong to this season.
     *
     * <p>Meaningless once {@link #complete} is {@code true}.
     */
    @Column(name = "next_start_offset", nullable = false)
    private int nextStartOffset;
}
