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
 * <p>{@code complete} means the season was walked back to its oldest match: every match the player
 * played during it, in a mode this application imports, is stored. Only then can a later run stop at
 * the first already-stored match, because the stored history is then a contiguous prefix.
 *
 * <p>{@code complete = false} means the walk was interrupted, by a rate limit, a crash or the page
 * cap, or has simply never finished. The next run re-walks that season in full instead of stopping
 * early, which is what prevents an interruption from leaving a permanent hole.
 *
 * <p>The row exists as soon as the walk of a season starts, so its presence is also the marker used
 * at a season boundary: an older season with no row was never targeted and must not be walked, which
 * is what bounds a first run on an empty database to the current season.
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
     * Pagination offset proven, by a page already durably imported, to still belong to this season.
     *
     * <p>A resumed walk of an incomplete season starts here instead of zero, skipping only the range a
     * previous execution already confirmed. Meaningless once {@link #complete} is {@code true}.
     */
    @Column(name = "next_start_offset", nullable = false)
    private int nextStartOffset;
}
